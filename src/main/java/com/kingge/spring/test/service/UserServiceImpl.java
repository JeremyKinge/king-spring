package com.kingge.spring.test.service;

import com.kingge.spring.stereotype.context.KAutowired;
import com.kingge.spring.stereotype.context.KService;
import com.kingge.spring.test.dao.UserDao;

/**
 * @program: kingspring
 * @description: 用户业务类
 * @author: JeremyKing
 * @create: 2020-05-03 19:17
 **/

@KService
public class UserServiceImpl implements  UserService{

    @KAutowired
    public UserDao userDao;
}
