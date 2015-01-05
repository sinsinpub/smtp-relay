package com.github.sinsinpub.smtp.relay.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可以自定义线程名称前缀的新线程创建工厂.
 * <p>
 * 其实和JDK默认工厂行为一样，只是允许动态修改线程名称前缀。<br>
 * 使用本工厂创建的线程名称固定为"前缀+线程序号"，实际其中不含"+"号，且序号从1开始编号。
 * 
 * @author sin_sin
 */
public class NameCustomizableThreadFactory implements ThreadFactory {

    protected final ThreadGroup group;
    protected final AtomicInteger threadNumber = new AtomicInteger(1);
    protected String namePrefix = "UserThread-";

    /**
     * 使用指定名称前缀创建新的工厂实例。
     * 
     * @param namePrefix 新的线程名称前缀，如果不传入有效值，默认为"UserThread-"
     * @return ThreadFactory实例
     */
    public static ThreadFactory newThreadFactory(String namePrefix) {
        return new NameCustomizableThreadFactory(namePrefix);
    }

    /**
     * 默认工厂的构造方法，默认名称前缀为"UserThread-"
     */
    public NameCustomizableThreadFactory() {
        // 会从当前SecurityManager或环境中获取所在线程分组
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread()
                .getThreadGroup();
    }

    /**
     * 指定名称前缀的工厂构造方法。
     * 
     * @param namePrefix 新的线程名称前缀，如果不传入有效值则使用默认
     */
    public NameCustomizableThreadFactory(String namePrefix) {
        this();
        if (null != namePrefix && namePrefix.length() > 0) {
            this.setNamePrefix(namePrefix);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix
                + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }

    /**
     * @return 当前线程名称前缀
     */
    public String getNamePrefix() {
        return namePrefix;
    }

    /**
     * 设置线程名称前缀，默认为"UserThread-"
     * 
     * @param namePrefix 新的线程名称前缀
     */
    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    /**
     * @return 创建的线程所在分组
     */
    public ThreadGroup getGroup() {
        return group;
    }

    /**
     * @return 已经通过本工厂创建了的线程数
     */
    public Integer getThreadNumber() {
        return threadNumber.get();
    }

}
