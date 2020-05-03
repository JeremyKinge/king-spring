package com.kingge.spring.stereotype.web;

import com.kingge.spring.stereotype.context.KComponent;

import java.lang.annotation.*;

/**
 * @Description: 控制层映射请求url
 * @Author: JeremyKing
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KRequestMapping {
	String value() default "";
}
