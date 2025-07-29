package com.ijinge.rpc.netty;

import com.ijinge.rpc.message.Request;

public interface IjingeClient {
    /**
     * 发送请求，并接收数据
     * @param request
     * @return
     */
    Object sendRequest(Request request);
}
