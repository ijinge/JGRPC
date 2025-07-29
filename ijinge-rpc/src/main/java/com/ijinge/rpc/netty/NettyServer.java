package com.ijinge.rpc.netty;


import com.ijinge.rpc.handler.IjingeRpcThreadFactory;
import com.ijinge.rpc.handler.server.NettyServerInitiator;
import com.ijinge.rpc.server.IjingeServiceProvider;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyServer implements IjingeServer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    public static final int PORT = 13567;
    @Setter
    private IjingeServiceProvider ijingeServiceProvider;
    private DefaultEventExecutorGroup eventExecutors;

    private boolean isRunning;

    public NettyServer() {
    }

    @Override
    public void run() {
        // 负责连接建立
        bossGroup = new NioEventLoopGroup();
        // 负责处理读写
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            eventExecutors = new DefaultEventExecutorGroup(8 * 2,new IjingeRpcThreadFactory(ijingeServiceProvider));
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .option(ChannelOption.SO_BACKLOG,1024)
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    //是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 当客户端第一次进行请求的时候才会进行初始化
                    .childHandler(new NettyServerInitiator(eventExecutors));
            // 绑定端口，同步等待绑定成功
            b.bind(ijingeServiceProvider.getRpcConfig().getProviderPort()).sync().channel();
            isRunning = true;
            // 在 JVM 进程即将退出时，自动执行 stopNettyServer() 方法，优雅地关闭 Netty 服务器并释放资源。
            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run() {
                    stopNettyServer();
                }
            });
        }catch (InterruptedException e){
            log.error("occur exception when start server:",e);
        }


    }

    public void stop() {
        stopNettyServer();
    }

    private void stopNettyServer() {
        if (eventExecutors != null){
            eventExecutors.shutdownGracefully();
        }
        if (bossGroup != null){
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null){
            workerGroup.shutdownGracefully();
        }
    }


    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }
}
