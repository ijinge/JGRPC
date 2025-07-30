package com.ijinge.rpc.constants;

public class IjingeRpcConstants {

    public static final int HEAD_LENGTH = 16;

    public static final byte[] MAGIC_NUMBER = {(byte)'j',(byte)'g',(byte)'n',(byte)'b'};

    public static final int VERSION = 1;

    //ping
    public static final byte HEARTBEAT_REQUEST_TYPE = 3;
    //pong
    public static final byte HEARTBEAT_RESPONSE_TYPE = 4;

    public static final String PING = "ping";
    public static final String PONG = "pong";
}
