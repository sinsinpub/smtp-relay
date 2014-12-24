package com.github.sinsinpub.smtp.relay.utils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended Object utilities.
 * 
 * @see org.apache.commons.lang.ObjectUtils
 * @author sin_sin
 */
public abstract class ObjectUtil extends ObjectUtils {

	private static final Logger LOG = LoggerFactory.getLogger(ObjectUtil.class);
	private static final int INITIAL_HASH = 7;
	private static final int MULTIPLIER = 31;
	private static final String EMPTY_STRING = "";
	private static final String PACKAGE_DELIMITER = ".";
	private static final String BIN_CLASS_DELIMITER = "$";
	private static final String NULL_STRING = "null";
	private static final String ARRAY_START = "{";
	private static final String ARRAY_END = "}";
	private static final String EMPTY_ARRAY = ARRAY_START + ARRAY_END;
	private static final String ARRAY_ELEMENT_SEPARATOR = ", ";

	/**
	 * The delimiter that separates the components of a nested reference.
	 */
	public final static char NESTED_DELIM = '.';

	/**
	 * Create a new instance of specified class bean by invoking its
	 * <code>new</code> method with empty arguments. <code>null</code> will
	 * be returned if fails.
	 * 
	 * @param clazz
	 * @return New instance
	 */
	public static Object newBean(Class<?> clazz) {
		try {
			return clazz.newInstance();
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
		return null;
	}

	/**
	 * Invade private property and get its value in class by normal reflection.
	 * Type of property must be an Object...
	 * 
	 * @param o
	 *            Object
	 * @param p
	 *            Property name
	 * @return Value object
	 */
	public static Object getPrivatePropertyValue(Object o, String p) {
		Object value = null;
		try {
			Class<?> clazz = o.getClass();
			Field field = null;
			try {
				field = clazz.getField(p);
			} catch (NoSuchFieldException e) {
				field = findPrivateField(clazz, p);
			}
			if (field != null) {
				field.setAccessible(true);
				value = field.get(o);
			}
		} catch (Exception e) {
			LOG.warn(e.toString(), e);
		}
		return value;
	}

	/**
	 * Find private field from class and its super class.
	 * 
	 * @param c
	 *            Class
	 * @param p
	 *            Field name
	 * @return Field
	 */
	public static Field findPrivateField(Class<?> c, String p) {
		Field field = null;
		try {
			field = c.getDeclaredField(p);
		} catch (NoSuchFieldException e) {
			if (c.getSuperclass() != null) {
				field = findPrivateField(c.getSuperclass(), p);
			}
		} catch (SecurityException e) {
			LOG.warn(e.toString(), e);
		}
		return field;
	}

	/**
	 * Invoke private method and get its returned by normal reflection.
	 * 
	 * @param o
	 *            Object
	 * @param m
	 *            Method name
	 * @param args
	 *            Arguments to call method
	 * @return Returned value
	 */
	public static Object invokePrivateMethod(Object o, String m, Object[] args) {
		Object returnValue = null;
		try {
			Class<?> clazz = o.getClass();
			Class<?>[] pTypes = getTypes(args);
			Method method = null;
			try {
				method = clazz.getMethod(m, pTypes);
			} catch (NoSuchMethodException e) {
				method = findPrivateMethod(clazz, m, pTypes);
			}
			if (method != null) {
				method.setAccessible(true);
				returnValue = method.invoke(o, args);
			}
		} catch (Exception e) {
			LOG.warn(e.toString(), e);
		}
		return returnValue;
	}

	/**
	 * Find private method from class and its super class.
	 * 
	 * @param c
	 *            Class
	 * @param m
	 *            Method name
	 * @param params
	 *            Types of parameters
	 * @return Method
	 */
	public static Method findPrivateMethod(Class<?> c, String m,
			Class<?>[] params) {
		Method method = null;
		try {
			method = c.getDeclaredMethod(m, params);
		} catch (NoSuchMethodException e) {
			if (c.getSuperclass() != null) {
				method = findPrivateMethod(c.getSuperclass(), m, params);
			}
		} catch (SecurityException e) {
			LOG.warn(e.toString(), e);
		}
		return method;
	}

	/**
	 * Determine all the type of objects in array.
	 * 
	 * @param objs
	 *            Object array
	 * @return Class array
	 */
	public static Class<?>[] getTypes(Object[] objs) {
		if (objs == null) {
			return null;
		}
		Class<?>[] types = new Class<?>[objs.length];
		for (int i = 0; i < objs.length; i++) {
			types[i] = objs[i].getClass();
		}
		return types;
	}

	/**
	 * Delete the prefix header <code>get</code> from the getter of property
	 * name.
	 * 
	 * @param getter
	 * @return String
	 */
	public static String delPrefixGetter(String getter) {
		return delPrefixHeader(getter, "get", false);
	}

	/**
	 * Delete the prefix header <code>get</code> from the getter of property
	 * name.
	 * 
	 * @param getter
	 * @param noS
	 * @return String
	 */
	public static String delPrefixGetter(String getter, boolean noS) {
		return delPrefixHeader(getter, "get", noS);
	}

	/**
	 * Delete the prefix header from property name.
	 * 
	 * @param accor
	 * @param header
	 * @param noS
	 * @return String
	 */
	public static String delPrefixHeader(String accor, String header,
			boolean noS) {
		if (accor.startsWith(header)) {
			if (noS)
				return accor.substring(header.length(), header.length() + 1)
						.toLowerCase()
						+ (accor.endsWith("s") ? accor.substring(
								header.length() + 1, accor.length() - 1)
								: accor.substring(header.length() + 1));
			else
				return accor.substring(header.length(), header.length() + 1)
						.toLowerCase()
						+ accor.substring(header.length() + 1);
		}
		return accor;
	}

	/**
	 * Add the prefix header <code>get</code> into the property name.
	 * 
	 * @param getter
	 * @return String
	 */
	public static String addPrefixGetter(String getter) {
		return addPrefixHeader(getter, "get", false);
	}

	/**
	 * Add the prefix header <code>get</code> into the property name.
	 * 
	 * @param getter
	 * @param noS
	 * @return String
	 */
	public static String addPrefixGetter(String getter, boolean noS) {
		return addPrefixHeader(getter, "get", noS);
	}

	/**
	 * Add the prefix header into property name.
	 * 
	 * @param accor
	 * @param header
	 * @param addS
	 * @return String
	 */
	public static String addPrefixHeader(String accor, String header,
			boolean addS) {
		if (!accor.startsWith(header)) {
			if (addS)
				return header.concat(
						accor.substring(0, 1).toUpperCase().concat(
								accor.substring(1))).concat(
						accor.endsWith("s") ? EMPTY_STRING : "s");
			else
				return header.concat(accor.substring(0, 1)
						.toUpperCase()
						.concat(accor.substring(1)));
		}
		return accor;
	}

	/**
	 * Get nested path name on the right of first delimiter.
	 * 
	 * @param pn
	 *            Property name
	 * @return String
	 */
	public static String getRightName(String pn) {
		return getSubstring(pn, 1, 1, NESTED_DELIM);
	}

	/**
	 * Get nest path name on the left of last class name.
	 * 
	 * @param pn
	 *            Property name
	 * @return String
	 */
	public static String getLeftName(String pn) {
		return getSubstring(pn, 0, -1, NESTED_DELIM);
	}

	/**
	 * Get property name on the right side of nested path name.
	 * 
	 * @param pn
	 *            Property name
	 * @return String
	 */
	public static String getPropertyName(String pn) {
		return getSubstring(pn, 0, 1, NESTED_DELIM);
	}

	/**
	 * Split string with substring method...
	 * 
	 * @param str
	 * @param startIndex
	 * @param tokenSign
	 * @param spChar
	 *            delimiter for nested reference.
	 * @return String
	 */
	public static String getSubstring(String str, int startIndex,
			int tokenSign, char spChar) {
		if (null == str)
			return EMPTY_STRING;
		int idx = 0;
		int lastDot = -1;
		int dot = str.indexOf(spChar);
		do {
			idx++;
			if (idx == startIndex)
				break;
			lastDot = dot;
			dot = str.indexOf(spChar, dot + 1);
		} while (dot > -1);
		if (tokenSign < 0) {
			if (dot > 0 && idx == startIndex)
				return str.substring(0, dot);
			if (dot == -1 && lastDot > 0)
				if (startIndex > 0)
					return str;
				else
					return str.substring(0, lastDot);
			return EMPTY_STRING;
		} else if (tokenSign > 0) {
			if (dot > -1 && dot < str.length() && idx == startIndex)
				return str.substring(dot + 1);
			if (dot == -1 && lastDot > -1 && lastDot < str.length())
				return str.substring(lastDot + 1);
			return EMPTY_STRING;
		} else {
			if (idx == startIndex - 1)
				return str.substring(lastDot + 1, str.length());
			if (idx != startIndex)
				return EMPTY_STRING;
			if (dot > 0 && lastDot == dot)
				return str.substring(0, dot);
			return str.substring(lastDot + 1, dot);
		}
	}

	/**
	 * Deep-clone one JavaBean by byte array IO stream copying.
	 * 
	 * @param o
	 *            srcObject
	 * @return newObject
	 * @throws IOException
	 */
	public static Object cloneBean(Object o) throws IOException {
		Object nb = null;
		ByteArrayOutputStream byteOut = null;
		ObjectOutputStream out = null;
		ByteArrayInputStream byteIn = null;
		ObjectInputStream in = null;
		try {
			// out
			byteOut = new ByteArrayOutputStream();
			out = new ObjectOutputStream(byteOut);
			out.writeObject(o);
			// in
			byteIn = new ByteArrayInputStream(byteOut.toByteArray());
			in = new ObjectInputStream(byteIn);
			nb = in.readObject();
		} catch (ClassNotFoundException e) {
			LOG.warn(e.toString(), e);
			throw new IllegalArgumentException(e.toString());
		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(byteOut);
			IOUtils.closeQuietly(byteIn);
		}
		return nb;
	}

	/**
	 * Serialize object to bytecode array.
	 * 
	 * @param object
	 *            to be serialized
	 * @return byte array
	 * @throws IOException
	 */
	public static byte[] serializeObject(Object object) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			return baos.toByteArray();
		} finally {
			IOUtils.closeQuietly(oos);
			IOUtils.closeQuietly(baos);
		}
	}

	/**
	 * Unserialize object from bytecode array.
	 * 
	 * @param bytes
	 *            to unserialize from
	 * @return new object instance
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object unserializeObject(byte[] bytes) throws IOException,
			ClassNotFoundException {
		Object object = null;
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(bais);
			object = ois.readObject();
			return object;
		} finally {
			IOUtils.closeQuietly(ois);
			IOUtils.closeQuietly(bais);
		}
	}

	/**
	 * Check if Object is null or empty or uninitialized.
	 * 
	 * @param o
	 *            Object to check
	 * @return boolean
	 */
	public static boolean isNullOrEmpty(Object o) {
		if (null == o)
			return true;
		if (o instanceof String && ((String) o).length() == 0)
			return true;
		if (o instanceof StringBuffer && ((StringBuffer) o).length() == 0)
			return true;
		if (o instanceof Collection<?> && ((Collection<?>) o).isEmpty())
			return true;
		if (o instanceof Map<?, ?> && ((Map<?, ?>) o).isEmpty())
			return true;
		if (o instanceof Void)
			return true;
		if (o instanceof Object[] && ((Object[]) o).length == 0)
			return true;
		return false;
	}

	/**
	 * Check if Object is not null and empty and uninitialized.
	 * 
	 * @param o
	 *            Object to check
	 * @return boolean
	 */
	public static boolean isNotNullAndEmpty(Object o) {
		return !isNullOrEmpty(o);
	}

	/**
	 * Indicates if all the properties included by Object are Null or Empty.<br>
	 * Some properties can be ignored by using parameter excludedProperties.
	 * 
	 * @param o
	 *            Object to check
	 * @param excludedProperties
	 * @return True when all null
	 */
	public static boolean isBeanAllNullOrEmpty(Object o,
			String[] excludedProperties) {
		return isBeanNullOrEmpty(o, excludedProperties, true, false);
	}

	/**
	 * Indicates if any property included by Object is Null or Empty.<br>
	 * Some properties can be ignored by using parameter excludedProperties.
	 * 
	 * @param o
	 *            Object to check
	 * @param excludedProperties
	 * @return True when any null found
	 */
	public static boolean isBeanAnyNullOrEmpty(Object o,
			String[] excludedProperties) {
		return isBeanNullOrEmpty(o, excludedProperties, false, true);
	}

	/*
	 * Bean properties checker.
	 */
	private static boolean isBeanNullOrEmpty(Object o,
			String[] excludedProperties, boolean zeroAsNull, boolean isAny) {
		if (isNullOrEmpty(o))
			return true;
		boolean result = true;
		if (isAny)
			result = false;
		BeanInfo bi = null;
		try {
			bi = Introspector.getBeanInfo(o.getClass());
		} catch (IntrospectionException e) {
			return result;
		}
		PropertyDescriptor pd[] = bi.getPropertyDescriptors();
		for (int i = 0; i < pd.length; i++) {
			String propertyName = pd[i].getName();
			String propertyType = pd[i].getPropertyType().getName();
			boolean found = true;
			if (propertyName.equals("class"))
				found = false;
			if (null != excludedProperties)
				for (int j = 0; j < excludedProperties.length; j++) {
					if (propertyName.equals(excludedProperties[j])) {
						found = false;
						break;
					}
				}
			if (propertyType.equals("int") || propertyType.equals("long")
					|| propertyType.equals("short")
					|| propertyType.equals("boolean")
					|| propertyType.equals("float")
					|| propertyType.equals("double")
					|| propertyType.equals("char")
					|| propertyType.equals("byte"))
				found = false;
			if (found) {
				Object value = getPrivatePropertyValue(o, propertyName);
				if (zeroAsNull && value instanceof Number
						&& value.toString().equals("0"))
					value = null;
				if (isAny) {
					result |= isNullOrEmpty(value);
					if (result)
						break;
				} else {
					result &= isNullOrEmpty(value);
					if (!result)
						break;
				}
			}
		}
		return result;
	}

	/**
	 * Return whether the given throwable is a checked exception: that is,
	 * neither a RuntimeException nor an Error.
	 * 
	 * @param ex
	 *            the throwable to check
	 * @return whether the throwable is a checked exception
	 * @see java.lang.Exception
	 * @see java.lang.RuntimeException
	 * @see java.lang.Error
	 */
	public static boolean isCheckedException(Throwable ex) {
		return !(ex instanceof RuntimeException || ex instanceof Error);
	}

	/**
	 * Check whether the given exception is compatible with the exceptions
	 * declared in a throws clause.
	 * 
	 * @param ex
	 *            the exception to checked
	 * @param declaredExceptions
	 *            the exceptions declared in the throws clause
	 * @return whether the given exception is compatible
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" })
	public static boolean isCompatibleWithThrowsClause(Throwable ex,
			Class[] declaredExceptions) {
		if (!isCheckedException(ex)) {
			return true;
		}
		if (declaredExceptions != null) {
			int i = 0;
			while (i < declaredExceptions.length) {
				if (declaredExceptions[i].isAssignableFrom(ex.getClass())) {
					return true;
				}
				i++;
			}
		}
		return false;
	}

	/**
	 * Determine whether the given object is an array: either an Object array or
	 * a primitive array.
	 * 
	 * @param obj
	 *            the object to check
	 */
	public static boolean isArray(Object obj) {
		return (obj != null && obj.getClass().isArray());
	}

	/**
	 * Determine whether the given array is empty: i.e. <code>null</code> or
	 * of zero length.
	 * 
	 * @param array
	 *            the array to check
	 */
	public static boolean isEmpty(Object[] array) {
		return (array == null || array.length == 0);
	}

	/**
	 * Return <code>true</code> if the supplied Collection is
	 * <code>null</code> or empty. Otherwise, return <code>false</code>.
	 * 
	 * @param collection
	 *            the Collection to check
	 * @return whether the given Collection is empty
	 */
	public static boolean isEmpty(Collection<?> collection) {
		return (collection == null || collection.isEmpty());
	}

	/**
	 * Return a default object only when object is judged as null or empty by
	 * {@link #isNullOrEmpty(Object)} method.
	 * 
	 * @param o
	 *            Object
	 * @param d
	 *            Default
	 * @return default object
	 */
	public static Object getDefault(Object o, Object d) {
		return isNullOrEmpty(o) ? d : o;
	}

	/**
	 * Append the given Object to the given array, returning a new array
	 * consisting of the input array contents plus the given Object.
	 * 
	 * @param array
	 *            the array to append to (can be <code>null</code>)
	 * @param obj
	 *            the Object to append
	 * @return the new array (of the same component type; never
	 *         <code>null</code>)
	 */
	public static Object[] addObjectToArray(Object[] array, Object obj) {
		Class<?> compType = Object.class;
		if (array != null) {
			compType = array.getClass().getComponentType();
		} else if (obj != null) {
			compType = obj.getClass();
		}
		int newArrLength = (array != null ? array.length + 1 : 1);
		Object[] newArr = (Object[]) Array.newInstance(compType, newArrLength);
		if (array != null) {
			System.arraycopy(array, 0, newArr, 0, array.length);
		}
		newArr[newArr.length - 1] = obj;
		return newArr;
	}

	/**
	 * Convert the given array (which may be a primitive array) to an object
	 * array (if necessary of primitive wrapper objects).
	 * <p>
	 * A <code>null</code> source value will be converted to an empty Object
	 * array.
	 * 
	 * @param source
	 *            the (potentially primitive) array
	 * @return the corresponding object array (never <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if the parameter is not an array
	 */
	public static Object[] toObjectArray(Object source) {
		if (source instanceof Object[]) {
			return (Object[]) source;
		}
		if (source == null) {
			return new Object[0];
		}
		if (!source.getClass().isArray()) {
			throw new IllegalArgumentException("Source is not an array: "
					+ source);
		}
		int length = Array.getLength(source);
		if (length == 0) {
			return new Object[0];
		}
		Class<? extends Object> wrapperType = Array.get(source, 0).getClass();
		Object[] newArray = (Object[]) Array.newInstance(wrapperType, length);
		for (int i = 0; i < length; i++) {
			newArray[i] = Array.get(source, i);
		}
		return newArray;
	}

	/**
	 * Determine if the given objects are equal, returning <code>true</code>
	 * if both are <code>null</code> or <code>false</code> if only one is
	 * <code>null</code>.
	 * <p>
	 * Compares arrays with <code>Arrays.equals</code>, performing an
	 * equality check based on the array elements rather than the array
	 * reference.
	 * 
	 * @param o1
	 *            first Object to compare
	 * @param o2
	 *            second Object to compare
	 * @return whether the given objects are equal
	 * @see java.util.Arrays#equals
	 */
	public static boolean nullSafeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1.equals(o2)) {
			return true;
		}
		if (o1.getClass().isArray() && o2.getClass().isArray()) {
			if (o1 instanceof Object[] && o2 instanceof Object[]) {
				return Arrays.equals((Object[]) o1, (Object[]) o2);
			}
			if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
				return Arrays.equals((boolean[]) o1, (boolean[]) o2);
			}
			if (o1 instanceof byte[] && o2 instanceof byte[]) {
				return Arrays.equals((byte[]) o1, (byte[]) o2);
			}
			if (o1 instanceof char[] && o2 instanceof char[]) {
				return Arrays.equals((char[]) o1, (char[]) o2);
			}
			if (o1 instanceof double[] && o2 instanceof double[]) {
				return Arrays.equals((double[]) o1, (double[]) o2);
			}
			if (o1 instanceof float[] && o2 instanceof float[]) {
				return Arrays.equals((float[]) o1, (float[]) o2);
			}
			if (o1 instanceof int[] && o2 instanceof int[]) {
				return Arrays.equals((int[]) o1, (int[]) o2);
			}
			if (o1 instanceof long[] && o2 instanceof long[]) {
				return Arrays.equals((long[]) o1, (long[]) o2);
			}
			if (o1 instanceof short[] && o2 instanceof short[]) {
				return Arrays.equals((short[]) o1, (short[]) o2);
			}
		}
		return false;
	}

	/**
	 * Return as hash code for the given object; typically the value of
	 * <code>{@link Object#hashCode()}</code>. If the object is an array,
	 * this method will delegate to any of the <code>nullSafeHashCode</code>
	 * methods for arrays in this class. If the object is <code>null</code>,
	 * this method returns 0.
	 * 
	 * @see #nullSafeHashCode(Object[])
	 * @see #nullSafeHashCode(boolean[])
	 * @see #nullSafeHashCode(byte[])
	 * @see #nullSafeHashCode(char[])
	 * @see #nullSafeHashCode(double[])
	 * @see #nullSafeHashCode(float[])
	 * @see #nullSafeHashCode(int[])
	 * @see #nullSafeHashCode(long[])
	 * @see #nullSafeHashCode(short[])
	 */
	public static int nullSafeHashCode(Object obj) {
		if (obj == null) {
			return 0;
		}
		if (obj.getClass().isArray()) {
			if (obj instanceof Object[]) {
				return nullSafeHashCode((Object[]) obj);
			}
			if (obj instanceof boolean[]) {
				return nullSafeHashCode((boolean[]) obj);
			}
			if (obj instanceof byte[]) {
				return nullSafeHashCode((byte[]) obj);
			}
			if (obj instanceof char[]) {
				return nullSafeHashCode((char[]) obj);
			}
			if (obj instanceof double[]) {
				return nullSafeHashCode((double[]) obj);
			}
			if (obj instanceof float[]) {
				return nullSafeHashCode((float[]) obj);
			}
			if (obj instanceof int[]) {
				return nullSafeHashCode((int[]) obj);
			}
			if (obj instanceof long[]) {
				return nullSafeHashCode((long[]) obj);
			}
			if (obj instanceof short[]) {
				return nullSafeHashCode((short[]) obj);
			}
		}
		return obj.hashCode();
	}

	/**
	 * Return a hash code based on the contents of the specified array. If
	 * <code>array</code> is <code>null</code>, this method returns 0.
	 */
	public static int nullSafeHashCode(Object[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		int arraySize = array.length;
		for (int i = 0; i < arraySize; i++) {
			hash = MULTIPLIER * hash + nullSafeHashCode(array[i]);
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array. If
	 * <code>array</code> is <code>null</code>, this method returns 0.
	 */
	public static int nullSafeHashCode(boolean[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		int arraySize = array.length;
		for (int i = 0; i < arraySize; i++) {
			hash = MULTIPLIER * hash + hashCode(array[i]);
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array. If
	 * <code>array</code> is <code>null</code>, this method returns 0.
	 */
	public static int nullSafeHashCode(byte[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		int arraySize = array.length;
		for (int i = 0; i < arraySize; i++) {
			hash = MULTIPLIER * hash + array[i];
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array. If
	 * <code>array</code> is <code>null</code>, this method returns 0.
	 */
	public static int nullSafeHashCode(char[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		int arraySize = array.length;
		for (int i = 0; i < arraySize; i++) {
			hash = MULTIPLIER * hash + array[i];
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array. If
	 * <code>array</code> is <code>null</code>, this method returns 0.
	 */
	public static int nullSafeHashCode(double[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		int arraySize = array.length;
		for (int i = 0; i < arraySize; i++) {
			hash = MULTIPLIER * hash + hashCode(array[i]);
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array. If
	 * <code>array</code> is <code>null</code>, this method returns 0.
	 */
	public static int nullSafeHashCode(float[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		int arraySize = array.length;
		for (int i = 0; i < arraySize; i++) {
			hash = MULTIPLIER * hash + hashCode(array[i]);
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array. If
	 * <code>array</code> is <code>null</code>, this method returns 0.
	 */
	public static int nullSafeHashCode(int[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		int arraySize = array.length;
		for (int i = 0; i < arraySize; i++) {
			hash = MULTIPLIER * hash + array[i];
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array. If
	 * <code>array</code> is <code>null</code>, this method returns 0.
	 */
	public static int nullSafeHashCode(long[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		int arraySize = array.length;
		for (int i = 0; i < arraySize; i++) {
			hash = MULTIPLIER * hash + hashCode(array[i]);
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array. If
	 * <code>array</code> is <code>null</code>, this method returns 0.
	 */
	public static int nullSafeHashCode(short[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		int arraySize = array.length;
		for (int i = 0; i < arraySize; i++) {
			hash = MULTIPLIER * hash + array[i];
		}
		return hash;
	}

	/**
	 * Return the same value as <code>{@link Boolean#hashCode()}</code>.
	 * 
	 * @see Boolean#hashCode()
	 */
	public static int hashCode(boolean bool) {
		return bool ? 1231 : 1237;
	}

	/**
	 * Return the same value as <code>{@link Double#hashCode()}</code>.
	 * 
	 * @see Double#hashCode()
	 */
	public static int hashCode(double dbl) {
		long bits = Double.doubleToLongBits(dbl);
		return hashCode(bits);
	}

	/**
	 * Return the same value as <code>{@link Float#hashCode()}</code>.
	 * 
	 * @see Float#hashCode()
	 */
	public static int hashCode(float flt) {
		return Float.floatToIntBits(flt);
	}

	/**
	 * Return the same value as <code>{@link Long#hashCode()}</code>.
	 * 
	 * @see Long#hashCode()
	 */
	public static int hashCode(long lng) {
		return (int) (lng ^ (lng >>> 32));
	}

	/**
	 * Return a String representation of an object's overall identity.
	 * 
	 * @param obj
	 *            the object (may be <code>null</code>)
	 * @return the object's identity as String representation, or an empty
	 *         String if the object was <code>null</code>
	 */
	public static String identityToString(Object obj) {
		if (obj == null) {
			return EMPTY_STRING;
		}
		return obj.getClass().getName() + "@" + getIdentityHexString(obj);
	}

	/**
	 * Return a hex String form of an object's identity hash code.
	 * 
	 * @param obj
	 *            the object
	 * @return the object's identity code in hex notation
	 */
	public static String getIdentityHexString(Object obj) {
		return Integer.toHexString(System.identityHashCode(obj));
	}

	/**
	 * Return a content-based String representation if <code>obj</code> is not
	 * <code>null</code>; otherwise returns an empty String.
	 * <p>
	 * Differs from {@link #nullSafeToString(Object)} in that it returns an
	 * empty String rather than "null" for a <code>null</code> value.
	 * 
	 * @param obj
	 *            the object to build a display String for
	 * @return a display String representation of <code>obj</code>
	 * @see #nullSafeToString(Object)
	 */
	public static String getDisplayString(Object obj) {
		if (obj == null) {
			return EMPTY_STRING;
		}
		return nullSafeToString(obj);
	}

	/**
	 * Determine the class name for the given object.
	 * <p>
	 * Returns <code>"null"</code> if <code>obj</code> is <code>null</code>.
	 * 
	 * @param obj
	 *            the object to introspect (may be <code>null</code>)
	 * @return the corresponding class name
	 */
	public static String nullSafeClassName(Object obj) {
		return (obj != null ? obj.getClass().getName() : NULL_STRING);
	}

	/**
	 * Determine the class name for the given object.
	 * <p>
	 * Returns <code>"null"</code> if <code>obj</code> is <code>null</code>.
	 * 
	 * @param obj
	 *            the object to introspect (may be <code>null</code>)
	 * @return the corresponding canonical class name
	 */
	public static String nullSafeClassCanonicalName(Object obj) {
		if (obj == null)
			return NULL_STRING;
		String simpleName = getSimpleName(obj.getClass());
		String packageName = ClassUtils.getPackageCanonicalName(obj.getClass());
		return packageName.length() > 0 ? packageName + PACKAGE_DELIMITER
				+ simpleName : simpleName;
	}

	/**
	 * Return a String representation of the specified Object.
	 * <p>
	 * Builds a String representation of the contents in case of an array.
	 * Returns <code>"null"</code> if <code>obj</code> is <code>null</code>.
	 * 
	 * @param obj
	 *            the object to build a String representation for
	 * @return a String representation of <code>obj</code>
	 */
	public static String nullSafeToString(Object obj) {
		if (obj == null) {
			return NULL_STRING;
		}
		if (obj instanceof String) {
			return (String) obj;
		}
		if (obj instanceof Object[]) {
			return nullSafeToString((Object[]) obj);
		}
		if (obj instanceof boolean[]) {
			return nullSafeToString((boolean[]) obj);
		}
		if (obj instanceof byte[]) {
			return nullSafeToString((byte[]) obj);
		}
		if (obj instanceof char[]) {
			return nullSafeToString((char[]) obj);
		}
		if (obj instanceof double[]) {
			return nullSafeToString((double[]) obj);
		}
		if (obj instanceof float[]) {
			return nullSafeToString((float[]) obj);
		}
		if (obj instanceof int[]) {
			return nullSafeToString((int[]) obj);
		}
		if (obj instanceof long[]) {
			return nullSafeToString((long[]) obj);
		}
		if (obj instanceof short[]) {
			return nullSafeToString((short[]) obj);
		}
		String str = obj.toString();
		return (str != null ? str : EMPTY_STRING);
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>
	 * The String representation consists of a list of the array's elements,
	 * enclosed in curly braces (<code>"{}"</code>). Adjacent elements are
	 * separated by the characters <code>", "</code> (a comma followed by a
	 * space). Returns <code>"null"</code> if <code>array</code> is
	 * <code>null</code>.
	 * 
	 * @param array
	 *            the array to build a String representation for
	 * @return a String representation of <code>array</code>
	 */
	public static String nullSafeToString(Object[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			} else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}
			sb.append(String.valueOf(array[i]));
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>
	 * The String representation consists of a list of the array's elements,
	 * enclosed in curly braces (<code>"{}"</code>). Adjacent elements are
	 * separated by the characters <code>", "</code> (a comma followed by a
	 * space). Returns <code>"null"</code> if <code>array</code> is
	 * <code>null</code>.
	 * 
	 * @param array
	 *            the array to build a String representation for
	 * @return a String representation of <code>array</code>
	 */
	public static String nullSafeToString(boolean[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			} else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}

			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>
	 * The String representation consists of a list of the array's elements,
	 * enclosed in curly braces (<code>"{}"</code>). Adjacent elements are
	 * separated by the characters <code>", "</code> (a comma followed by a
	 * space). Returns <code>"null"</code> if <code>array</code> is
	 * <code>null</code>.
	 * 
	 * @param array
	 *            the array to build a String representation for
	 * @return a String representation of <code>array</code>
	 */
	public static String nullSafeToString(byte[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			} else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}
			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>
	 * The String representation consists of a list of the array's elements,
	 * enclosed in curly braces (<code>"{}"</code>). Adjacent elements are
	 * separated by the characters <code>", "</code> (a comma followed by a
	 * space). Returns <code>"null"</code> if <code>array</code> is
	 * <code>null</code>.
	 * 
	 * @param array
	 *            the array to build a String representation for
	 * @return a String representation of <code>array</code>
	 */
	public static String nullSafeToString(char[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			} else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}
			sb.append("'").append(array[i]).append("'");
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>
	 * The String representation consists of a list of the array's elements,
	 * enclosed in curly braces (<code>"{}"</code>). Adjacent elements are
	 * separated by the characters <code>", "</code> (a comma followed by a
	 * space). Returns <code>"null"</code> if <code>array</code> is
	 * <code>null</code>.
	 * 
	 * @param array
	 *            the array to build a String representation for
	 * @return a String representation of <code>array</code>
	 */
	public static String nullSafeToString(double[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			} else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}

			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>
	 * The String representation consists of a list of the array's elements,
	 * enclosed in curly braces (<code>"{}"</code>). Adjacent elements are
	 * separated by the characters <code>", "</code> (a comma followed by a
	 * space). Returns <code>"null"</code> if <code>array</code> is
	 * <code>null</code>.
	 * 
	 * @param array
	 *            the array to build a String representation for
	 * @return a String representation of <code>array</code>
	 */
	public static String nullSafeToString(float[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			} else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}

			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>
	 * The String representation consists of a list of the array's elements,
	 * enclosed in curly braces (<code>"{}"</code>). Adjacent elements are
	 * separated by the characters <code>", "</code> (a comma followed by a
	 * space). Returns <code>"null"</code> if <code>array</code> is
	 * <code>null</code>.
	 * 
	 * @param array
	 *            the array to build a String representation for
	 * @return a String representation of <code>array</code>
	 */
	public static String nullSafeToString(int[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			} else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}
			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>
	 * The String representation consists of a list of the array's elements,
	 * enclosed in curly braces (<code>"{}"</code>). Adjacent elements are
	 * separated by the characters <code>", "</code> (a comma followed by a
	 * space). Returns <code>"null"</code> if <code>array</code> is
	 * <code>null</code>.
	 * 
	 * @param array
	 *            the array to build a String representation for
	 * @return a String representation of <code>array</code>
	 */
	public static String nullSafeToString(long[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			} else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}
			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>
	 * The String representation consists of a list of the array's elements,
	 * enclosed in curly braces (<code>"{}"</code>). Adjacent elements are
	 * separated by the characters <code>", "</code> (a comma followed by a
	 * space). Returns <code>"null"</code> if <code>array</code> is
	 * <code>null</code>.
	 * 
	 * @param array
	 *            the array to build a String representation for
	 * @return a String representation of <code>array</code>
	 */
	public static String nullSafeToString(short[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			} else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}
			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * Get the simple name of the underlying class, like the method in JDK5.
	 * 
	 * @param clazz
	 *            class to be determined
	 * @return empty string if class refers nothing or anonymous
	 */
	public static String getSimpleName(Class<?> clazz) {
		if (null == clazz)
			return EMPTY_STRING;
		if (clazz.isArray())
			return getSimpleName(clazz.getComponentType()) + "[]";
		// Enclosing binary name not supported
		return getSimpleName(clazz.getName());
	}

	/**
	 * Get the simple short name of the underlying class name.<br>
	 * 
	 * @param className
	 *            assume as the canonical class name to get the short name for,
	 *            coded array and primitive type not expected, will not be
	 *            checked at all
	 * @return empty string if className is null or refers anonymous class
	 * @see org.apache.commons.lang.ClassUtils#getShortClassName(String) similar
	 *      method
	 */
	public static String getSimpleName(String className) {
		if (null == className)
			return EMPTY_STRING;
		// Strip the package name
		String simpleName = className.substring(className.lastIndexOf(PACKAGE_DELIMITER) + 1);
		// Strip suffix part of proxy class name, such as CGLIB
		simpleName = StringUtils.substringBefore(simpleName, "$$");

		// According to JLS3 "Binary Compatibility" (13.1) the binary
		// name of non-package classes (not top level) is the binary
		// name of the immediately enclosing class followed by a '$' followed
		// by:
		// (for nested and inner classes): the simple name.
		// (for local classes): 1 or more digits followed by the simple name.
		// (for anonymous classes): 1 or more digits.

		// Keep inner class only, anonymous class will be treated as "".
		// Remove leading "\$[0-9]*" from the name, following the rule from JDK5
		// But starts with "$" used by JDK dynamic proxy, ignore it.
		if (simpleName.lastIndexOf(BIN_CLASS_DELIMITER) > 0) {
			simpleName = removeLeadingDigits(StringUtils.substringAfterLast(
					simpleName, BIN_CLASS_DELIMITER));
		}
		return simpleName;
	}

	/**
	 * Remove all leading digits from string.
	 * 
	 * @see used by {@link #getSimpleName(String)}
	 * @param str
	 *            the String to process, may be null
	 * @return null if input is null, or the String itself if first character is
	 *         not a digit, or empty string if all characters are digit
	 */
	private static String removeLeadingDigits(String str) {
		if (null == str)
			return null;
		int length = str.length();
		int index = 0;
		while (index < length && Character.isDigit(str.charAt(index)))
			index++;
		return index > 0 ? str.substring(index) : str;
	}

}
