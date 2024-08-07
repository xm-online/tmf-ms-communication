package com.icthh.xm.tmf.ms.communication.service.mail;

import static com.icthh.xm.tmf.ms.communication.config.Constants.DEFAULT_LANGUAGE;
import static com.icthh.xm.tmf.ms.communication.config.Constants.TRANSLATION_KEY;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.context.i18n.LocaleContextHolder.getLocale;
import static org.springframework.context.i18n.LocaleContextHolder.getLocaleContext;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;
import static org.springframework.context.i18n.LocaleContextHolder.setLocaleContext;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.mail.provider.MailProviderService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.CommunicationTenantConfigService;
import com.icthh.xm.tmf.ms.communication.config.CommunicationTenantConfigService.CommunicationTenantConfig.MailSetting;
import com.icthh.xm.tmf.ms.communication.domain.EmailReceiver;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import freemarker.template.Configuration;
import io.github.jhipster.config.JHipsterProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails.
 * We use the @Async annotation to send emails asynchronously.
 */
@Slf4j
@RequiredArgsConstructor
@Service
@IgnoreLogginAspect
public class MailService {

    private final JHipsterProperties jHipsterProperties;
    private final ApplicationProperties applicationProperties;
    private final MailProviderService mailProviderService;
    private final MessageSource messageSource;
    private final TenantEmailTemplateService tenantEmailTemplateService;
    private final Configuration freeMarkerConfiguration;
    private final TenantContextHolder tenantContextHolder;
    private final CommunicationTenantConfigService tenantConfigService;
    private final LocalizationMessageService localizationMessageService;
    private final EmailSpecService emailSpecService;
    private final EmailTemplateService emailTemplateService;

    @Resource
    @Lazy
    private MailService selfReference;

    private static void execForCustomRid(String rid, Runnable runnable) {
        final String oldRid = MdcUtils.getRid();
        try {
            MdcUtils.putRid(rid);
            runnable.run();
        } finally {
            if (oldRid != null) {
                MdcUtils.putRid(oldRid);
            } else {
                MdcUtils.removeRid();
            }
        }
    }

    /**
     * Send mail with raw subject and from.
     *
     * @param locale       the locale
     * @param templateName the email template name
     * @param subject      the raw subject
     * @param email        the to email
     * @param from         the from email
     * @param objectModel  the email parameters
     */
    public void sendEmailFromTemplate(Locale locale,
                                      String templateName,
                                      String subject,
                                      String email,
                                      String from,
                                      Map<String, Object> objectModel) {
        selfReference.sendEmailFromTemplate(TenantContextUtils.getRequiredTenantKey(tenantContextHolder.getContext()),
            locale,
            templateName,
            subject,
            email,
            objectModel,
            MdcUtils.generateRid(),
            from);
    }

    /**
     * Send email with subject from messageSource and generated from.
     *
     * @param tenantKey    the tenant key
     * @param locale       the locale
     * @param templateName the email template name
     * @param titleKey     the subject key from messageSource
     * @param email        the to email
     * @param model        the email parameters
     * @param rid          the request id
     */
    public void sendEmailFromTemplate(TenantKey tenantKey,
                                      Locale locale,
                                      String templateName,
                                      String titleKey,
                                      String email,
                                      Map<String, Object> model,
                                      String rid) {
        String subject = messageSource.getMessage(titleKey, null, locale);
        selfReference.sendEmailFromTemplate(tenantKey,
            locale,
            templateName,
            subject,
            email,
            model,
            rid,
            generateFrom(tenantKey)
        );
    }

    /**
     * Send of email.
     *
     * @param tenantKey    the tenant key
     * @param locale       the locale
     * @param templateName the email template name
     * @param subject      the raw subject
     * @param email        the to email
     * @param objectModel  the email parameters
     * @param rid          the request id
     * @param from         the from email
     */
    public void sendEmailFromTemplate(TenantKey tenantKey,
                                      Locale locale,
                                      String templateName,
                                      String subject,
                                      String email,
                                      Map<String, Object> objectModel,
                                      String rid,
                                      String from) {
        initAndSendEmail(tenantKey,
            locale,
            templateName,
            subject,
            new EmailReceiver(email),
            objectModel,
            rid,
            from,
            null);
    }

