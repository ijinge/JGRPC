package com.ijinge.rpc.annotation;

import com.ijinge.rpc.bean.IjingeBeanDefinitionRegistry;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({IjingeBeanDefinitionRegistry.class})
public @interface EnableHttpClient {
    String basePackage();
}
