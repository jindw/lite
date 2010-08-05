package org.xidea.el.json;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.impl.ReflectUtil;

public class JSONDecoder {
	private static Log log = LogFactory.getLog(JSONDecoder.class);
	private static JSONDecoder decoder = new JSONDecoder(false);
	private boolean strict = false;

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

	@SuppressWarnings("unchecked")
	protected <T> T toValue(Object value, Class<T> type) {
		try {
			type = (Class<T>) ReflectUtil.toWrapper(type);
			if (String.class == type || Boolean.class == type
					||  type == null || value == null) {
				return (T) value;
			} else if (type.isArray()) {
				List<Object> list = (List<Object>) value;
				Object result = Array.newInstance(type.getComponentType(),
						list.size());
				for (int i = 0, len = list.size(); i < len; i++) {
					Array.set(result, i,
							toValue(list.get(i), type.getComponentType()));
				}
				return (T) result;
			}else if (Map.class.isAssignableFrom(type)
					|| Collection.class.isAssignableFrom(type)) {
				return (T) value;
			} else if (Number.class.isAssignableFrom(type)) {
				return (T) ReflectUtil.toValue((Number) value, type);
			} else if (Character.class == type) {
				return (T) (Character) ((String) value).charAt(0);
			}
			if (value instanceof String) {
				return type.getConstructor(String.class).newInstance(value);
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
