package com.github.sinsinpub.smtp.relay.utils;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Helper class for the creation of {@link javax.management.ObjectName} instances.
 * 
 * @author sin_sin
 */
public abstract class ObjectNameHelper {

    /**
     * <p>
     * 生成ObjectName的最佳实践。
     * </p>
     * 格式为：
     * 
     * <pre>
     * [group]:type=[type],name=[name],[suffix]
     * </pre>
     * 
     * 方框中的内容将被实参替换。
     * 
     * @param group 组包名
     * @param type 类型名
     * @param name 对象名
     * @param suffix 附加后缀
     * @return 拼好的ObjectName
     */
    public static String newObjectName(String group, String type, String name,
            String suffix) {
        final StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(group);
        nameBuilder.append(":type=");
        nameBuilder.append(type);
        if (name != null && name.length() > 0) {
            nameBuilder.append(",name=");
            nameBuilder.append(name);
        }
        if (suffix != null && suffix.length() > 0) {
            nameBuilder.append(",");
            nameBuilder.append(suffix);
        }
        return nameBuilder.toString();
    }

    /**
     * Retrieve the <code>ObjectName</code> instance corresponding to the supplied
     * name.
     * 
     * @param objectName the <code>ObjectName</code> in <code>ObjectName</code> or
     *            <code>String</code> format
     * @return the <code>ObjectName</code> instance
     * @throws MalformedObjectNameException in case of an invalid object name
     *             specification
     * @see ObjectName#ObjectName(String)
     * @see ObjectName#getInstance(String)
     */
    public static ObjectName getInstance(Object objectName)
            throws MalformedObjectNameException {
        if (objectName instanceof ObjectName) {
            return (ObjectName) objectName;
        }
        if (!(objectName instanceof String)) {
            throw new MalformedObjectNameException(
                    "Invalid ObjectName value type ["
                            + objectName.getClass().getName()
                            + "]: only ObjectName and String supported.");
        }
        return getInstance((String) objectName);
    }

    /**
     * Retrieve the <code>ObjectName</code> instance corresponding to the supplied
     * name.
     * 
     * @param objectName the <code>ObjectName</code> in <code>String</code> format
     * @return the <code>ObjectName</code> instance
     * @throws MalformedObjectNameException in case of an invalid object name
     *             specification
     * @see ObjectName#ObjectName(String)
     * @see ObjectName#getInstance(String)
     */
    public static ObjectName getInstance(String objectName)
            throws MalformedObjectNameException {
        return ObjectName.getInstance(objectName);
    }

    /**
     * Retrieve an <code>ObjectName</code> instance for the specified domain and a
     * single property with the supplied key and value.
     * 
     * @param domainName the domain name for the <code>ObjectName</code>
     * @param key the key for the single property in the <code>ObjectName</code>
     * @param value the value for the single property in the
     *            <code>ObjectName</code>
     * @return the <code>ObjectName</code> instance
     * @throws MalformedObjectNameException in case of an invalid object name
     *             specification
     * @see ObjectName#ObjectName(String, String, String)
     * @see ObjectName#getInstance(String, String, String)
     */
    public static ObjectName getInstance(String domainName, String key,
            String value) throws MalformedObjectNameException {
        return ObjectName.getInstance(domainName, key, value);
    }

    /**
     * Retrieve an <code>ObjectName</code> instance with the specified domain name
     * and the supplied key/name properties.
     * 
     * @param domainName the domain name for the <code>ObjectName</code>
     * @param properties the properties for the <code>ObjectName</code>
     * @return the <code>ObjectName</code> instance
     * @throws MalformedObjectNameException in case of an invalid object name
     *             specification
     * @see ObjectName#ObjectName(String, java.util.Hashtable)
     * @see ObjectName#getInstance(String, java.util.Hashtable)
     */
    public static ObjectName getInstance(String domainName,
            Hashtable<String, String> properties)
            throws MalformedObjectNameException {
        return ObjectName.getInstance(domainName, properties);
    }

}
