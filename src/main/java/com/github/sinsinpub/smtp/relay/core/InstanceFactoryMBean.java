package com.github.sinsinpub.smtp.relay.core;

import java.util.Date;

/**
 * InstanceFactory的MBean定义接口
 * <p>
 * 所谓MBean就是管理(Management)用的JavaBean，不懂的请自行脑补。标准的非ModelMBean需要像这样先定义以MBean为后缀接口。
 * 
 * @author sin_sin
 */
public interface InstanceFactoryMBean {

    /** 本类的管理ObjectName */
    public static final String OBJECT_NAME = "com.github.sinsinpub.smtp.relay:type=Application,name=InstanceFactory";

    /**
     * 重载配置，刷新容器中的所有实例
     */
    void reload();

    /**
     * 关闭工厂并结束应用
     */
    void shutdown();

    /**
     * @return 工厂启动时间启动时间
     */
    Date getStartupDate();

    /**
     * @return 依赖注入容器启动或最后刷新时间
     */
    Date getRefreshDate();

    /**
     * @return 应用在线运行持续时间
     */
    String getUpTime();

    /**
     * @return 系统核心个数和平均负载值
     */
    String getLoadAverage();

    /**
     * @return 应用内存消耗百分比
     */
    String getHeapMemoryUsage();

    /**
     * @return 系统打开文件句柄消耗百分比
     */
    String getOpenFilesUsage();

    /**
     * @return 进程内除守护线程之外的用户线程数
     */
    int getUserThreadCount();

    /**
     * @return 应用程序名称
     */
    String getApplicationName();

    /**
     * @return 应用程序版本号
     */
    String getApplicationVersion();

    /**
     * @return 应用程序所有版本元数据
     */
    String getApplicationVersionMeta();

    /**
     * @return 本实例源代码版本修订号
     */
    String getInstanceRevision();

    /**
     * @return 查询服务监听地址
     */
    String getListenerBindAddress();

    /**
     * @return RMI协议远程连接地址
     */
    String getJmxRmiAddress();

    /**
     * 将时间戳转成日期时间
     * 
     * @param timestamp
     * @return Date
     */
    Date fromTimestamp(long timestamp);

    /**
     * 将日期时间转成时间戳
     * 
     * @param time
     * @return long，出错时返回0
     */
    long toTimestamp(String time);

    /**
     * 将时间转成时间戳后加上一个时差再转回日期时间
     * 
     * @param time
     * @param diff
     * @return Date
     */
    Date fromTimestampDiff(String time, long diff);

    /**
     * 将以毫秒表示的时间差转成可阅读的文本
     * 
     * @param diff
     * @return ISO格式文本
     */
    String formatDuration(long diff);

}
