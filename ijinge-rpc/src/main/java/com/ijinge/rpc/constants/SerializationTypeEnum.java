package com.ijinge.rpc.constants;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Getter
public enum SerializationTypeEnum {
    //读取协议这的压缩类型，来此枚举进行匹配
    PROTOSTUFF((byte) 0x01, "protostuff");

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        for (SerializationTypeEnum c : SerializationTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }
}
