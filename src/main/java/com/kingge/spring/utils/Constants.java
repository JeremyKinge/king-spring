package com.kingge.spring.utils;

/**
 * @program: kingspring
 * @description: 常量类
 * @author: JeremyKing
 **/
public final class Constants {

    private Constants() {
        throw  new RuntimeException("常量类不能实例化");
    }

    //获取配置文件的key值
    public static String CONFIG_PROPERTIES_LOCATION = "contextConfigLocation";
    //获取扫描包路径的key值
    public static String SCAN_PACKAGE_KEY = "scanPackages";

}
