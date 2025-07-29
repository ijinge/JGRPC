package com.ijinge.rpc.proxy;

import com.ijinge.rpc.annotation.IjingeReference;
import com.ijinge.rpc.exception.RpcException;
import com.ijinge.rpc.message.Request;
import com.ijinge.rpc.message.Response;
import com.ijinge.rpc.netty.NettyClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


//每一个动态代理类的调用处理程序都必须实现InvocationHandler接口，
// 并且每个代理类的实例都关联到了实现该接口的动态代理类调用处理程序中，
// 当我们通过动态代理对象调用一个方法时候，
// 这个方法的调用就会被转发到实现InvocationHandler接口类的invoke方法来调用
public class IjingeRpcClientProxy implements InvocationHandler {
    public IjingeRpcClientProxy(){

    }
    private IjingeReference ijingeReference;
    private NettyClient nettyClient;

    public IjingeRpcClientProxy(IjingeReference IjingeReference,NettyClient nettyClient) {
        this.ijingeReference = IjingeReference;
        this.nettyClient = nettyClient;
    }
    /**
     * proxy:代理类代理的真实代理对象com.sun.proxy.$Proxy0
     * method:我们所要调用某个对象真实的方法的Method对象
     * args:指代代理对象方法传递的参数
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //在这里实现调用
//        System.out.println("rpc的代理实现类 调用了...");
        //构建请求数据
        //构建请求数据
        String requestId = UUID.randomUUID().toString();
        Request request = Request.builder()
                .methodName(method.getName())
                .parameters(args)
                .interfaceName(method.getDeclaringClass().getName())
                .paramTypes(method.getParameterTypes())
                .requestId(requestId)
                .version(ijingeReference.version())
                .build();
        //创建Netty客户端
        String host = ijingeReference.host();
        int port = ijingeReference.port();
        CompletableFuture<Response<Object>> future = (CompletableFuture<Response<Object>>) nettyClient.sendRequest(request);
        Response<Object> response = future.get();
        if (response == null){
            throw new RpcException("服务调用失败");
        }
        if (!requestId.equals(response.getRequestId())){
            throw new RpcException("响应结果和请求不一致");
        }
        return response.getData();
    }
    /**
     * get the proxy object
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }
}
