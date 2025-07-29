package com.ijinge.rpc.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface IjingeMapping {
    String api() default "";
    String url() default "";
}
