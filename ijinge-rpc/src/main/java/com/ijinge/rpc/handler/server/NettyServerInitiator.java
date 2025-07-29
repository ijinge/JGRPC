package com.ijinge.rpc.handler.server;

import com.ijinge.rpc.netty.codec.IjingeRpcEncoder;
import com.ijinge.rpc.netty.codec.IjingeRpcDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;

public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {
    private EventExecutorGroup eventExecutors;

    public NettyServerInitiator(EventExecutorGroup eventExecutors) {
        this.eventExecutors = eventExecutors;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        //解码器
        ch.pipeline ().addLast ( "decoder",new IjingeRpcDecoder());
        //编码器
        ch.pipeline ().addLast ( "encoder",new IjingeRpcEncoder());
        //消息处理器，线程池处理 inHandler
        ch.pipeline ().addLast (eventExecutors,"handler",new IjingeNettyServerHandler() );
    }
}
