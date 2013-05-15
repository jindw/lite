package org.xidea.el.json;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONDecoder.TypeTransformer;

class JSONTransformer {
	private static Log log = LogFactory.getLog(JSONTransformer.class);

	private static final Pattern W3CDATE = Pattern.compile(
	// data
			"^(\\d{4})" + // YYYY1
					"(?:" + "\\-(\\d{1,2})" + // MM2
					"(?:" + "\\-(\\d{1,2})" + // DD3
					// time
					"(?:" + "T(\\d{2})\\:(\\d{2})" + // hour:4,minutes:5
					"(?:\\:(\\d{2}(?:\\.\\d+)?))?" + // seconds//6
					"(Z|[+\\-]\\d{2}\\:?\\d{2})?" + // timeZone:7
					")?" + ")?" +

					")?$");

	private static final Object SIMPLE_NOT_FOUND = new Object();

	private static final class TransformException extends RuntimeException {
		private static final long serialVersionUID = -7860716983754523610L;
		String title;
		Type type;
		Object value;

		public TransformException(String title, Type type, Object value) {
			this.title = title;
			this.type = type;
			this.value = value;
		}

	}

	private static Map<Class<?>, Class<?>> interfaceMap = new HashMap<Class<?>, Class<?>>();

	private Map<Type, TypeTransformer<? extends Object>> objectFactory = new HashMap<Type, TypeTransformer<? extends Object>>();
	static {
		interfaceMap.put(Map.class, HashMap.class);
		interfaceMap.put(List.class, ArrayList.class);
		interfaceMap.put(Collection.class, ArrayList.class);
		interfaceMap.put(Set.class, HashSet.class);
	}

	/**
	 * <pre>
	 * Year:YYYY (eg 1997)
	 * Year and month:YYYY-MM (eg 1997-07)
	 * Complete date:YYYY-MM-DD (eg 1997-07-16)
	 * Complete date plus hours and minutes:
	 *    YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
	 * Complete date plus hours, minutes and seconds:
	 *    YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
	 * Complete date plus hours, minutes, seconds and a decimal fraction of a second
	 *    YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
	 * </pre>
	 * 
	 * @param source
	 * @return
	 * @throws ParseException
	 */
	public Date parseW3Date(String source) {
		Matcher m = W3CDATE.matcher(source);
		if (m.find()) {
			Calendar ca = Calendar.getInstance();
			ca.clear();
			String timeZone = m.group(7);
			if (timeZone != null) {
				// System.out.println(timeZone+"/"+TimeZone.getTimeZone("GMT" +
				// timeZone));
				ca.setTimeZone(TimeZone.getTimeZone("GMT" + timeZone));
			}
			ca.set(Calendar.YEAR, Integer.parseInt(m.group(1)));// year
			String month = m.group(2);
			if (month != null) {
				ca.set(Calendar.MONTH, Integer.parseInt(month) - 1);
				String date = m.group(3);
				if (date != null) {
					ca.set(Calendar.DATE, Integer.parseInt(date));
					String hour = m.group(4);
					if (hour != null) {
						String minutes = m.group(5);
						ca.set(Calendar.HOUR, Integer.parseInt(hour));
						ca.set(Calendar.MINUTE, Integer.parseInt(minutes));
						String seconds = m.group(6);
						if (seconds == null) {
						} else if (seconds.length() > 2) {
							float f = Float.parseFloat(seconds);
							ca.set(Calendar.SECOND, (int) f);
							ca.set(Calendar.MILLISECOND,
									(int) (f * 1000) % 1000);
						} else {
							ca.set(Calendar.SECOND, Integer.parseInt(seconds));
						}
					}
				}
			}
			return ca.getTime();
		}
		return null;
	}

