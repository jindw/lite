package org.xidea.el.json;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

public class JSONTransformer {
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

	private static Map<Class<?>, Class<?>> interfaceMap = new HashMap<Class<?>, Class<?>>();
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
	public Date parseW3Date(String source) throws ParseException {
		Matcher m = W3CDATE.matcher(source);
		if (m.find()) {
			Calendar ca = Calendar.getInstance();
			ca.clear();
			String timeZone = m.group(7);
			if (timeZone != null) {
				//System.out.println(timeZone+"/"+TimeZone.getTimeZone("GMT" + timeZone));
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
							ca.set(Calendar.MILLISECOND,(int) (f * 1000) % 1000);
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

	@SuppressWarnings("unchecked")
	public Object transform(Object value, Type type) {
		if(type instanceof Class && ((Class<?>)type).isInstance(value)){
			return value;
		}
		try {
			Class<?> clazz = ReflectUtil.baseClass(type);
			if (value instanceof String) {
				if (clazz == Class.class) {
					return Class.forName((String) value);
				} else if(Enum.class.isAssignableFrom(clazz)){
					return ReflectUtil.getEnum(value, clazz);
				}else if(Date.class.isAssignableFrom(clazz)){
					return createDate( (String)value, clazz) ;
				}else{
					Constructor<?> c = null;
					try{
						c = clazz.getConstructor(String.class);
					}catch(Exception e){
						
					}
					if(c != null){
						return c.newInstance(value);
					}
				}
			}
			if (value instanceof Map) {
				@SuppressWarnings("rawtypes")
				Map map = (Map) value;
				String className = (String) map.get("class");
				@SuppressWarnings("rawtypes")
				Class clazz2 = className != null ? Class.forName(className)
						: clazz;
				if (clazz2.isInterface()) {
					@SuppressWarnings("rawtypes")
					Class impl = interfaceMap.get(clazz2);
					if (impl == null) {
						log.warn("接口" + clazz2 + " 找不到默认实现！！");
						return null;
					}
					clazz2 = impl;
				}
				Object result = ReflectUtil.newInstance(clazz2);
				for (Object key : map.keySet()) {
					Type atype = ReflectUtil.getPropertyType(clazz2, key);

					Object values = map.get(key);
					if (atype != null) {
						Class<?> aclazz = ReflectUtil.baseClass(atype);
						if (List.class.isAssignableFrom(aclazz)
								&& atype instanceof ParameterizedType) {
							setupList((List<Object>) values,
									(ParameterizedType) atype);

						}
						values = transform(values, aclazz);
					}
					ReflectUtil.setValue(result, key, values);
				}
				return result;
			} else if (value instanceof List) {
				if (clazz.isArray()) {
					List<Object> list = (List<Object>) value;
					Object result = Array.newInstance(clazz.getComponentType(),
							list.size());
					for (int i = 0, len = list.size(); i < len; i++) {
						Array.set(
								result,
								i,
								transform(list.get(i),
										(Type) clazz.getComponentType()));
					}
					return result;
				} else if (List.class.isAssignableFrom(clazz)
						&& type instanceof ParameterizedType) {
					setupList((List<Object>) value, (ParameterizedType) type);
				}
				return value;
			} else {
				return toSimpleValue(value, clazz);
			}
		} catch (Exception e) {
			log.warn("JSON 类型异常:"+type+":"+value, e);
			return null;
		}
	}

	private void setupList(List<Object> list, ParameterizedType ptype) {
		Type listItemClazz = (ptype.getActualTypeArguments()[0]);
		int c = list.size();
		for (int j = 0; j < c; j++) {
			list.set(
					j,
					transform(list.get(j), ReflectUtil.baseClass(listItemClazz)));
		}
	}
	@SuppressWarnings({ "rawtypes", "unchecked" }) 
	private Object toSimpleValue(Object value, Class clazz)
			throws ParseException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		if (String.class == clazz) {
			return (value).toString();
		}
		if(Enum.class.isAssignableFrom(clazz)){
			return ReflectUtil.getEnum(value, clazz);
		}
		boolean isPrimitive = clazz.isPrimitive();
		clazz = ReflectUtil.toWrapper(clazz);
		if (Number.class.isAssignableFrom(clazz)) {
			if (isPrimitive && value == null) {
				value = 0;
			}
			if (value != null && !(value instanceof Number)) {
				value = Double.parseDouble(String.valueOf(value));
			}
			return ReflectUtil.toValue((Number) value, clazz);
		} else if (Boolean.class == clazz) {
			if(value == null){
				return isPrimitive?false:null;
			}
			if(value instanceof Boolean){
				return value;
			}else if (value instanceof Number){
				return ((Number)value).floatValue() == 0;
			}else if(value instanceof String){
				String text = (String)value;
				if(text.equalsIgnoreCase("true")){
					return true;
				}else if(text.equalsIgnoreCase("false")){
					return false;
				}
			}
		} else if (clazz == null || value == null
				|| Map.class.isAssignableFrom(clazz)
				|| Collection.class.isAssignableFrom(clazz)) {
			return value;
		} else if (Character.class == clazz) {
			if (value instanceof String) {// 正常
				value = ((String) value).charAt(0);
			} else if (value instanceof Number) {// 异常数据
				value = (char) ((Number) value).intValue();
			} else if (isPrimitive) {// 异常数据
				value = '\0';
			}
			return value;
		} else if (Date.class.isAssignableFrom(clazz)) {
			if (value instanceof String) {
				return createDate((String)value, clazz);
			} else {
				Class type = SecondOffsetDate.class.isAssignableFrom(clazz)?Integer.class:Long.class;
				return clazz.getConstructor(type).newInstance(
						((Number) value).longValue());
			}
		}
		log.warn("JSON 类型异常" + clazz+":"+JSONEncoder.encode(value));
		return null;
	}

	private Object createDate(String value, Class<?> clazz) throws ParseException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		Date date =  parseW3Date(value);
		if(clazz.isInstance(date)){
			return date;
		}
		return clazz.getConstructor(Long.class).newInstance(date.getTime());
	}

}
