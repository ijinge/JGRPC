package com.ijinge.rpc.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 协议中，除了魔法数，版本，数据长度外的其他数据，封装到Message对象中
 * */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Message {
    //rpc message type
    private byte messageType;
    //serialization type
    private byte codec;
    //compress type
    private byte compress;
    //request id
    private int requestId;
    //request data
    private Object data;
}