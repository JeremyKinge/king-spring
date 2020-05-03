package com.kingge.spring.stereotype.context;

import java.lang.annotation.*;



/**
* @Description: 自动注入
* @Author: JeremyKing
*/
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KAutowired {
	String value() default "";
}
