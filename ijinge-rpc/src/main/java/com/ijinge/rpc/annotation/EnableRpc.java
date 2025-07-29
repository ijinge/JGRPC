package com.ijinge.rpc.annotation;

import com.ijinge.rpc.spring.IjingeRpcSpringBeanPostProcessor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({IjingeRpcSpringBeanPostProcessor.class})
public @interface EnableRpc {
    //nacos主机名
    String nacosHost() default "192.168.52.128";
    //nacos端口号
    int nacosPort() default 8848;
    //nacos组，同一个组内 互通，并且组成集群
    String nacosGroup() default "ijinge-rpc-group";
    //server服务端口
    int serverPort() default 13567;
}
