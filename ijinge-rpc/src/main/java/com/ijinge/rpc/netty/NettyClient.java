package com.ijinge.rpc.netty;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.ijinge.rpc.config.RpcConfig;
import com.ijinge.rpc.constants.CompressTypeEnum;
import com.ijinge.rpc.constants.MessageTypeEnum;
import com.ijinge.rpc.constants.SerializationTypeEnum;
import com.ijinge.rpc.exception.RpcException;
import com.ijinge.rpc.factory.SingletonFactory;
import com.ijinge.rpc.handler.client.IjingeNettyClientHandler;
import com.ijinge.rpc.handler.client.UnprocessedRequests;
import com.ijinge.rpc.message.Message;
import com.ijinge.rpc.message.Request;
import com.ijinge.rpc.message.Response;
import com.ijinge.rpc.netty.codec.IjingeRpcDecoder;
import com.ijinge.rpc.netty.codec.IjingeRpcEncoder;
import com.ijinge.rpc.register.NacosTemplate;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class NettyClient implements IjingeClient {

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private  UnprocessedRequests unprocessedRequests;
    private NacosTemplate nacosTemplate;
    @Setter
    private RpcConfig rpcConfig;

    // 读快 写慢 不适用于存取大量数据  并且写多的场景
    // 如果nacos挂掉了从缓存中获取服务方方法实例
    private static final Set<String> SERVICES= new CopyOnWriteArraySet<>();

    public NettyClient(){
        this.nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //超时时间设置
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline ().addLast ( "decoder",new IjingeRpcDecoder() );
                        ch.pipeline ().addLast ( "encoder",new IjingeRpcEncoder());
                        ch.pipeline ().addLast ( "handler",new IjingeNettyClientHandler() );
                    }
                });
    }

    @Override
    public Object sendRequest(Request request) {
        //1. 连接netty服务，获取channel
//        InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
        //通过注册中心获取主机和端口
        String serviceName = request.getInterfaceName() + request.getVersion();
        InetSocketAddress inetSocketAddress = null;
        Instance oneHealthyInstance = null;
        if(!SERVICES.isEmpty()){
            //有缓存的服务提供者服务器，直接获取
            //随机获取一个
            Optional<String> optional = SERVICES.stream().skip(SERVICES.size() - 1).findFirst();
            if (optional.isPresent()){
                String ipAndPort = optional.get();
                String[] split = ipAndPort.split(":");
                inetSocketAddress = new InetSocketAddress(split[0],Integer.parseInt(split[1]));
                log.info("走了缓存的服务提供者地址，省去了连接nacos的过程...");
            }
        }
        if (inetSocketAddress == null){

            try {
                log.debug("寻找的服务名称：{}",serviceName);
                log.debug("nacos服务器源{}",rpcConfig.getNacosPort());
                oneHealthyInstance = nacosTemplate.getOneHealthyInstance(serviceName,rpcConfig.getNacosGroup());
            } catch (Exception e) {
                throw new RpcException("没有获取到可用的服务提供者");
            }
            //从nacos获取实例后，将其缓存起来
            SERVICES.add(oneHealthyInstance.getIp()+":"+oneHealthyInstance.getPort());
            // 连接地址、端口
            inetSocketAddress = new InetSocketAddress(oneHealthyInstance.getIp(), oneHealthyInstance.getPort());
        }

        //连接
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();

        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()){
                //代表连接成功，将channel放入任务中
                completableFuture.complete(future.channel());
            }else {
                //连接失败 从 缓存中 去除
                // address : /10.147.20.168:13568
                String address = future.channel().remoteAddress().toString().substring(1);
                SERVICES.remove(address);
                log.info("删除provider服务缓存成功...");
                //标识 任务失败
                completableFuture.completeExceptionally(future.cause());
                throw new RpcException("连接服务器失败");
            }
        });
        //结果获取的任务
        CompletableFuture<Response<Object>> resultFuture = new CompletableFuture<>();
        try {
            // 阻塞在此 与服务端建立连接成功时 返回此channel
            Channel channel = completableFuture.get();
            if (channel.isActive()){
                //将任务 存起来，和请求id对应，便于后续读取到数据后，可以根据请求id，将任务标识完成
                unprocessedRequests.put(request.getRequestId(),resultFuture);
                //构建发送的数据
                Message message = Message.builder()
                        .messageType(MessageTypeEnum.REQUEST.getCode())
                        .codec(SerializationTypeEnum.PROTOSTUFF.getCode())
                        .compress(CompressTypeEnum.GZIP.getCode())
                        .data(request)
                        .build();
                //请求,并添加监听
                channel.writeAndFlush(message).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()){
                        //任务完成
                        log.info("发送数据成功:{}",message);
                    }else{
                        //发送数据失败
                        future.channel().close();
                        //任务标识为完成 有异常
                        resultFuture.completeExceptionally(future.cause());
                        log.info("发送数据失败:",future.cause());
                    }
                });

            }

        } catch (Exception e) {
            throw new RpcException("获取Channel失败",e);
        }

        return resultFuture;
    }

}
