package com.ijinge.rpc.server;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.ijinge.rpc.annotation.IjingeService;
import com.ijinge.rpc.config.RpcConfig;
import com.ijinge.rpc.factory.SingletonFactory;
import com.ijinge.rpc.netty.NettyServer;
import com.ijinge.rpc.register.NacosTemplate;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class IjingeServiceProvider {
    @Getter
    private RpcConfig rpcConfig;
    private final Map<String, Object> serviceMap;
    private NacosTemplate nacosTemplate;

    public IjingeServiceProvider(){
        //发布的服务 都在这里
        serviceMap = new ConcurrentHashMap<>();

    }

    public void init(RpcConfig rpcConfig) {
        this.rpcConfig = rpcConfig;
        nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
        nacosTemplate.init(rpcConfig.getNacosHost(),rpcConfig.getNacosPort());
    }

    public void publishService(IjingeService IjingeService, Object service) {
        registerService(IjingeService,service);
        //检测到有服务发布的注解，启动NettyServer
        NettyServer nettyServer = SingletonFactory.getInstance(NettyServer.class);
        nettyServer.setIjingeServiceProvider(this);
        if (!nettyServer.isRunning()){
            nettyServer.run();
        }
    }
    private void registerService(IjingeService IjingeService, Object service) {
        //service要进行注册, 先创建一个map进行存储
        String serviceName = service.getClass().getInterfaces()[0].getCanonicalName()+IjingeService.version();
        serviceMap.put(serviceName,service);
        //将服务注册到nacos上
        try {
            Instance instance = new Instance();
            instance.setPort(rpcConfig.getProviderPort());
            instance.setIp(InetAddress.getLocalHost().getHostAddress());
            instance.setServiceName(serviceName);
            nacosTemplate.registerServer(rpcConfig.getNacosGroup(),instance);

        } catch (Exception e) {
            log.error("nacos 注册服务失败:",e);
        }
        log.info("发现服务{}并注册",serviceName);
    }

    public Object getService(String serviceName) {
        return serviceMap.get(serviceName);
    }


}