    /**
     * Send of email with attachment
     *
     * @param tenantKey          the tenant key
     * @param locale             the locale
     * @param templateName       the email template name
     * @param subject            the raw subject
     * @param email              the to email
     * @param objectModel        the email parameters
     * @param rid                the request id
     * @param from               the from email
     * @param attachmentFilename the name of the attachment as it will appear in the mail
     * @param dataSource         the {@code javax.activation.DataSource} to take the content from, determining the InputStream
     *                           and the content type
     */
    public void sendEmailFromTemplateWithAttachment(TenantKey tenantKey,
                                                    Locale locale,
                                                    String templateName,
                                                    String subject,
                                                    String email,
                                                    Map<String, Object> objectModel,
                                                    String rid,
                                                    String from,
                                                    String attachmentFilename,
                                                    InputStreamSource dataSource) {
        initAndSendEmail(tenantKey,
            locale,
            templateName,
            subject,
            new EmailReceiver(email),
            objectModel,
            rid,
            from,
            Map.of(attachmentFilename, dataSource));
    }

    /**
     * Send of email with attachment
     *
     * @param tenantKey    the tenant key
     * @param locale       the locale
     * @param templateName the email template name
     * @param subject      the raw subject
     * @param email        the to email
     * @param objectModel  the email parameters
     * @param rid          the request id
     * @param from         the from email
     * @param attachments  map of attachment file name which appear in the mail and data source of file content
     *                     and the content type
     */
    public void sendEmailFromTemplateWithAttachments(TenantKey tenantKey,
                                                     Locale locale,
                                                     String templateName,
                                                     String subject,
                                                     String email,
                                                     Map<String, Object> objectModel,
                                                     String rid,
                                                     String from,
                                                     Map<String, InputStreamSource> attachments) {
        initAndSendEmail(tenantKey,
            locale,
            templateName,
            subject,
            new EmailReceiver(email),
            objectModel,
            rid,
            from,
            attachments);
    }

    public void sendEmailFromTemplateWithAttachments(TenantKey tenantKey,
                                                     Locale locale,
                                                     String templateName,
                                                     String subject,
                                                     EmailReceiver email,
                                                     Map<String, Object> objectModel,
                                                     String rid,
                                                     String from,
                                                     Map<String, InputStreamSource> attachments) {
        initAndSendEmail(tenantKey,
            locale,
            templateName,
            subject,
            email,
            objectModel,
            rid,
            from,
            attachments);
    }

    /**
     * Send mail with raw subject, from and content.
     *
     * @param content            the content of email
     * @param subject            the raw subject
     * @param email              the to email
     * @param from               the from email
     * @param attachmentFilename the name of the attachment as it will appear in the mail
     * @param dataSource         the {@code javax.activation.DataSource} to take the content from, determining the InputStream
     *                           and the content type
     */
    public void sendEmailWithContentAndAttachments(
        TenantKey tenantKey,
        String content,
        String subject,
        String email,
        String from,
        String attachmentFilename,
        InputStreamSource dataSource) {
        initAndSendEmail(tenantKey,
            content,
            subject,
            new EmailReceiver(email),
            MdcUtils.generateRid(),
            from,
            Map.of(attachmentFilename, dataSource));
    }

    /**
     * Send mail with raw subject, from and content.
     *
     * @param content the content of email
     * @param subject the raw subject
     * @param email   the to email
     * @param from    the from email
     */
    public void sendEmailWithContent(
        TenantKey tenantKey,
        String content,
        String subject,
        String email,
        String from) {
        initAndSendEmail(tenantKey,
            content,
            subject,
            new EmailReceiver(email),
            MdcUtils.generateRid(),
            from,
            null);
    }

    public void sendEmailWithContent(
        TenantKey tenantKey,
        String content,
        String subject,
        EmailReceiver emailReceiver,
        String from) {
        initAndSendEmail(tenantKey,
            content,
            subject,
            emailReceiver,
            MdcUtils.generateRid(),
            from,
            null);
    }

