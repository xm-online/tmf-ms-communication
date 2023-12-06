package com.icthh.xm.tmf.ms.communication.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter
@EqualsAndHashCode
public class EmailReceiver {
    private final String email;
    private final List<String> bcc;

    public EmailReceiver(String email) {
        this.email = email;
        this.bcc = List.of();
    }

    public EmailReceiver(String email, List<String> bcc) {
        this.email = email;
        this.bcc = bcc;
    }
}
