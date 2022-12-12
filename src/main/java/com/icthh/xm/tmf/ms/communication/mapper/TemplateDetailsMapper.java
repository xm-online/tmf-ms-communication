package com.icthh.xm.tmf.ms.communication.mapper;

import com.icthh.xm.tmf.ms.communication.domain.dto.TemplateDetails;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TemplateDetailsMapper {

    @Mapping(target = "content", ignore = true)
    TemplateDetails emailTemplateToDetails(EmailTemplateSpec emailTemplateSpec);
}
