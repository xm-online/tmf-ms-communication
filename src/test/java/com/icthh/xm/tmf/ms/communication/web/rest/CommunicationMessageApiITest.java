package com.icthh.xm.tmf.ms.communication.web.rest;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.collect.ImmutableMap;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiController;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.ExceptionTranslator;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = CommunicationMessageApiController.class, secure = false)
@ContextConfiguration(
    classes = {CommunicationMessageApiImpl.class, CommunicationMessageApiController.class, ExceptionTranslator.class})
public class CommunicationMessageApiITest {

    public static final String CONTEXT_OF_SMS = "Context of sms";
    public static final String SENDER_ID = "SENDER_ID";
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SmppService smppService;

    @Test
    @SneakyThrows
    public void testCreatesANewCommunicationMessageAndSendIt() {

        Map<String, Object> request = of("content", CONTEXT_OF_SMS, "receiver",
                                         createReceivers("380900510000", "380900510001", "380900510002"), "type", "SMS",
                                         "sender", of("id", SENDER_ID));

        mockMvc.perform(
            post("/api/communicationManagement/v2/communicationMessage/send").contentType("application/json")
                                                                             .content(TestUtil.convertObjectToJsonBytes(
                                                                                 request)))
               .andDo(print())
               .andExpect(status().isOk());

        verify(smppService).sendMultipleMessages(eq(asList("380900510000", "380900510001", "380900510002")),
                                                 eq(CONTEXT_OF_SMS), eq(SENDER_ID));
    }

    private List<ImmutableMap<String, String>> createReceivers(String... phoneNumbers) {
        return Arrays.stream(phoneNumbers).map(it -> of("phoneNumber", it, "id", it)).collect(toList());
    }

}
