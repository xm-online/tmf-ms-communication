package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommunicationMessageMapper {

    @Mapping(target = "href", ignore = true)
    @Mapping(target = "id", ignore = true)
    CommunicationMessage messageCreateToMessage(CommunicationMessageCreate car);

    CommunicationMessageCreate messageToMessageCreate(CommunicationMessage cm);

}
