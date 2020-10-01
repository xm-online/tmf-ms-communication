package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommunicationMessageMapper {

    CommunicationMessage messageCreateToMessage(CommunicationMessageCreate car);
}
