package com.ijinge.rpc.handler.idle;

import java.net.InetSocketAddress;

public interface CacheClearHandler {
    /**
     * 清理缓存
     */
    void clear(InetSocketAddress inetSocketAddress);
}
