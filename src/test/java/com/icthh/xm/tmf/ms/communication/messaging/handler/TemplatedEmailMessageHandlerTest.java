package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.service.mail.MailService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
}
