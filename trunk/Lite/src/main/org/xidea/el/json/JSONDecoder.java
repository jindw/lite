package org.xidea.el.json;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.impl.ReflectUtil;

public class JSONDecoder {
	private static Log log = LogFactory.getLog(JSONDecoder.class);
	private static JSONDecoder decoder = new JSONDecoder(false);
	private static JSONTransformer transformer = new JSONTransformer();
	private boolean strict = false;

	public JSONDecoder(boolean strict) {
		this.strict = strict;
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
		try{
			Object result = new JSONTokenizer(value, strict).parse();
			if (type != null && type != Object.class) {
				result = transform(result, type);
			}
			return (T) result;
		}catch (RuntimeException e) {
			log.warn("json error:"+value,e);
			throw e;
		}
	}
	@SuppressWarnings("unchecked")
	public <T> List<T> decodeList(String value, Class<T> type) {
		try{
			List<Object> result = (List<Object>) new JSONTokenizer(value, strict).parse();
			if (type != null && type != Object.class) {
				int c = result.size();
				while(c-->0){
					result.set(c,transform(result.get(c), type));
				}
				
			}
			return (List<T>) result;
		}catch (RuntimeException e) {
			log.warn("json error:"+value,e);
			throw e;
		}
	}
	@SuppressWarnings("unchecked")
	public static <T> T transform(Object source,Type type){
		return (T)transformer.transform(source, type);
	}
}
