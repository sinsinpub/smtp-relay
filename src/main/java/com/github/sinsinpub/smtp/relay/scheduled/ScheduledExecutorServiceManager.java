package com.github.sinsinpub.smtp.relay.scheduled;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.concurrent.ScheduledExecutorFactoryBean;
import org.springframework.scheduling.concurrent.ScheduledExecutorTask;

import com.github.sinsinpub.smtp.relay.utils.ObjectUtil;

/**
 * A simple pure MBean for managing ScheduledExecutorService
 * (ScheduledExecutorFactoryBean in fact).
 * 
 * @see ScheduledExecutorFactoryBean
 * @author sin_sin
 */
@ManagedResource(objectName = ScheduledExecutorServiceManager.OBJECT_NAME, description = "A simple scheduled executor service manager.")
public class ScheduledExecutorServiceManager {

    public static final String OBJECT_NAME = "com.github.sinsinpub.smtp.relay:type=Scheduler,name=ScheduledExecutor";
    private final static Logger logger = LoggerFactory.getLogger(ScheduledExecutorServiceManager.class);

    /*
     * ScheduledExecutorFactoryBean must be injected directly. To make this, for
     * XML configuration, insert '&'(&amp;) at the beginning of the bean name.
     */
    private ScheduledExecutorFactoryBean factoryBean;

    public ScheduledExecutorServiceManager() {
        super();
    }

    @ManagedAttribute
    public boolean isRunning() {
        return !(getFactoryBean().getObject().isShutdown() || getFactoryBean().getObject()
                .isTerminated());
    }

    @ManagedOperation
    public void shutdown() {
        if (isRunning()) {
            getFactoryBean().shutdown();
            logger.info("{}ExecutorService is shut down.",
                    getFactoryBean().getThreadNamePrefix());
        } else {
            logger.info("{}ExecutorService is not running.",
                    getFactoryBean().getThreadNamePrefix());
        }
    }

    @ManagedOperation
    public void startup() {
        if (!isRunning()) {
            getFactoryBean().initialize();
            logger.info("{}ExecutorService is started up.",
                    getFactoryBean().getThreadNamePrefix());
        } else {
            logger.info("{}ExecutorService is still running.",
                    getFactoryBean().getThreadNamePrefix());
        }
    }

    @ManagedOperation
    public String[] listScheduledTasks() {
        ScheduledExecutorTask[] tasks = (ScheduledExecutorTask[]) ObjectUtil.getPrivatePropertyValue(
                getFactoryBean(), "scheduledExecutorTasks");
        if (null != tasks) {
            StringBuilder sb = new StringBuilder();
            for (ScheduledExecutorTask task : tasks) {
                sb.append(ObjectUtil.nullSafeClassName(task.getRunnable()));
                sb.append(",").append(task.getPeriod());
                sb.append(",").append(task.getDelay());
                sb.append("|");
            }
            return StringUtils.split(sb.toString(), "|");
        }
        return null;
    }

    public ScheduledExecutorFactoryBean getFactoryBean() {
        return factoryBean;
    }

    public void setFactoryBean(ScheduledExecutorFactoryBean factoryBean) {
        this.factoryBean = factoryBean;
    }

}
