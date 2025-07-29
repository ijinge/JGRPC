package com.ijinge.rpc.handler.server;

import com.ijinge.rpc.constants.MessageTypeEnum;
import com.ijinge.rpc.exception.RpcException;
import com.ijinge.rpc.factory.SingletonFactory;
import com.ijinge.rpc.message.Message;
import com.ijinge.rpc.message.Request;
import com.ijinge.rpc.message.Response;
import com.ijinge.rpc.server.IjingeServiceProvider;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IjingeNettyServerHandler extends ChannelInboundHandlerAdapter {

    private RequestHandler requestHandler;

    IjingeNettyServerHandler(){
        requestHandler = SingletonFactory.getInstance(RequestHandler.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            //这里 接收到 请求的信息，然后根据请求，找到对应的服务提供者，调用，获取结果，然后返回
            //消费方 会启动一个 客户端，用户接收返回的数据
            if (msg instanceof Message){
                Message message = (Message) msg;
                // 来自客户端的请求
                if (message.getData() instanceof Request request){
                    //客户端请求
                    Object handler = requestHandler.handler(request);
                    message.setMessageType(MessageTypeEnum.RESPONSE.getCode());
                    if (ctx.channel().isActive() && ctx.channel().isWritable()){
                        Response<Object> msResponse = Response.success(handler,request.getRequestId());
                        message.setData(msResponse);
                    }else{
                        Response<Object> msResponse = Response.fail("net fail");
                        message.setData(msResponse);
                    }
                    log.info("服务端收到数据，并处理完成{}:",message);
                    //写完数据 并关闭通道
                    ctx.writeAndFlush(message).addListener(ChannelFutureListener.CLOSE);
                }
                //
            }

        }catch (Exception e){
            throw new RpcException("数据读取异常",e);
        }finally {
            //释放 以防内存泄露
            ReferenceCountUtil.release(msg);
        }
    }
}
