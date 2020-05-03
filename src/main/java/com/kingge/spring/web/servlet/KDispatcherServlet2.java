package com.kingge.spring.web.servlet;


import com.kingge.spring.stereotype.context.*;
import com.kingge.spring.stereotype.web.KRequestMapping;
import com.kingge.spring.utils.Constants;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
* @Description: 控制类
* @Author: JeremyKing
* @Date: 2020/3/3
*/
public class KDispatcherServlet2 extends HttpServlet {

    private  final String CONFIG_PROPERTIES_LOCATION = Constants.CONFIG_PROPERTIES_LOCATION;
    private  final String SCAN_PACKAGE_KEY = Constants.SCAN_PACKAGE_KEY;

    //保存解析application.properties 的配置文件信息
    private Properties contextConfig = new Properties();
    //保存扫描到的所有bean的全类名（还未过滤，可能包含不需要注入到ioc容器的bean）
    private List<String> classNames = new ArrayList<String>();

    //IOC容器中的bean，classNames经过了过滤后得到的
    private Map<String,Object> beanDefinitionMap = new ConcurrentHashMap<String,Object>(256);

    //保存控制层，url跟处理方法的映射关系
    private Map<String,Method> handlerMapping = new ConcurrentHashMap<String,Method>();

    //课后再去思考一下这样设计的经典之处
    //GPHandlerMapping最核心的设计，也是最经典的
    //它牛B到直接干掉了Struts、Webwork等MVC框架
//    private List<KHandlerMapping> handlerMappings = new ArrayList<KHandlerMapping>();
//
//    private Map<KHandlerMapping, KHandlerAdapter> handlerAdapters = new HashMap<KHandlerMapping, KHandlerAdapter>();
//
//    private List<KViewResolver> viewResolvers = new ArrayList<KViewResolver>();

    @Override
    public void init(ServletConfig config) throws ServletException {

        System.out.println("[ init ]  start call  ....");

        //1.加载配置文件
            loadConfig(config.getInitParameter(CONFIG_PROPERTIES_LOCATION));

        //2.根据配置文件配置的扫描路径，扫描类
            scanPackage(contextConfig.getProperty(SCAN_PACKAGE_KEY));

        //3.筛选上面扫描的所有类，将标有注入注解的类，放到IOC容器中
        //类似于解析xml的bean标签
            initContext();

        //4.解析IOC容器中的bean，并设置依赖 - DI阶段
            diBean();

        //5.初始化handlerMapping，解析所有控制类的url地址和相关联的处理method
            initHandlerMapping();

        Properties contextConfig1 =    this.contextConfig;
        List<String> classNames1 =    this.classNames;
        Map<String,Object> beanDefinitionMap1 =    this.beanDefinitionMap;
        Map<String,Method> handlerMapping1 =    this.handlerMapping;

        System.out.println("[ init ]  end ....");

    }


