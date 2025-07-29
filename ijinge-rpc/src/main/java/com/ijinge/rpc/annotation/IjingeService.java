package com.ijinge.rpc.annotation;

import java.lang.annotation.*;

/**
 * 服务提供方使用此注解
 * Spring会将加上此注解的Bean 将其发布为服务 放入 serviceProvider中
 * */

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface IjingeService {
    String version() default "1.0";
}