    private void initAndSendEmail(TenantKey tenantKey,
                                  Locale locale,
                                  String templateName,
                                  String subject,
                                  EmailReceiver email,
                                  Map<String, Object> objectModel,
                                  String rid,
                                  String from,
                                  Map<String, InputStreamSource> attachments) {
        execForCustomRid(rid, () -> {
            if (email == null) {
                log.warn("Can't send email on null address for tenant: {}, email template: {}",
                    tenantKey.getValue(),
                    templateName);
                return;
            }

            String templatePath = EmailTemplateUtil.emailTemplateKey(tenantKey, templateName,locale.getLanguage());
            String emailTemplate = tenantEmailTemplateService.getEmailTemplateByKey(tenantKey, templateName, locale.getLanguage());
            String processedContent = emailTemplateService.processEmailTemplate(tenantKey.getValue(), emailTemplate, objectModel, locale.getLanguage(), templatePath);

            MailParams mailParams = resolve(subject, from, templateName, locale, objectModel);
            mailParams = resolveMailParamsBySpec(mailParams, tenantKey.getValue(), templateName, locale.getLanguage(), objectModel);

            sendEmail(
                email,
                mailParams.getSubject(),
                processedContent,
                mailParams.getFrom(),
                attachments,
                mailProviderService.getJavaMailSender(tenantKey.getValue())
            );
        });
    }

    private String generateFrom(TenantKey tenantKey) {
        return jHipsterProperties.getMail().getFrom().replace("<tenantname>", tenantKey.getValue());
    }

    private MailParams resolve(String subject,
                               String from,
                               String templateName,
                               Locale locale,
                               Map<String, Object> objectModel
    ) {
        MailParams mailParams = new MailParams(subject, from);

        List<MailSetting> settings = tenantConfigService.getCommunicationTenantConfig().getMailSettings();
        LocaleContext localeContext = getLocaleContext();
        setLocale(locale);

        Optional<MailSetting> mailSettingOptional = settings.stream()
            .filter(it -> templateName.equals(it.getTemplateName()))
            .findFirst();

        if (mailSettingOptional.isPresent()) {
            MailSetting mailSetting = mailSettingOptional.get();
            log.info("resolve: for templateName: {} found MailSetting: {}", templateName, mailSetting);

            String i18nSubject = getI18nName(mailSetting.getSubject()).orElse(subject);
            i18nSubject = applyModel(i18nSubject, objectModel);
            mailParams.setSubject(i18nSubject);

            String i18nFrom = getI18nName(mailSetting.getFrom()).orElse(from);
            i18nFrom = applyModel(i18nFrom, objectModel);
            mailParams.setFrom(i18nFrom);
        }

        setLocaleContext(localeContext);
        return mailParams;
    }

    private MailParams resolveMailParamsBySpec(MailParams mailParams, String tenantKey, String templateKey, String lang, Map<String, Object> objectModel) {
        Optional<EmailTemplateSpec> templateSpec = emailSpecService.getEmailTemplateSpec(tenantKey, templateKey);

        if (templateSpec.isPresent()) {
            EmailTemplateSpec emailTemplateSpec = templateSpec.get();
            log.info("resolve by spec: for templateKey: {} found templateSubject: {}",
                emailTemplateSpec.getTemplateKey(), emailTemplateSpec.getSubjectTemplate());

            if (StringUtils.isBlank(mailParams.getSubject())) {
                setMailSubject(emailTemplateSpec, mailParams, lang, objectModel);
            }

            if (StringUtils.isBlank(mailParams.getFrom())) {
                setMailFrom(emailTemplateSpec, mailParams, lang, objectModel);
            }
        }

        return mailParams;
    }

    private void setMailSubject(EmailTemplateSpec emailTemplateSpec, MailParams mailParams, String lang, Map<String, Object> objectModel) {
        Map<String, String> langToSubjectMap = emailTemplateSpec.getSubjectTemplate();
        String i18nSubject = langMapToLocalizedParam(langToSubjectMap, lang, objectModel);
        mailParams.setSubject(i18nSubject);
    }

    private void setMailFrom(EmailTemplateSpec emailTemplateSpec, MailParams mailParams, String lang, Map<String, Object> objectModel) {
        Map<String, String> langToEmailFromMap = emailTemplateSpec.getEmailFrom();
        String i18nFrom = langMapToLocalizedParam(langToEmailFromMap, lang, objectModel);
        mailParams.setFrom(i18nFrom);
    }

