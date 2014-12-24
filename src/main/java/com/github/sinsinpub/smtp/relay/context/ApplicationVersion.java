package com.github.sinsinpub.smtp.relay.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Properties;

import javax.annotation.concurrent.Immutable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateFormatUtils;

/**
 * 应用程序源码、构建、版本、实例配置等元数据信息容器。
 * <p>
 * 信息来自META-INF/application.properties，部分收集自maven、jenkins等。<br>
 * 所以最少需要经过maven构建信息才会更新。要经过jenkins才有会CI相关信息。
 * 
 * @author sin_sin
 */
@Immutable
public class ApplicationVersion implements Serializable, Cloneable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    /** 应用程序包起始位置 */
    public static final String PACKAGE_ROOT = StringUtils.removeEnd(
            ApplicationVersion.class.getPackage().getName(), ".context");
    /** 应用属性文件默认位置 */
    public static final String APP_PROPS_FILE = "/META-INF/application.properties";
    /** 默认实例 */
    private static final ApplicationVersion INSTANCE = new ApplicationVersion();

    /** 本模块的组标识 */
    private String projectGroupId;
    /** 本模块的标识 */
    private String projectArtifactId;
    /** 本模块的版本号 */
    private String projectVersion;
    /** 应用程序名称 */
    private String applicationName;
    /** 应用程序版本 */
    private String applicationVersion;
    /** 构建应用时使用的配置 */
    private String buildProfile;
    /** 构建应用时版本控制管理器的版本号标识 */
    private String scmVersion;
    /** 构建应用时的持续整合管理器的版本号标识 */
    private String ciVersion;
    /** 应用实例上下文配置文件列表 */
    private String appInstanceContexts;

    /**
     * @return 返回默认实例
     */
    public static ApplicationVersion getInstance() {
        return INSTANCE;
    }

    /**
     * 创建新实例并立即加载属性
     */
    public ApplicationVersion() {
        loadProperties(null);
    }

    /**
     * 从属性文件加载属性值
     * 
     * @param propsFile 指定属性文件，为null时从默认位置加载
     * @see #APP_PROPS_FILE
     */
    protected void loadProperties(String propsFile) {
        InputStream is = getClass().getResourceAsStream(
                StringUtils.defaultIfBlank(propsFile, APP_PROPS_FILE));
        Properties app = new Properties();
        try {
            app.load(is);
        } catch (IOException e) {
            // 加载文件失败
            return;
        } finally {
            IOUtils.closeQuietly(is);
        }
        projectGroupId = app.getProperty("project.groupId");
        projectArtifactId = app.getProperty("project.artifactId");
        projectVersion = app.getProperty("project.version");
        applicationName = app.getProperty("application.name");
        applicationVersion = app.getProperty("application.version");
        buildProfile = app.getProperty("build.profile");
        String scmRevision = app.getProperty("scm.revision");
        scmVersion = scmRevision + "," + app.getProperty("scm.branch");
        // 将时间戳变成可读时间
        String timestamp = app.getProperty("scm.timestamp");
        if (NumberUtils.isNumber(timestamp)) {
            long date = NumberUtils.toLong(timestamp);
            scmVersion += ","
                    + DateFormatUtils.format(new Date(date),
                            "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        }
        ciVersion = app.getProperty("ci.buildTag") + ","
                + app.getProperty("ci.buildId");
        // 将构建号和版本控制号加入应用版本标识
        String buildNumber = app.getProperty("ci.buildNumber");
        if (StringUtils.isNotBlank(buildNumber)
                && !StringUtils.equals(buildNumber, "${BUILD_NUMBER}")) {
            applicationVersion += ".b" + buildNumber;
        }
        if (StringUtils.isNotBlank(scmRevision)
                && !StringUtils.equals(scmRevision, "${buildNumber}")) {
            applicationVersion += ".r" + scmRevision;
        }
        String instanceContexts = app.getProperty("instance.contexts");
        if (StringUtils.isNotBlank(instanceContexts)
                && !StringUtils.equals(instanceContexts, "${config.locations}")) {
            appInstanceContexts = instanceContexts;
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public String getProjectGroupId() {
        return projectGroupId;
    }

    public String getProjectArtifactId() {
        return projectArtifactId;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public String getBuildProfile() {
        return buildProfile;
    }

    public String getScmVersion() {
        return scmVersion;
    }

    public String getCiVersion() {
        return ciVersion;
    }

    public String getAppInstanceContexts() {
        return appInstanceContexts;
    }

}
