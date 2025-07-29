package com.ijinge.rpc.handler.client;

import com.ijinge.rpc.constants.MessageTypeEnum;
import com.ijinge.rpc.factory.SingletonFactory;
import com.ijinge.rpc.message.Message;
import com.ijinge.rpc.message.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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
            }
        }finally {
            //释放ByteBuf 避免内存泄露
            ReferenceCountUtil.release(msg);
        }
    }
}
