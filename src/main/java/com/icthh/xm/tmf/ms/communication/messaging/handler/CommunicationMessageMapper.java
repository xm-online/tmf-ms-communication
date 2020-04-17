package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CommunicationMessageMapper {
    CommunicationMessageMapper INSTANCE = Mappers.getMapper( CommunicationMessageMapper.class );

    CommunicationMessage messageCreateToMessage(CommunicationMessageCreate car);

}
