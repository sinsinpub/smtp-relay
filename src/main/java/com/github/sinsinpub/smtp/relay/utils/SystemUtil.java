package com.github.sinsinpub.smtp.relay.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;

import org.apache.commons.lang.SystemUtils;

/**
 * Extended system utilities.
 * 
 * @see org.apache.commons.lang.SystemUtils
 * @author sin_sin
 */
public abstract class SystemUtil extends SystemUtils {

	/** The System property key for the Java encoding charset. */
	private static final String FILE_ENCODING = "file.encoding";

	/**
	 * <p>
	 * Gets a System property, defaulting to <code>null</code> if the property
	 * cannot be read.
	 * </p>
	 * 
	 * @param property
	 *            the system property name
	 * @return the system property value or <code>null</code> if a security
	 *         problem occurs
	 * @see org.apache.commons.lang.SystemUtils#getSystemProperty(String)
	 */
	public static String getSystemProperty(String property) {
		try {
			return System.getProperty(property);
		} catch (SecurityException ex) {
			return null;
		}
	}

	/**
	 * @return 虚拟机运行环境默认编码字符集
	 */
	public static String getFileEncoding() {
		return getSystemProperty(FILE_ENCODING);
	}

	/**
	 * @return 操作系统名称、版本与架构
	 */
	public static String getOperatingSystem() {
		OperatingSystemMXBean osMxbean = ManagementFactory.getOperatingSystemMXBean();
		return new StringBuilder().append(osMxbean.getName())
				.append(" ")
				.append(osMxbean.getVersion())
				.append(" ")
				.append(osMxbean.getArch())
				.toString();
	}

	/**
	 * @return CPU可用处理器数
	 */
	public static int getAvailableProcessors() {
		return ManagementFactory.getOperatingSystemMXBean()
				.getAvailableProcessors();
	}

	/**
	 * @return 虚拟机名称与版本
	 */
	public static String getJvmVersion() {
		RuntimeMXBean rtMxbean = ManagementFactory.getRuntimeMXBean();
		return new StringBuilder().append(rtMxbean.getVmName())
				.append(" ")
				.append(rtMxbean.getVmVersion())
				.toString();
	}

	/**
	 * @return 虚拟机启动命令行参数
	 */
	public static String getJvmArguments() {
		return String.valueOf(ManagementFactory.getRuntimeMXBean()
				.getInputArguments());
	}

	/**
	 * @return 虚拟机CLASSPATH
	 */
	public static String getClasspath() {
		return ManagementFactory.getRuntimeMXBean().getClassPath();
	}

	/**
	 * @return 虚拟机开始运行的时间，单位毫秒
	 */
	public static long getUpTime() {
		RuntimeMXBean rtMxbean = ManagementFactory.getRuntimeMXBean();
		return rtMxbean.getUptime();
	}

	/**
	 * @return 虚拟机Heap内存使用百分比
	 */
	public static double getHeapUsage() {
		final MemoryUsage usage = ManagementFactory.getMemoryMXBean()
				.getHeapMemoryUsage();
		return usage.getUsed() / (double) usage.getMax();
	}

	/**
	 * @return 虚拟机Heap内存分配最大值
	 */
	public static long getHeapMemoryMax() {
		final MemoryUsage usage = ManagementFactory.getMemoryMXBean()
				.getHeapMemoryUsage();
		return usage.getMax();
	}

	/**
	 * @return 虚拟机Heap内存已使用值
	 */
	public static long getHeapMemoryUsed() {
		final MemoryUsage usage = ManagementFactory.getMemoryMXBean()
				.getHeapMemoryUsage();
		return usage.getUsed();
	}

	/**
	 * @return 虚拟机Heap内存已提交值
	 */
	public static long getHeapMemoryCommitted() {
		final MemoryUsage usage = ManagementFactory.getMemoryMXBean()
				.getHeapMemoryUsage();
		return usage.getCommitted();
	}

	/**
	 * @return 操作系统物理内存总值，无法检测到值时返回-1
	 */
	public static long getTotalPhysicalMemory() {
		final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		Long value = (Long) ObjectUtil.invokePrivateMethod(os,
				"getTotalPhysicalMemorySize", null);
		return value != null ? value : -1L;
	}

	/**
	 * @return 操作系统物理内存空余值，无法检测到值时返回-1
	 */
	public static long getFreePhysicalMemory() {
		final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		Long value = (Long) ObjectUtil.invokePrivateMethod(os,
				"getFreePhysicalMemorySize", null);
		return value != null ? value : -1L;
	}

	/**
	 * @return 操作系统打开文件句柄使用百分比，无法检测到值时返回NaN
	 */
	public static double getOpenFilesUsage() {
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		try {
			final Long openFds = (Long) ObjectUtil.invokePrivateMethod(os,
					"getOpenFileDescriptorCount", null);
			final Long maxFds = (Long) ObjectUtil.invokePrivateMethod(os,
					"getMaxFileDescriptorCount", null);
			return openFds.doubleValue() / maxFds.doubleValue();
		} catch (RuntimeException e) {
			return Double.NaN;
		}
	}

	/**
	 * @return 虚拟机内当前总共活动的线程数
	 */
	public static int getThreadCount() {
		return ManagementFactory.getThreadMXBean().getThreadCount();
	}

	/**
	 * @return 虚拟机内当前共活动的守护线程数
	 */
	public static int getDaemonThreadCount() {
		return ManagementFactory.getThreadMXBean().getDaemonThreadCount();
	}

	/**
	 * 获取当前操作系统的CPU核心数和平均负载。只适用于JVM运行于*NIX系统上，不支持的OS负载会返回-1。
	 * 
	 * @return CPU核心数+":"+平均负载
	 */
	public static String getSystemLoadAverage() {
		OperatingSystemMXBean osMxbean = ManagementFactory.getOperatingSystemMXBean();
		double loadAvg = osMxbean.getSystemLoadAverage();
		int cores = osMxbean.getAvailableProcessors();
		return new StringBuilder(String.valueOf(cores)).append(":").append(
				String.valueOf(loadAvg)).toString();
	}

}
