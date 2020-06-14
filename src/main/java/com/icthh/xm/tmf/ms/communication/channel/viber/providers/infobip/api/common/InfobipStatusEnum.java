package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.common;

public enum InfobipStatusEnum {
    PENDING(1),
    UNDELIVERABLE(2),
    DELIVERED(3),
    EXPIRED(4),
    REJECTED(5);

    final int groupId;

    InfobipStatusEnum(int groupId) {
        this.groupId = groupId;
    }

    public int getGroupId() {
        return groupId;
    }
}
