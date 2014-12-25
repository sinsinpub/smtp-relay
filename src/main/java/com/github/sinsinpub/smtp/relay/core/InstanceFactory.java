package com.github.sinsinpub.smtp.relay.core;

import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sinsinpub.smtp.relay.context.ApplicationVersion;
import com.github.sinsinpub.smtp.relay.core.impl.SpringContextProvider;
import com.github.sinsinpub.smtp.relay.utils.AppBootUtils;
import com.github.sinsinpub.smtp.relay.utils.JmxUtils;
import com.github.sinsinpub.smtp.relay.utils.ObjectUtil;
import com.github.sinsinpub.smtp.relay.utils.SystemUtil;

/**
 * 应用程序的实例管理工厂。
 * 
 * @author sin_sin
 */
@ThreadSafe
public class InstanceFactory implements InstanceFactoryMBean {

    /** 静态Logger */
    private static final Logger logger = LoggerFactory.getLogger(InstanceFactory.class);
    /** 需要动态加载的配置文件位置 */
    private static final String DYNAMIC_CONF_LOC = "file:etc/mail-session-conf.xml";
    /** 工厂启动时间，即应用开始时间 */
    protected long startDate;
    /** 依赖注入容器提供者 */
    protected InstanceProvider instanceProvider;
    /** 工厂单例 */
    private static InstanceFactory INSTANCE = null;
    private static final ReentrantLock LOCK = new ReentrantLock();

    private InstanceFactory() {
        startDate = System.currentTimeMillis();
        // 默认从application.properties读取应用配置文件组
        String instanceContexts = ApplicationVersion.getInstance()
                .getAppInstanceContexts();
        if (StringUtils.isNotBlank(instanceContexts)) {
            // 添加动态配置文件到spring的小把戏，这样会依赖spring，并不建议用
            File confFile = SpringContextProvider.getResourceFile(DYNAMIC_CONF_LOC);
            if (confFile != null && confFile.isFile()
                    && !instanceContexts.contains(DYNAMIC_CONF_LOC)) {
                instanceContexts += " " + DYNAMIC_CONF_LOC;
            }

            // 实际拆分之前先把回车、换行、'\'等符号去除
            String[] configLocations = StringUtils.split(instanceContexts.replaceAll(
                    "(\\r|\\n|\\\\)", " "));
            instanceProvider = ObjectUtil.isNotNullAndEmpty(configLocations) ? new SpringContextProvider(
                    configLocations) : new SpringContextProvider();
        } else {
            instanceProvider = new SpringContextProvider();
        }
        registerSelfToMBeanServer();
    }

    /**
     * 以编程方式将本工厂实例注册到平台MBeanServer，用作应用生命周期的管理
     */
    protected void registerSelfToMBeanServer() {
        JmxUtils.registerMBean(this, OBJECT_NAME);
    }

    /**
     * @return 工厂实例
     */
    public static InstanceFactory getFactory() {
        if (INSTANCE == null) {
            LOCK.lock();
            try {
                if (INSTANCE == null) {
                    INSTANCE = new InstanceFactory();
                }
            } finally {
                LOCK.unlock();
            }
        }
        return INSTANCE;
    }

    /**
     * 获取指定类型的对象实例。
     * 
     * @param <T> 对象的类型
     * @param beanClass 对象的类
     * @return 类型为T的对象实例
     */
    public <T> T getInstance(Class<T> beanClass) {
        return instanceProvider.getInstance(beanClass);
    }

    /**
     * 获取指定类型指定名称的实例。
     * 
     * @param beanClass Bean类
     * @param beanName Bean名称或ID
     * @return 与指定类型相同的Bean实例
     */
    public <T> T getInstance(Class<T> beanClass, String beanName) {
        return instanceProvider.getInstance(beanClass, beanName);
    }

    /**
     * @return 实例提供者实例
     */
    public InstanceProvider getInstanceProvider() {
        return instanceProvider;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 注意对于spring来说，refresh意味着会销毁所有单例Bean，并重新创建。<br>
     * 所以要小心不要在销毁之后因所有非守护线程结束而导致JVM进程结束。
     */
    public void reload() {
        AppBootUtils.initLogger();
        instanceProvider.refresh();
        logger.info("Application context refreshed at: {}",
                DateFormatUtils.format(getRefreshDate(),
                        "yyyy-MM-dd HH:mm:ss Z E"));
        logger.info("Server is currently listening on {}",
                getListenerBindAddress());
    }

    public void shutdown() {
        logger.info("Shutting down server...");
        instanceProvider.close();
    }

    public Date getStartupDate() {
        return new Date(this.startDate);
    }

    public Date getRefreshDate() {
        return new Date(
                ((SpringContextProvider) instanceProvider).getApplicationContext()
                        .getStartupDate());
    }

    public String getUpTime() {
        return DurationFormatUtils.formatDurationHMS(SystemUtil.getUpTime());
    }

    public String getLoadAverage() {
        return SystemUtil.getSystemLoadAverage();
    }

    public String getHeapMemoryUsage() {
        return NumberFormat.getPercentInstance().format(
                SystemUtil.getHeapUsage());
    }

    public String getOpenFilesUsage() {
        return NumberFormat.getPercentInstance().format(
                SystemUtil.getOpenFilesUsage());
    }

    public int getUserThreadCount() {
        return SystemUtil.getThreadCount() - SystemUtil.getDaemonThreadCount();
    }

    public String getApplicationName() {
        return ApplicationVersion.getInstance().getApplicationName();
    }

    public String getApplicationVersion() {
        return ApplicationVersion.getInstance().getApplicationVersion();
    }

    public String getApplicationVersionMeta() {
        return ApplicationVersion.getInstance().toString();
    }

    public String getInstanceRevision() {
        return StringUtils.split(ApplicationVersion.getInstance()
                .getScmVersion(), ',')[0];
    }

    public String getListenerBindAddress() {
        return String.valueOf(JmxUtils.getAttribute(
                "com.github.sinsinpub.smtp.relay:type=Frontend,name=SmtpListener",
                "DisplayableBindAddress"));
    }

    public String getJmxRmiAddress() {
        return String.valueOf(JmxUtils.getAttribute(
                "javax.management.remote.rmi:name=jmxRmiConnectorServer,type=RMIConnectorServer",
                "Address"));
    }

    public Date fromTimestamp(long timestamp) {
        return new Date(timestamp);
    }

    public long toTimestamp(String time) {
        try {
            return DateUtils.parseDate(
                    time,
                    new String[] { "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd",
                            "ddMMMyy HHmm", "ddMMMyy" }).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    public Date fromTimestampDiff(String time, long diff) {
        return fromTimestamp(toTimestamp(time) + diff);
    }

    public String formatDuration(long diff) {
        return DurationFormatUtils.formatDuration(diff, null);
    }

}
