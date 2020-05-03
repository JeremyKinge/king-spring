package com.kingge.spring.stereotype.context;

import java.lang.annotation.*;

/**
 * @Description: 持久层层逻辑,注入接口
 * @Author: JeremyKing
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KRepository {
	String value() default "";
}
