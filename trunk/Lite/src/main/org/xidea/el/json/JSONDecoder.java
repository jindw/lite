package org.xidea.el.json;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.impl.ReflectUtil;

public class JSONDecoder {
	private final static String PATTERN= "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	private static Log log = LogFactory.getLog(JSONDecoder.class);
	private static JSONDecoder decoder = new JSONDecoder(false);
	private boolean strict = false;
	private final static String SAMPLE = "1900-01-01T00:00:00.000";//"+08:00"
	private final static int DATE_LENGTH = 10;
	private final static int DATE_TIME_LENGTH = SAMPLE.length();

	public JSONDecoder(boolean strict) {
		this.strict = strict;
	}

	public static <T> T decode(Reader value) throws IOException {
		StringBuilder buf = new StringBuilder();
		char[] cbuf = new char[32];
		int c;
		while ((c = value.read(cbuf)) >= 0) {
			buf.append(cbuf, 0, c);
		}
		return decode(buf.toString());
	}

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
	Pattern w3cdate = Pattern.compile(
			//data
			"\\d{4}" +//YYYY1
				"(?:" +
					"\\-(\\d{1,2})" +//MM2
					"(?:" +
						"\\-(\\d{1,2})" +//DD3
						//time
						"(?:" +
							"T(\\d{2})\\:(\\d{2})" +//hour:4,minutes:5
							"(?:\\:(\\d{2}(\\.\\d+)?))?"+//seconds//5
							"(Z|[+\\-](\\d{2})\\:?(\\d{2}))?" +//timeZone:6
						")?" +
					")?"+
			
				")?");
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
	protected Date parse(String source) throws ParseException{
	    // if sDate has time on it, it injects 'GTM' before de TZ displacement to
        // allow the SimpleDateFormat parser to parse it properly
		final int len = source.length();
		boolean noZone =false;
		if(len <= DATE_LENGTH){
			source = source+SAMPLE.substring(len);//+"+0000";
			noZone = true;
			//return new SimpleDateFormat(PATTERN.substring(0,DATE_TIME_LENGTH)).parse(source);
		}else{
			//标准化日期信息
			final int t = source.indexOf('T'); 
			if(t != DATE_LENGTH){
				
			}
            //标准化时间信息
            if(len != DATE_TIME_LENGTH+5){
				// 标准化TimeZone
				if ('Z' == source.charAt(len - 1)) {
					return parse(source.substring(0, len - 1) + "+0000");
				}
				if (source.charAt(len - 3) == ':') {
					source = new StringBuilder(source).delete(len - 3, 1).toString();
				}
            	final int len2 = source.length();
            	final int t = source.indexOf('T');
            	final int offset = DATE_LENGTH - t;
            	final int zp = len2 - 5;
            	//add timezone
            	final char c = source.charAt(zp);
            	if(c == '+' || c == '-'){
            		source = source.substring(0,zp) + SAMPLE.substring(zp + offset)+source.substring(zp);
            	}else{
            		noZone = true;
            		source = source+ SAMPLE.substring(len2 + offset);
            	}
                
            }
        }
//        ParsePosition p = new ParsePosition(0);
        return new SimpleDateFormat(noZone?PATTERN.substring(0,DATE_TIME_LENGTH):PATTERN).parse(source);
//        if(p.getIndex()!=source.length()){
//        	throws new Runtime
//        }
//        	
//        return result;
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
					return (T)parse((String)value);
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
