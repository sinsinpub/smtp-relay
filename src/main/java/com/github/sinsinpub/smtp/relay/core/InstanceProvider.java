package com.github.sinsinpub.smtp.relay.core;

/**
 * 抽象出依赖注入容器提供者的接口。
 * 
 * @author sin_sin
 */
public interface InstanceProvider {

    /**
     * 返回指定类型的实例。
     * 
     * @param <T> 类型范型
     * @param beanClass 实例类型
     * @return 匹配的实例
     */
    <T> T getInstance(Class<T> beanClass);

    /**
     * 返回指定类型指定名称的实例。
     * 
     * @param <T> 类型范型
     * @param beanName 容器中配置的名称
     * @param beanClass 实例类型
     * @return 匹配的实例
     */
    <T> T getInstance(Class<T> beanClass, String beanName);

    /**
     * 刷新容器配置
     */
    void refresh();

    /**
     * 关闭容器
     */
    void close();

    /**
     * @return 是否已经激活，即是否已经刷新过配置并且尚未关闭
     */
    boolean isActive();

}
