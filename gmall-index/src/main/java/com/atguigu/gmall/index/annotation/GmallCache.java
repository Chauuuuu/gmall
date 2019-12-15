package com.atguigu.gmall.index.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    @AliasFor("prefix")
    String value() default "";
    @AliasFor("value")
    String prefix() default "";

    int timeout() default 300;

    int random() default 100;
}
