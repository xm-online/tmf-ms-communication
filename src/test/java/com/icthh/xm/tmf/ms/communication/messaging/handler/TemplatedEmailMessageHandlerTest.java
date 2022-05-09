package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.service.mail.MailService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.ExtendedAttachment;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ByteArrayResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TemplatedEmailMessageHandlerTest {

    @Mock
    MailService mailService;

    @Mock
    TenantContextHolder tenantContextHolder;

    @Mock
    CommunicationMessageMapper mapper;

    @InjectMocks
    private EmailMessageHandler emailMessageHandler;

    @InjectMocks
    private TemplatedEmailMessageHandler templatedEmailMessageHandler;

    @Test
    public void testHandleMessage_shouldInvokeMailService() {
        CommunicationMessageCreate messageCreate = new CommunicationMessageCreate();
        messageCreate.setContent("content");
        messageCreate.setSubject("subject");
        messageCreate.setReceiver(List.of(new Receiver().email("email")));
        messageCreate.setSender(new Sender().id("sender"));
        messageCreate.setSender(new Sender().id("sender"));

        TenantContext tenantContextMock = mock(TenantContext.class);
        when(tenantContextMock.getTenantKey()).thenReturn(Optional.of(new TenantKey("xm")));

        when(tenantContextHolder.getContext()).thenReturn(tenantContextMock);
        when(mapper.messageCreateToMessage(messageCreate)).thenReturn(new CommunicationMessage());

        emailMessageHandler.handle(messageCreate);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendEmailWithContent(any(), captor.capture(), captor.capture(), captor.capture(), captor.capture());
        List<String> allValues = captor.getAllValues();
        assertThat(allValues).containsExactly("content", "subject", "email", "sender");
    }

    @Test
    public void testHandleMessage_shouldSendEmailFromTemplateWithAttachments() {
        CommunicationMessageCreate messageCreate = new CommunicationMessageCreate();
        messageCreate.setContent("content");
        messageCreate.setSubject("subject");
        messageCreate.setReceiver(List.of(new Receiver().email("email")));
        messageCreate.setSender(new Sender().id("sender"));
        messageCreate.setSender(new Sender().id("sender"));
        messageCreate.setType("TemplatedEmail");

        messageCreate.setCharacteristic(createBaseTemplateCharacteristicList());

        ExtendedAttachment attachment = new ExtendedAttachment();
        attachment.setAttachmentType("ExtendedAttachment");
        attachment.setName("fileName.txt");
        attachment.setFileBytes("json array file bytes");
        messageCreate.setAttachment(List.of(attachment));

        TenantContext tenantContextMock = mock(TenantContext.class);
        when(tenantContextMock.getTenantKey()).thenReturn(Optional.of(new TenantKey("xm")));

        when(tenantContextHolder.getContext()).thenReturn(tenantContextMock);
        when(mapper.messageCreateToMessage(messageCreate)).thenReturn(new CommunicationMessage());

        try (MockedStatic<MdcUtils> utilities = Mockito.mockStatic(MdcUtils.class)) {
            utilities.when(MdcUtils::generateRid).thenReturn("rid");

            templatedEmailMessageHandler.handle(messageCreate);

            verify(mailService).sendEmailFromTemplateWithAttachments(
                TenantContextUtils.getRequiredTenantKey(tenantContextHolder.getContext()),
                Locale.ENGLISH,
                "templateName",
                "subject",
                "email",
                Map.of("firstName", "firstName", "lastName", "lastName",
                    "templateName", "templateName", "language", "en"
                ),
                "rid",
                "sender",
                Map.of(attachment.getName(), new ByteArrayResource(Base64.decodeBase64(attachment.getFileBytes())))
            );
        }
    }

    private List<CommunicationRequestCharacteristic> createBaseTemplateCharacteristicList() {
        List<CommunicationRequestCharacteristic> characteristics = new ArrayList<>();

        characteristics.add(createBaseTemplateCharacteristic("templateName", "templateName"));
        characteristics.add(createBaseTemplateCharacteristic("language", "en"));
        characteristics.add(createBaseTemplateCharacteristic("firstName", "firstName"));
        characteristics.add(createBaseTemplateCharacteristic("lastName", "lastName"));

        return characteristics;
    }

    private CommunicationRequestCharacteristic createBaseTemplateCharacteristic(String name, String value) {
        final CommunicationRequestCharacteristic characteristic = new CommunicationRequestCharacteristic();
        characteristic.setName(name);
        characteristic.setValue(value);
        return characteristic;
    }
}
