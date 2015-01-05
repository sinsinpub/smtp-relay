package com.github.sinsinpub.smtp.relay.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

public abstract class ResourcesUtils {

    private static ClassLoader defaultClassLoader;
    private static String defaultEncoding = System.getProperty("file.encoding",
            "UTF-8");

    private ResourcesUtils() {
    }

    public static ClassLoader getDefaultClassLoader() {
        return defaultClassLoader;
    }

    public static void setDefaultClassLoader(ClassLoader defaultClassLoader) {
        ResourcesUtils.defaultClassLoader = defaultClassLoader;
    }

    public static URL getResourceURL(String resource) throws IOException {
        return getResourceURL(getClassLoader(), resource);
    }

    public static URL getResourceURL(ClassLoader loader, String resource)
            throws IOException {
        URL url = null;
        if (loader != null)
            url = loader.getResource(resource);
        if (url == null)
            url = ClassLoader.getSystemResource(resource);
        if (url == null)
            throw new IOException("Could not find resource " + resource);
        return url;
    }

    public static InputStream getResourceAsStream(String resource)
            throws IOException {
        return getResourceAsStream(getClassLoader(), resource);
    }

    public static InputStream getResourceAsStream(ClassLoader loader,
            String resource) throws IOException {
        InputStream in = null;
        if (loader != null)
            in = loader.getResourceAsStream(resource);
        if (in == null)
            in = ClassLoader.getSystemResourceAsStream(resource);
        if (in == null)
            throw new IOException("Could not find resource " + resource);
        return in;
    }

    public static Properties getResourceAsProperties(String resource)
            throws IOException {
        Properties props = new Properties();
        InputStream in = null;
        String propfile = resource;
        in = getResourceAsStream(propfile);
        props.load(in);
        in.close();
        return props;
    }

    public static Properties getResourceAsProperties(ClassLoader loader,
            String resource) throws IOException {
        Properties props = new Properties();
        InputStream in = null;
        String propfile = resource;
        in = getResourceAsStream(loader, propfile);
        props.load(in);
        in.close();
        return props;
    }

    public static Reader getResourceAsReader(String resource)
            throws IOException {
        return new InputStreamReader(getResourceAsStream(resource),
                defaultEncoding);
    }

    public static Reader getResourceAsReader(ClassLoader loader, String resource)
            throws IOException {
        return new InputStreamReader(getResourceAsStream(loader, resource),
                defaultEncoding);
    }

    public static File getResourceAsFile(String resource) throws IOException {
        return new File(getResourceURL(resource).getFile());
    }

    public static File getResourceAsFile(ClassLoader loader, String resource)
            throws IOException {
        return new File(getResourceURL(loader, resource).getFile());
    }

    public static InputStream getUrlAsStream(String urlString)
            throws IOException {
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        return conn.getInputStream();
    }

    public static Reader getUrlAsReader(String urlString) throws IOException {
        return new InputStreamReader(getUrlAsStream(urlString), defaultEncoding);
    }

    public static Properties getUrlAsProperties(String urlString)
            throws IOException {
        Properties props = new Properties();
        InputStream in = null;
        String propfile = urlString;
        in = getUrlAsStream(propfile);
        props.load(in);
        in.close();
        return props;
    }

    public static Class<?> classForName(String className)
            throws ClassNotFoundException {
        Class<?> clazz = null;
        try {
            clazz = getClassLoader().loadClass(className);
        } catch (Exception e) {
            // Ignore. Fail-safe below.
        }
        if (clazz == null) {
            clazz = Class.forName(className);
        }
        return clazz;
    }

    public static Object instantiate(String className)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        return instantiate(classForName(className));
    }

    public static Object instantiate(Class<?> clazz)
            throws InstantiationException, IllegalAccessException {
        return clazz.newInstance();
    }

    private static ClassLoader getClassLoader() {
        if (defaultClassLoader != null) {
            return defaultClassLoader;
        } else {
            return Thread.currentThread().getContextClassLoader();
        }
    }

}
