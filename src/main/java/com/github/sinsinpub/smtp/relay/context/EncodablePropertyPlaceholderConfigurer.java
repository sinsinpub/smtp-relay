package com.github.sinsinpub.smtp.relay.context;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * <p>
 * Subclass of
 * <tt>org.springframework.beans.factory.config.PropertyPlaceholderConfigurer</tt>
 * which can make use of {@link Base64} or other algorithm to encode property
 * values if they are encoded in the loaded resource locations.
 * </p>
 * <p>
 * A value is considered "encoded" when it appears surrounded by
 * <tt>ENC(...)</tt> , like:
 * </p>
 * 
 * <pre>
 * my.value=ENC(Tm9ib2R5VG9TZWU=)
 * </pre>
 * 
 * <p>
 * Encoded and decoded objects can be combined in the same resources file.
 * </p>
 * Codes from
 * org.jasypt.spring3.properties.EncryptablePropertyPlaceholderConfigurer
 * <p>
 * 
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 * @author sin_sin
 */
public class EncodablePropertyPlaceholderConfigurer extends
		PropertyPlaceholderConfigurer {

	protected static final String ENCODED_VALUE_PREFIX = "ENC(";
	protected static final String ENCODED_VALUE_SUFFIX = ")";

	/**
	 * 除了默认Base64编码，也允许再之中插入简单的加、解密处理。不过加密方法请用子类实现。
	 */
	private boolean doEncrypt = false;

	/**
	 * 仅仅只是Base64编码，也允许反复辗转操作，增加逆向解析时的困难程度。
	 */
	private boolean doIterating = false;
	/** 迭代编码次数。注意次数太多编码会变得过长。 */
	private int iteratingTimes = 3;

	/**
	 * 判断值是否被用编码前、后缀包住
	 * 
	 * @param value
	 *            原始值
	 * @return 有ENC(...)包住的值返回true
	 */
	protected static boolean isEncryptedValue(final String value) {
		if (value == null) {
			return false;
		}
		final String trimmedValue = value.trim();
		return (trimmedValue.startsWith(ENCODED_VALUE_PREFIX) && trimmedValue.endsWith(ENCODED_VALUE_SUFFIX));
	}

	/**
	 * 截取被编码前、后缀包住的中间内容
	 * 
	 * @param value
	 *            原始值
	 * @return 去掉ENC()后的中间内容
	 */
	protected static String getInnerEncryptedValue(final String value) {
		return value == null ? null : value.trim().substring(
				ENCODED_VALUE_PREFIX.length(),
				(value.length() - ENCODED_VALUE_SUFFIX.length()));
	}

	/**
	 * 使用内定算法编码原始内容。子类可以覆盖本方法实现其它编码算法。
	 * 
	 * @param value
	 *            未编码过的文本
	 * @return 编码后的文本。编码失败会将传入文本原样返回。
	 */
	protected String encodeValue(final String value) {
		try {
			return isDoEncrypt() ? encodeBase64(encrypt(value))
					: encodeBase64(value);
		} catch (RuntimeException e) {
			return value;
		}
	}

	/**
	 * 内定Base64编码算法实现，支持迭代辗转。
	 * 
	 * @param msg
	 *            未编码过的消息
	 * @return 编码后的文本
	 */
	private String encodeBase64(String msg) {
		int c = isDoIterating() ? iteratingTimes : 1;
		String enc = msg;
		for (int i = 0; i < c; i++) {
			enc = Base64.encodeBase64String(StringUtils.getBytesUtf8(msg));
		}
		return enc;
	}

	/**
	 * 把原始文本编码并用前、后缀包起来。仅用于测试。
	 * 
	 * @param originalValue
	 *            原始文本
	 * @return 编码并包装后的文本
	 */
	protected String convertPropertyToEncoded(final String originalValue) {
		return ENCODED_VALUE_PREFIX + encodeValue(originalValue)
				+ ENCODED_VALUE_SUFFIX;
	}

	/**
	 * 使用内定算法解码原始内容。子类可以覆盖本方法实现其它编码算法。
	 * 
	 * @param value
	 *            编码过的文本
	 * @return 解码后的文本。解码失败会将传入文本原样返回。
	 */
	protected String decodeValue(final String value) {
		try {
			return isDoEncrypt() ? decrypt(decodeBase64(value))
					: decodeBase64(value);
		} catch (RuntimeException e) {
			return value;
		}
	}

	/**
	 * 内定Base64解码算法实现，支持迭代辗转。
	 * 
	 * @param msg
	 *            编码过的消息
	 * @return 解码后的文本
	 */
	private String decodeBase64(final String msg) {
		int c = isDoIterating() ? iteratingTimes : 1;
		String dec = msg;
		for (int i = 0; i < c; i++) {
			dec = StringUtils.newStringUtf8(Base64.decodeBase64(dec));
		}
		return dec;
	}

	/**
	 * 需要子类实现的文本加密方法，加密后再被Base64编码。
	 * 
	 * @param value
	 *            原文
	 * @return 密文
	 */
	protected String encrypt(final String value) {
		throw new UnsupportedOperationException(
				"Implements your encryption by your subclass");
	}

	/**
	 * 需要子类实现的文本解密方法，密文要以Base64编码。
	 * 
	 * @param value
	 *            密文
	 * @return 原文
	 */
	protected String decrypt(final String value) {
		throw new UnsupportedOperationException(
				"Implements your decryption by your subclass");
	}

	protected String convertPropertyValue(final String originalValue) {
		if (!isEncryptedValue(originalValue)) {
			return originalValue;
		}
		return decodeValue(getInnerEncryptedValue(originalValue));
	}

	protected String resolveSystemProperty(String key) {
		return convertPropertyValue(super.resolveSystemProperty(key));
	}

	/**
	 * @return 是否附加加密处理
	 */
	public boolean isDoEncrypt() {
		return doEncrypt;
	}

	/**
	 * 设置是否附加简单加密处理
	 * 
	 * @param doEncrypt
	 */
	public void setDoEncrypt(boolean doEncrypt) {
		this.doEncrypt = doEncrypt;
	}

	/**
	 * @return 是否迭代编码处理
	 */
	public boolean isDoIterating() {
		return doIterating;
	}

	/**
	 * 设置是否迭代编码处理
	 * 
	 * @param doIterating
	 */
	public void setDoIterating(boolean doIterating) {
		this.doIterating = doIterating;
	}

	/**
	 * 设置迭代次数，需要大于1才有意义。
	 * 
	 * @param iteratingTimes
	 */
	public void setIteratingTimes(int iteratingTimes) {
		if (iteratingTimes > 0)
			this.iteratingTimes = iteratingTimes;
	}

	public static void main(String[] args) {
		EncodablePropertyPlaceholderConfigurer i = new EncodablePropertyPlaceholderConfigurer();
		i.setDoEncrypt(false);
		i.setDoIterating(false);
		// 这个测试用例可以帮你将需要处理的内容进行编码，不过不要把改后的“测试”内容提交了喔。
		String rawString = "AnythingYouWannaEncode";
		System.out.println(rawString);

		String encString = i.encodeValue(rawString);
		System.out.println(encString);
		System.out.println(i.decodeValue(encString));

		String cfgString = i.convertPropertyToEncoded(rawString);
		System.out.println(cfgString);
		System.out.println(i.convertPropertyValue(cfgString));
	}

}
