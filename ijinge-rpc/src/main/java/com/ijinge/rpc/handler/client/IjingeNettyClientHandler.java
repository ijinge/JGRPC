package com.ijinge.rpc.handler.client;

import com.ijinge.rpc.constants.CompressTypeEnum;
import com.ijinge.rpc.constants.IjingeRpcConstants;
import com.ijinge.rpc.constants.MessageTypeEnum;
import com.ijinge.rpc.constants.SerializationTypeEnum;
import com.ijinge.rpc.factory.SingletonFactory;
import com.ijinge.rpc.message.Message;
import com.ijinge.rpc.message.Response;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IjingeNettyClientHandler extends ChannelInboundHandlerAdapter {
    private  UnprocessedRequests unprocessedRequests;

    public IjingeNettyClientHandler(){
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof Message){
                Message message = (Message) msg;
                byte messageType = message.getMessageType();
                //读取数据 如果是response的消息类型，拿到数据，标识为完成
                if (messageType == MessageTypeEnum.RESPONSE.getCode()){
                    Response<Object> data = (Response<Object>) message.getData();
                    unprocessedRequests.complete(data);
                    log.info("客户端收到服务器响应的数据，并处理完成{}:",message);
                }
                // 心跳包
                else if (messageType == MessageTypeEnum.HEARTBEAT_PING.getCode()){
                    //心跳包 返回PONG
                    message.setMessageType(MessageTypeEnum.HEARTBEAT_PONG.getCode());
                    message.setData(IjingeRpcConstants.PONG);
                    //写完数据 并关闭通道
                    ctx.writeAndFlush(message).addListener(ChannelFutureListener.CLOSE);
                }
            }
        }finally {
            //释放ByteBuf 避免内存泄露
            ReferenceCountUtil.release(msg);
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                log.info("3s未收到写请求，发起心跳,地址：{}", ctx.channel().remoteAddress());
                Message rpcMessage = new Message();
                rpcMessage.setCodec(SerializationTypeEnum.PROTOSTUFF.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                rpcMessage.setMessageType(IjingeRpcConstants.HEARTBEAT_REQUEST_TYPE);
                rpcMessage.setData(IjingeRpcConstants.PING);
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //代表通道已连接
        //表示channel活着
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //代表连接关闭了
        log.info("服务端连接关闭:{}",ctx.channel().remoteAddress());
        //需要将缓存清除掉

        //标识channel不活着
        ctx.fireChannelInactive();
    }
}
