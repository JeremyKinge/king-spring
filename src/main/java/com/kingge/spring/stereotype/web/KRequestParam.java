package com.kingge.spring.stereotype.web;

import java.lang.annotation.*;


/**
 * @Description: 控制层请求参数映射
 * @Author: JeremyKing
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KRequestParam {
	
	String value() default "";
	
	boolean required() default true;

}