    private String langMapToLocalizedParam(Map<String, String> langMap, String lang,  Map<String, Object> objectModel) {
        String i18nParam = langMap.getOrDefault(lang, langMap.get(DEFAULT_LANGUAGE));
        return applyModel(i18nParam, objectModel);
    }

    private String applyModel(String value, Map<String, Object> objectModel) {
        if (isBlank(value)) {
            return value;
        }
        for (Map.Entry<String, Object> entry : objectModel.entrySet()) {
            value = value.replace(tokenizeKey(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return value;
    }

    private String tokenizeKey(String key) {
        return "${" + key + "}";
    }

    private Optional<String> getI18nName(Map<String, String> name) {
        if (name == null) {
            return Optional.empty();
        }
        if (name.containsKey(TRANSLATION_KEY)) {
            String translationKey = name.get(TRANSLATION_KEY);
            return Optional.of(localizationMessageService.getMessage(translationKey));
        } else if (name.containsKey(getLocale().getLanguage())) {
            return Optional.of(name.get(getLocale().getLanguage()));
        } else if (name.containsKey(ENGLISH.getLanguage())) {
            return Optional.of(name.get(ENGLISH.getLanguage()));
        }
        return Optional.empty();
    }

    // package level for testing
    void sendEmail(String to,
                   String subject,
                   String content,
                   String from,
                   Map<String, InputStreamSource> attachments,
                   JavaMailSender javaMailSender) {
        sendEmail(new EmailReceiver(to), subject, content, from, attachments, javaMailSender);
    }

    void sendEmail(EmailReceiver emailReceiver,
                   String subject,
                   String content,
                   String from,
                   Map<String, InputStreamSource> attachments,
                   JavaMailSender javaMailSender) {
        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper message;
        String to = emailReceiver.getEmail();
        try {
            boolean hasAttachments = !isEmpty(attachments) &&
                attachments
                    .entrySet()
                    .stream()
                    .allMatch(entry -> nonNull(entry.getKey()) && nonNull(entry.getValue()));

            log.debug("Send email[multipart: '{}' and html: '{}' and attachmentFilenames: '{}' to: '{}'] " +
                    "with subject: '{}' and content: '{}'",
                hasAttachments, true, ofNullable(attachments).map(Map::keySet).orElse(null),
                to, subject, content);

            message = new MimeMessageHelper(mimeMessage, hasAttachments, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(from);
            message.setSubject(subject);
            message.setText(content, true);
            if (hasAttachments) {
                for (Map.Entry<String, InputStreamSource> entry : attachments.entrySet()) {
                    message.addAttachment(entry.getKey(), entry.getValue());
                }
            }
            emailReceiver.getBcc().forEach(it -> getAddBcc(it, message));
            javaMailSender.send(mimeMessage);
            log.debug("Sent email to User '{}'", to);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Email could not be sent to user '" + to + "'", e);
            } else {
                log.warn("Email could not be sent to user '{}': {}", to, e.getMessage());
            }

            if (applicationProperties.getEmail().isFailOnError()) {
                throw new IllegalStateException("Email could not be sent to user '" + to, e);
            }
        }
    }

    @SneakyThrows
    private void getAddBcc(String it, MimeMessageHelper message) {
        message.addBcc(it);
    }

    private void initAndSendEmail(TenantKey tenantKey,
                                  String content,
                                  String subject,
                                  EmailReceiver emailReceiver,
                                  String rid,
                                  String from,
                                  Map<String, InputStreamSource> attachments) {
        execForCustomRid(rid, () -> {
            if (emailReceiver.getEmail() == null) {
                log.warn("Can't send email on null address for tenant: {}, Email [ subject : {}, to : {} ]",
                    tenantKey.getValue(), subject, emailReceiver.getEmail());
                return;
            }

            sendEmail(
                emailReceiver,
                subject,
                content,
                from,
                attachments,
                mailProviderService.getJavaMailSender(tenantKey.getValue())
            );
        });
    }

    @Data
    private static class MailParams {
        private String subject;
        private String from;

        MailParams(String subject, String from) {
            this.subject = subject;
            this.from = from;
        }
    }
}
