package org.xidea.el.json;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.impl.ReflectUtil;

public class JSONDecoder {
	private static Log log = LogFactory.getLog(JSONDecoder.class);
	private static JSONDecoder decoder = new JSONDecoder(false);
	private boolean strict = false;
	private static final Pattern W3CDATE = Pattern.compile(
			//data
			"^(\\d{4})" +//YYYY1
				"(?:" +
					"\\-(\\d{1,2})" +//MM2
					"(?:" +
						"\\-(\\d{1,2})" +//DD3
						//time
						"(?:" +
							"T(\\d{2})\\:(\\d{2})" +//hour:4,minutes:5
							"(?:\\:(\\d{2}(\\.\\d+)?))?"+//seconds//6
							"(Z|[+\\-]\\d{2}\\:?\\d{2})?" +//timeZone:7
						")?" +
					")?"+
			
				")?$");

	public JSONDecoder(boolean strict) {
		this.strict = strict;
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
	 * @param source
	 * @return
	 * @throws ParseException 
	 */
	public Date parseW3Date(String source) throws ParseException{
		Matcher m = W3CDATE.matcher(source);
		if(m.find()){
			Calendar ca = Calendar.getInstance();
			ca.clear();
			String timeZone = m.group(7);
			if(timeZone!=null){
				ca.setTimeZone(TimeZone.getTimeZone("GMT"+timeZone));
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
						if(seconds == null){
						}else if (seconds.length() > 2) {
							float f = Float.parseFloat(seconds);
							ca.set(Calendar.SECOND, (int) f);
							ca.set(Calendar.MILLISECOND,
									((int) f * 1000) % 1000);
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
//	public static <T> T decode(Reader value) throws IOException {
//		StringBuilder buf = new StringBuilder();
//		char[] cbuf = new char[32];
//		int c;
//		while ((c = value.read(cbuf)) >= 0) {
//			buf.append(cbuf, 0, c);
//		}
//		T rtv = decode(buf.toString());
//		return rtv;
//	}

	@SuppressWarnings("unchecked")
	public static <T> T decode(String value) {
		return (T) decoder.decode(value, null);
	}

	@SuppressWarnings("unchecked")
	public <T> T decode(String value, Class<T> type) {
		Object result = new JSONTokenizer(value, strict).parse();
		if (type != null && type != Object.class) {
			result = toValue(result, type);
		}
		return (T) result;
	}

	
	@SuppressWarnings("unchecked")
	protected <T> T toValue(Object value, Class<T> type) {
		try {
			boolean isPrimitive = type.isPrimitive();
			type = (Class<T>) ReflectUtil.toWrapper(type);
			if (Number.class.isAssignableFrom(type)) {
				if(isPrimitive && value == null){
					value = 0;
				}
				return (T) ReflectUtil.toValue((Number) value, type);
			} else if (Boolean.class == type) {
				if(isPrimitive && value == null){
					value = false;
				}
				return (T) value;
			} else if (String.class == type 
					|| type == null || value == null 
					|| Map.class.isAssignableFrom(type)
					|| Collection.class.isAssignableFrom(type)) {
				return (T) value;
			} else if (Character.class == type) {
				if(value instanceof String){//正常
					value = ((String) value).charAt(0);
				}else if(value instanceof Number){//异常数据
					value = (char)((Number)value).intValue();
				}else if(isPrimitive && value == null){//异常数据
					value = '\0';
				}
				return (T)value;
			} else if (type.isArray()) {
				List<Object> list = (List<Object>) value;
				Object result = Array.newInstance(type.getComponentType(),
						list.size());
				for (int i = 0, len = list.size(); i < len; i++) {
					Array.set(result, i,
							toValue(list.get(i), type.getComponentType()));
				}
				return (T) result;
			} else if (Date.class.isAssignableFrom(type)) {
				if(value instanceof String) {
					return (T)parseW3Date((String)value);
				}else{
					return type.getConstructor(Long.class).newInstance(((Number)value).longValue());
				}
			} else if (value instanceof String) {
				if(type == Class.class){
					return (T) Class.forName((String)value);
				}else{
					return type.getConstructor(String.class).newInstance(value);
				}
			} else if (value instanceof Map) {
				Map map = (Map) value;
				String className = (String) map.get("class");
				Class clazz = className != null ? Class.forName(className)
						: type;
				Object result = clazz.newInstance();
				for (Object key : map.keySet()) {
					Class atype = ReflectUtil.getPropertyType(clazz, key);
					ReflectUtil.setValue(result, key,
							toValue(map.get(key), atype));
				}
				return (T) result;
			}
			log.warn("JSON 类型异常" + type);
			return null;
		} catch (Exception e) {
			log.warn("JSON 类型异常", e);
			return null;
		}
	}
}
