package com.kingge.spring.test.controller;

import com.kingge.spring.stereotype.context.KAutowired;
import com.kingge.spring.stereotype.context.KController;
import com.kingge.spring.stereotype.web.KRequestMapping;
import com.kingge.spring.test.service.UserService;

/**
 * @program: kingspring
 * @description: 用户控制层
 * @author: JeremyKing
 * @create: 2020-05-03 19:20
 **/
@KController
@KRequestMapping
public class UserController {

    @KAutowired
    public UserService userService;

    @KRequestMapping(value = "/getUserList")
    public  void  getUserList(String name){
        System.out.println("获取请求的name："+name);
    }

}
