package com.github.sinsinpub.smtp.relay.core.impl;

import java.io.File;
import java.io.FileNotFoundException;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.ResourceUtils;

import com.github.sinsinpub.smtp.relay.core.InstanceProvider;

/**
 * 依赖注入容器：Spring的提供者实现
 * <p>
 * 目前容器的所有配置详情查看src/main/resources下以applicationContext开头的XML文件。
 * 
 * @author sin_sin
 */
@ThreadSafe
public class SpringContextProvider implements InstanceProvider {

    /** 静态Logger */
    private static final Logger logger = LoggerFactory.getLogger(SpringContextProvider.class);
    /** 应用基础默认配置文件，其中的实例生命周期与应用相同 */
    static final String[] BASE_CONTEXT_FILES = { "applicationContext-base.xml",
            "applicationContext-mbeanServer.xml" };
    /** 当前应用默认的配置文件，其中的实例会因为重载配置而被重建 */
    static final String[] APP_CONTEXT_FILES = { "applicationContext-base.xml",
            "applicationContext-frontend.xml", "applicationContext-backend.xml" };
    /** 父Spring上下文 */
    @GuardedBy("itself")
    protected AbstractApplicationContext baseApplicationContext;
    /** 应用Spring上下文 */
    @GuardedBy("itself")
    protected AbstractApplicationContext applicationContext;

    /**
     * 使用默认配置文件创建实例
     */
    public SpringContextProvider() {
        this(APP_CONTEXT_FILES);
    }

    /**
     * 使用提供的配置文件创建实例
     * 
     * @param configLocations 配置文件数组
     */
    public SpringContextProvider(String... configLocations) {
        try {
            baseApplicationContext = new ClassPathXmlApplicationContext(
                    BASE_CONTEXT_FILES);
            baseApplicationContext.registerShutdownHook();
            applicationContext = new ClassPathXmlApplicationContext(
                    configLocations, baseApplicationContext);
            applicationContext.registerShutdownHook();
        } catch (BeansException e) {
            logger.error(e.toString(), e);
            if (applicationContext != null) {
                applicationContext.close();
            }
            logger.info("Exit with code: -1");
            System.exit(-1);
        }
    }

    @Override
    public <T> T getInstance(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    @Override
    public <T> T getInstance(Class<T> beanClass, String beanName) {
        return applicationContext.getBean(beanName, beanClass);
    }

    @Override
    public void refresh() {
        applicationContext.refresh();
    }

    @Override
    public void close() {
        applicationContext.close();
        baseApplicationContext.close();
    }

    @Override
    public boolean isActive() {
        return baseApplicationContext.isActive()
                && applicationContext.isActive();
    }

    /**
     * @return 返回Spring上下文
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 利用spring资源文件地址规则获取资源文件实例
     * 
     * @param resourceLocation
     * @return 如果指定位置上找不到该文件则返回null
     */
    public static File getResourceFile(String resourceLocation) {
        try {
            return ResourceUtils.getFile(resourceLocation);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

}