    /**
    * @Description: 加载配置文件
    * @Param:
    * @return:
    * @Author: JeremyKing
     * @param initParameter
    */
    private void loadConfig(String initParameter) {
        InputStream is = this.getClass().getClassLoader()
                .getResourceAsStream(initParameter.replace("classpath:",""));

        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(null != is){is.close();}
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
    * @Description: 根据配置文件配置的扫描路径，扫描类
    * @Param:
    * @return:
    * @Author: JeremyKing
    */
    private void scanPackage(String pcn) {
        //pcn是个包路径，例如com.kingge.spring,那么当编译后
        //类文件保存在class目录下，那么编译后的bean的目录结构应该是
        //com/kingge/spring，所以需要替换一下
        URL url = this.getClass().getClassLoader()
                .getResource("/" + pcn.replaceAll("\\.","/"));

        File classDir = new File(url.getFile());

        for (File file : classDir.listFiles()){//获取当前目录下所有文件/目录
            if(file.isDirectory()){//如果是目录，那么递归获取目录下的所有bean
                scanPackage(pcn + "." +file.getName());
            }else {//否则说明是class文件，那么只需要获取bean的name即可
                //拼凑bean文件的全路径，例如com.kingge.spring.dao.UserDao
                classNames.add(pcn + "." + file.getName().replace(".class",""));
            }
        }
    }


    /**
    * @Description:筛选上面扫描的所有类，将标有注入注解的类，放到IOC容器中
    * @Param:
    * @return:
    * @Author: JeremyKing
    */
    private void initContext() {
        //1.扫描的所有类为空，则直接返回
        if(classNames.isEmpty()){ return;}

        try{
            //2.遍历所有类，将加了标识注入的注解的类实例化后，放入ioc容器中
            for (String className : classNames) {
                //获取当前类的类信息
                Class<?> clazz = Class.forName(className);
                //将标注了，这些注解的bean，放到ioc容器中
                if(clazz.isAnnotationPresent(KController.class)||
                        clazz.isAnnotationPresent(KService.class)||
                        clazz.isAnnotationPresent(KRepository.class)||
                        clazz.isAnnotationPresent(KComponent.class)){

                    //1.检查当前注解是否配置了beanName
                    //2.没有设置beanName，那么默认使用类名称的首字母小写
                    String beanName = getAnnotationValue(clazz);
                    
                    //在Spring中在这个阶段不是不会直接put instance，这里put的是BeanDefinition
                    //value中只是放置了这个bean的一些额外信息，例如作用域是单例还是多例
                    //是不是懒加载等等，这样的好处是到用时采取new实例，减少堆空间不必要浪费
                    Object instance = clazz.newInstance();
                    beanDefinitionMap.put(beanName,instance);//我们这里直接new实例放置

                    //如果当前bean实现了接口，那么直接使用接口的名称为key
                    // 接口的实现类为value（也就是当前bean实例），放到ioc容器中
                    //接口可能多个实现类，那么怎么办呢？
                    //判断如果ioc容器中已经存在相同的key，则抛异常
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i :interfaces){
                        if( beanDefinitionMap.containsKey(i.getName()) ){
                            throw new RuntimeException(i.getName() + " already exists  ");
                        }
                        beanDefinitionMap.put(i.getName(),instance);
                    }
                }

            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }


    /**
    * @Description: 解析IOC容器中的bean，并设置依赖 - DI阶段
    * @Param: 
    * @return: 
    * @Author: JeremyKing
    */
    private void diBean() {

        if(beanDefinitionMap.isEmpty()){ return; }

        //1.遍历所有ioc容器中的bean
        for (Map.Entry<String,Object> entry : beanDefinitionMap.entrySet()) {

            //1.1获取当前bean的所有成员属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields){
                //1.1.1当前成员属性如果没有表示注入注解，那么无需注入
                if(!field.isAnnotationPresent(KAutowired.class)){continue;}

                KAutowired kautowried = field.getAnnotation(KAutowired.class);

                //1.1.2 判断当前autowire注解是否声明指定注入的beanName
                String beanName = kautowried.value().trim();
                if("".equals(beanName)){//否则，默认取得依赖类的类名为beanName
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);//避免当前属性是非public修饰，那么需要添加true
                //设置为可访问

                try {
                    //设置依赖
                    field.set(entry.getValue(),beanDefinitionMap.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }

        }
    }
    
    /**
    * @Description: 初始化handlerMapping，解析所有控制类的url地址和相关联的处理method
    * @Param:
    * @return:
    * @Author: JeremyKing
    */
    private void initHandlerMapping() {

        //按照我们通常的理解应该是一个Map
        //Map<String,Method> map;
        //map.put(url,Method)
        if(beanDefinitionMap.isEmpty()){ return; }
        //首先从容器中取到所有的实例
        for (Map.Entry<String, Object> objectEntry : beanDefinitionMap.entrySet()) {
            Class<?> clazz = objectEntry.getValue().getClass();
            if (!clazz.isAnnotationPresent(KController.class)) {continue;}

            String baseUrl = "";
            //判断当前控制类，是否设置了访问前缀
            if (clazz.isAnnotationPresent(KRequestMapping.class)) {
                KRequestMapping requestMapping = clazz.getAnnotation(KRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            //扫描所有的public方法，拼接baseUrl
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(KRequestMapping.class)) {continue; }

                KRequestMapping requestMapping = method.getAnnotation(KRequestMapping.class);
                String  url = ("/"+baseUrl + "/" +requestMapping.value() )
                        .replaceAll("/+","/");
//                String regex = ("/" + baseUrl + requestMapping.value().replaceAll("\\*", ".*")).replaceAll("/+", "/");
//                Pattern pattern = Pattern.compile(regex);
                this.handlerMapping.put(url.toString(), method);
                System.out.println("Mapping: " + url + " , " + method);

            }
        }
    }


    /**
    * @Description: 获得注解设置的value属性的值
    * @Param: 
    * @return: 
    * @Author: JeremyKing
    * @Date: 2020/5/3
     * @param clazz
    */
    private String getAnnotationValue(Class<?> clazz){

        String defaultBeanName = "";
        if(clazz.isAnnotationPresent(KController.class)){
            KController  kController= clazz.getAnnotation(KController.class);
            defaultBeanName = kController.value();
        }
        if(clazz.isAnnotationPresent(KService.class)){
            KService  kService= clazz.getAnnotation(KService.class);
            defaultBeanName = kService.value();
        }
        if(clazz.isAnnotationPresent(KRepository.class)){
            KRepository  kRepository= clazz.getAnnotation(KRepository.class);
            defaultBeanName = kRepository.value();
        }
        if(clazz.isAnnotationPresent(KComponent.class)){
            KComponent  kComponent= clazz.getAnnotation(KComponent.class);
            defaultBeanName = kComponent.value();
        }

        if("".equals(defaultBeanName.trim())){
            return lowerFirstCase(clazz.getSimpleName());
        }
        return defaultBeanName;
    }

    
    //beanName首字母小写
    private String lowerFirstCase(String str){
        char [] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            //客户端处理请求
            doDispatcher(req, resp);
        }catch (Exception e){
            resp.getWriter().write("<font size='25' color='blue'>500 Exception</font><br/>Details:<br/>" + Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\]","")
                    .replaceAll("\\s","\r\n") +  "<font color='green'><i>Copyright@GupaoEDU</i></font>");
            e.printStackTrace();
        }
    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) {


        if(this.handlerMapping.isEmpty()){ return  ;}


        String url = req.getRequestURI();//获得绝对路径
        //处理成相对路径
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");


        try {
            if( !this.handlerMapping.containsKey(url) ){
                resp.getWriter().write("找不到处理类");
                return;
            }

            //处理请求
            Method method = this.handlerMapping.get(url);
            //那么怎么获取当前method所在的bean实例呢？
            //通过反射拿到method所在的class，然后拿到class的名称
            //再调用获取beanName，然后通过BeanDefinition获取bean实例
            String simpleName = method.getDeclaringClass().getSimpleName();
            String beanName = lowerFirstCase(simpleName);

            Map<String, String[]> parameterMap = req.getParameterMap();//获取请求参数
            //为了方便演示，这里先写死
            method.invoke(beanDefinitionMap.get(beanName),
                    new Object[]{parameterMap.get("name")[0]});


        } catch (IOException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }


//        for (GPHandlerMapping handler : this.handlerMappings) {
//            Matcher matcher = handler.getPattern().matcher(url);
//            if(!matcher.matches()){ continue;}
//            return handler;
//        }
//
//        return null;

//        //根据用户请求的URL来获得一个Handler
//        GPHandlerMapping handler = getHandler(req);
//        if(handler == null){
//            resp.getWriter().write("<font size='25' color='red'>404 Not Found</font><br/><font color='green'><i>Copyright@GupaoEDU</i></font>");
//            return;
//        }
//
//        GPHandlerAdapter ha = getHandlerAdapter(handler);
//
//
//        //这一步只是调用方法，得到返回值
//        GPModelAndView mv = ha.handle(req, resp, handler);
//
//
//        //这一步才是真的输出
//        processDispatchResult(resp, mv);

    }


}
