package org.xidea.el.json;

import java.lang.reflect.Type;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
