package com.icthh.xm.tmf.ms.communication.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class EmailReceiver {
    private final String email;
    private final List<String> bcc;
}
