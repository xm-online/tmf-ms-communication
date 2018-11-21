package com.icthh.xm.tmf.ms.communication.config;

import org.jsmpp.bean.BindType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class SmppConfiguration {

    private final ApplicationProperties appProps;

    protected SmppConfiguration(ApplicationProperties appProps) {
        this.appProps = appProps;
    }

    public SMPPSession getSession() throws IOException {
        SMPPSession session = new SMPPSession();
        ApplicationProperties.Smpp smpp = appProps.getSmpp();
        BindParameter bindParam = new BindParameter(
            smpp.getBindType(),
            smpp.getSystemId(),
            smpp.getPassword(),
            smpp.getSystemType(),
            smpp.getAddrTon(),
            smpp.getAddrNpi(),
            smpp.getAddressRange()
        );
        session.connectAndBind(smpp.getHost(), smpp.getPort(), bindParam);
        return session;
    }
}
