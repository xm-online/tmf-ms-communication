package com.icthh.xm.tmf.ms.communication.config;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.permission.inspector.PrivilegeInspector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private final PrivilegeInspector privilegeInspector;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        privilegeInspector.readPrivileges(MdcUtils.getRid());
    }
}
