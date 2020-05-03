package com.kingge.spring.stereotype.context;

import java.lang.annotation.*;

/**
 * @Description: 控制层
 * @Author: JeremyKing
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KController {
	String value() default "";
}
