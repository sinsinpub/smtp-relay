package com.github.sinsinpub.smtp.relay.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * 应用初始化或系统信息相关的工具方法
 * 
 * @author sin_sin
 */
public abstract class AppBootUtils {

    /** Logger */
    private static Logger logger;

    /**
     * 调整VM内部参数，如语言、时区、默认字符集为天朝所用。
     */
    public static void initVmDefault() {
        System.setProperty("file.encoding", "UTF-8");
        Locale.setDefault(Locale.US);
        TimeZone zone = TimeZone.getTimeZone("GMT+8");
        TimeZone.setDefault(zone);
    }

    /**
     * 加载logger配置，让状态信息能开始输出到日志。
     * <p>
     * 当前支持logback，提供高性能日志记录。
     */
    public synchronized static Logger initLogger() {
        InputStream cfgXml = null;
        // 初始化logback绑定的配置
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            cfgXml = new FileInputStream("etc/logback.xml");
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(cfgXml);
        } catch (IOException ioe) {
        } catch (JoranException je) {
            // 因为已reset，可能需要重新载入默认配置
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        IOUtils.closeQuietly(cfgXml);

        // 取得Logger接口实例，当前直接得到slf4j门面
        logger = LoggerFactory.getLogger(AppBootUtils.class);
        return logger;
    }

    /**
     * 打印系统环境和虚拟机信息
     * <p>
     * 注意需要先调用过{@link #initLogger()}方法之后，logger才会有效。
     */
    public static void printSystemInfo() {
        if (logger == null)
            return;
        logger.info("Operating system: {}", SystemUtil.getOperatingSystem());
        logger.info("Threads of CPU: {}", SystemUtil.getAvailableProcessors());
        logger.info("JVM version: {}", SystemUtil.getJvmVersion());
        logger.info("JVM arguments: {}", SystemUtil.getJvmArguments());
        logger.info("Classpath: {}", SystemUtil.getClasspath());
        logger.info("Working dir: {}", SystemUtil.getUserDir());
        logger.info("Current charset: {}", SystemUtil.getFileEncoding());
        logger.info("Current locale: {}", Locale.getDefault());
        logger.info("Current timezone: {}", TimeZone.getDefault()
                .getDisplayName());
    }

}
