package com.ijinge.rpc.handler.server;

import com.ijinge.rpc.exception.RpcException;
import com.ijinge.rpc.factory.SingletonFactory;
import com.ijinge.rpc.message.Request;
import com.ijinge.rpc.server.IjingeServiceProvider;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RequestHandler{
    private IjingeServiceProvider serviceProvider;

    public RequestHandler(){
        this.serviceProvider = SingletonFactory.getInstance(IjingeServiceProvider.class);
    }

    // 处理客户端请求
    public  Object handler(Request request){
        String interfaceName = request.getInterfaceName();
        String version = request.getVersion();
        String serviceName = interfaceName + version;
        Object service = serviceProvider.getService(serviceName);
        try {
            Method method = service.getClass().getMethod(request.getMethodName(), request.getParamTypes());
            return method.invoke(service, request.getParameters());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RpcException("服务调用出现问题:"+e.getMessage(),e);
        }
    }
}
