package com.ijinge.rpc.annotation;
import java.lang.annotation.*;

@Target({ElementType.CONSTRUCTOR,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface IjingeReference {
    String host() default "127.0.0.1";

    int port() default 13567;

    String version() default "1.0";
}