	public Date requireW3Date(String source) {
		Date date = parseW3Date(source);
		if (date != null) {
			return date;
		}
		throw new TransformException("日期解析失败", Date.class, source);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object createObject(Map map, Class defaultClass)
			throws InstantiationException, IllegalAccessException {
		String className = (String) map.get("class");
		if (className != null) {
			try {
				defaultClass = Class.forName(className);
			} catch (ClassNotFoundException e) {
			}
		}
		if (defaultClass.isInterface()) {
			// @SuppressWarnings("rawtypes")
			Class impl = interfaceMap.get(defaultClass);
			if (impl == null) {
				throw new TransformException("接口找不到默认实现类" + interfaceMap,
						defaultClass, map);
			}
			defaultClass = impl;
		}

		Object result = ReflectUtil.newInstance(defaultClass);
		if (result != null) {
			return result;
		}
		if (defaultClass != null && defaultClass.isMemberClass()
				&& 0 == (defaultClass.getModifiers() & Modifier.STATIC)) {
			throw new TransformException("请尽量避免用非静态的内部类存储数据", defaultClass, map);
		} else {
			throw new TransformException("JavaBean 创建异常", defaultClass, map);
		}
	}

	public TypeTransformer<? extends Object> addFactory(
			TypeTransformer<? extends Object> factory) {
		Type type = ReflectUtil.getParameterizedType(factory.getClass(),
				TypeTransformer.class, 0);
		return this.objectFactory.put(type, factory);
	}


	private Object require(Object value, Type type) throws Exception {
		if (type instanceof Class && ((Class<?>) type).isInstance(value)) {
			return value;
		}
		Class<?> clazz;
		if (type instanceof Class) {
			clazz = (Class<?>) type;
			Object result = toSimpleValue(value, clazz);
			if (result != SIMPLE_NOT_FOUND) {
				return result;
			}
		} else {
			clazz = ReflectUtil.baseClass(type);
		}

		if (value instanceof List) {// 折腾，就是为了少调用一次 Class.forName
			return createFromList(value, type, clazz);
		} else {
			Class.forName(clazz.getName());
			TypeTransformer<? extends Object> of = objectFactory.get(type);
			if (of != null) {
				return of.create(value);
			}
			if (value instanceof Map) {
				return createFromMap(value, type, clazz);
			} else {
				return createFallback(value, type, clazz);
			}
		}
	}

	public Object transform(Object value, Type type) {
		try {
			return require(value, type);
		} catch (TransformException e) {
			log.warn(e.title +":"+ e.type + "=>" + e.value, e);
		} catch (Exception e) {
			log.warn("未知JSON 类型异常:" + type + "=>" + value, e);
		}
		return null;
	}
	private Object createFromMap(Object value, Type type, final Class<?> clazz)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		Class.forName(clazz.getName());
		@SuppressWarnings("rawtypes")
		Map map = (Map) value;
		Object result = createObject(map, clazz);
		for (Object key : map.keySet()) {
			Type atype = ReflectUtil.getPropertyType(type, key);
			Object item = map.get(key);
			try {
				if (atype != null) {
					// throw exception on failed
					item = require(item, atype);
				}
				ReflectUtil.setValue(result, key, item);
			} catch (TransformException e) {
				log.warn(e.title +":"+ e.type + "=>" + e.value, e);
			}catch (Exception e) {
				log.warn("未知JSON 类型异常:"+type+"#"+key+ "=>" + item, e);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private Object createFromList(Object value, Type type, final Class<?> clazz)
			throws ClassNotFoundException {
		if (clazz.isArray()) {
			List<Object> list = (List<Object>) value;
			Object result = Array.newInstance(clazz.getComponentType(),
					list.size());
			for (int i = 0, len = list.size(); i < len; i++) {
				// null for exception
				Object item = transform(list.get(i), clazz.getComponentType());
				Array.set(result, i, item);
			}
			return result;
		} else if (List.class.isAssignableFrom(clazz)
				&& type instanceof ParameterizedType) {
			setupList((List<Object>) value, (ParameterizedType) type);
		} else {
			Class.forName(clazz.getName());
			TypeTransformer<? extends Object> of = objectFactory.get(type);
			if (of != null) {
				return of.create(value);
			}
			if (!clazz.isInstance(value)) {
				throw new TransformException("List类型转换失败", clazz, value);
			}
		}
		return value;
	}

	private void setupList(List<Object> list, ParameterizedType ptype) {
		Type listItemClazz = (ptype.getActualTypeArguments()[0]);
		int c = list.size();
		for (int j = 0; j < c; j++) {
			// null for exception
			Object item = transform(list.get(j),
					ReflectUtil.baseClass(listItemClazz));
			list.set(j, item);
		}
	}

	private Object createFallback(Object value, Type type, final Class<?> clazz)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		if (Enum.class.isAssignableFrom(clazz)) {
			return ReflectUtil.getEnum(value, clazz);
		}
		// 查漏补缺
		if (Character.class == clazz) {
			if (value instanceof CharSequence) {// 正常
				return ((CharSequence) value).charAt(0);
			} else if (value instanceof Number) {// 异常数据
				return (char) ((Number) value).intValue();
			}
			throw new TransformException(
					"Char类型转换失败(只支持CharSequence和Number 到Char的转换)", clazz,
					value);
		}
		// url,uri,file....
		Constructor<?> c = null;
		try {
			c = clazz.getConstructor(value.getClass());
		} catch (Exception e) {
			throw new TransformException("不支持数据类型", clazz, value);
		}
		return c.newInstance(value);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Object toSimpleValue(Object value, Class clazz)
			throws ParseException, ClassNotFoundException {
		boolean isPrimitive = clazz.isPrimitive();
		clazz = ReflectUtil.toWrapper(clazz);
		if (value == null) {
			if (isPrimitive) {
				return defaultPrimitive(clazz);
			} else {
				return null;
			}
		} else if (String.class == clazz) {
			return value.toString();
		}
		if (value instanceof String) {
			String text = (String) value;
			if (Date.class == clazz) {// 字符串日期
				return requireW3Date(text);
			} else if (java.sql.Date.class == clazz) {// 字符串日期
				return new java.sql.Date(requireW3Date(text).getTime());
			} else if (clazz == Class.class) {
				return Class.forName(text);
			} else if (Boolean.class == clazz) {// 容错
				return Boolean.valueOf(text);
			}
		} else {
			if (Number.class.isAssignableFrom(clazz)) {// 数字优先判断
				return ReflectUtil.toValue(
						value instanceof Number ? (Number) value : Double
								.parseDouble(String.valueOf(value)), clazz);
			}
			if (value instanceof Boolean) {
				if (Boolean.class == clazz) {
					return value;
				}
			} else if (value instanceof Number) {
				Number n = (Number) value;
				if (Date.class == clazz) {// 数字日期
					return new Date(n.longValue());
				} else if (java.sql.Date.class == clazz) {// 数字日期
					return new java.sql.Date(n.longValue());
				} else if (Boolean.class == clazz) {// 数字－>boolean 容错
					return n.floatValue() != 0;
				}

			}
		}

		return SIMPLE_NOT_FOUND;
	}

	private Object defaultPrimitive(Class<?> clazz) {
		if (clazz == Boolean.class) {
			return false;
		} else if (clazz == Character.class) {
			return '\0';
		} else if (Number.class.isAssignableFrom(clazz)) {
			return ReflectUtil.toValue(0, clazz);
		}
		// 不可能的分支
		throw new IllegalArgumentException("class is not primitive type:"
				+ clazz);
	}
}
