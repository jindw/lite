package org.xidea.el.json;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.impl.ReflectUtil;

/**
 * 改造自stringtree
 * 
 * @author stringtree.org
 * @author jindw
 */
public class JSONEncoder {
	public final static String W3C_DATE_FORMAT = "yyyy-MM-dd";
	public final static String W3C_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mmZ";
	public final static String W3C_DATE_TIME_SECOND_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	public final static String W3C_DATE_TIME_MILLISECOND_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	private static Log log = LogFactory.getLog(JSONEncoder.class);

	private static JSONEncoder encoder = new JSONEncoder(
			W3C_DATE_TIME_MILLISECOND_FORMAT, true, 64);

	private final boolean ignoreClassName;
	private final String dateFormat;
	private final Object[] parent;
	private int index = 0;

	public JSONEncoder(String dateFormat, boolean ignoreClassName, int depth) {
		this.dateFormat = dateFormat;
		this.ignoreClassName = ignoreClassName;
		this.parent = depth > 0 ? new Object[depth] : null;
	}

	public static String encode(Object value) {
		return encoder.encode(value, new StringBuilder()).toString();
	}

	public StringBuilder encode(Object value, StringBuilder out) {
		if (this.parent == null) {
			print(value, out);
		} else {
			synchronized (parent) {
				index = 0;
				print(value, out);
			}
		}
		return out;
	}

	protected void print(Object object, StringBuilder out){
		if (object == null) {
			out.append("null");
			return;
		}
		Class<?> type = object.getClass();
		if (type == Boolean.class) {
			out.append(object.toString());
		} else if (type == String.class) {
			printString((String) object, out);
		} else if (type == Character.class) {
			printString(object.toString(), out);
		} else if (Number.class.isAssignableFrom(type)) {
			out.append(object.toString());
		} else if (type == Class.class) {
			// Class 系列化容易导致死循环
			printString(((Class<?>) object).getName(), out);
		} else if (Date.class.isAssignableFrom(type) && dateFormat != null) {
			// see http://www.w3.org/TR/NOTE-datetime
			String date = new SimpleDateFormat(dateFormat)
					.format((Date) object);
			date = new StringBuilder(date).insert(date.length() - 2, ':')
					.toString();
			printString(date, out);
		} else {// PATTERN
			if (parent != null) {
				if (index > parent.length) {
					reportError("深度超出许可范围：" + out);
					return;
				}
				if (checkNest(object)) {
					out.append("null");
					reportError("JSON 数据源中发现递归行为,递归数据将当null处理:" + out);
					return;
				} else {
					parent[index++] = object;
				}
			}
			try {
				if (object instanceof Map<?, ?>) {
					printMap((Map<?, ?>) object, out);
				} else if (type.isArray()) {
					printList(object, out);
				} else if (object instanceof Iterator<?>) {
					printList((Iterator<?>) object, out);
				} else if (object instanceof Collection<?>) {
					printList(((Collection<?>) object).iterator(), out);
				} else {
					if(object instanceof Enum){
						printString(((Enum<?>)object).name(),out);
					}else if(object instanceof URL || object instanceof URI || object instanceof File){
						printString(object.toString(), out);
					}else{
						printMap(object, out);
					}
				}
			} finally {
				if (parent != null) {
					parent[--index] = null;
				}
			}
		}
	}

	protected void reportError(String error) {
		log.error(error);
		// throw new IllegalStateException(error);
	}

	protected boolean checkNest(Object object) {
		int i = index;
		if (object instanceof Collection<?> || object instanceof Map<?, ?>
				|| object instanceof Object[]) {
			while (i-- > 0) {
				if (parent[i] == object) {
					return true;
				}
			}
		} else {
			while (i-- > 0) {
				if (object.equals(parent[i])) {
					return true;
				}
			}
		}
		;

		return false;
	}

	protected void printString(String text, StringBuilder out) {
		out.append('"');
		final int len = text.length();
		for (int i = 0; i < len; i++) {
			char c = text.charAt(i);
			switch (c) {
			case '/':// escape HTML </script> </script >
				if (i > 0 && text.charAt(i - 1) == '<'
						&& text.regionMatches(true, i - 1, "</script", 0, 8)) {
					out.append('\\');
				}
				out.append(c);
				break;
			case '\\':
			case '"':
				// case '\'':
				// case '/':
				out.append('\\');
				out.append(c);
				break;
			case '\b':// \u0008
				out.append("\\b");
				break;
			case '\n'://

				// case '\v'://\u000b
				out.append("\\n");
				break;
			// case '\f'://
			// out.append("\\f");
			// break;
			case '\r'://

				out.append("\\r");
				break;
			case '\t':// \u0009
				out.append("\\t");
				break;
			default:
				if (Character.isISOControl(c)) {
					// if ((c >= 0x0000 && c <= 0x001F)|| (c >= 0x007F && c <=
					// 0x009F)) {
					out.append("\\u");
					out.append(Integer.toHexString(0x10000 + c), 1, 5);
				} else {
					out.append(c);
				}
			}
		}
		out.append('"');
	}

	protected void printMap(Object object, StringBuilder out) {
		out.append('{');
		try {
			Map<String, Object> props = getAccessorMap(object.getClass());
			boolean first = true;
			for (String name : props.keySet()) {
				try {
					Object accessor = props.get(name);
					if (accessor == null
							|| (ignoreClassName && "class".equals(name))) {
						continue;
					}
					
					Object value;
					if(accessor instanceof Method){
						value = ((Method)accessor).invoke(object);
					}else{
						value = ((Field)accessor).get(object);
					}
					if (first) {
						first = false;
					} else {
						out.append(',');
					}
					out.append('"');
					out.append(name);
					out.append('"');
					out.append(':');
					print(value, out);
				} catch (Exception e) {
					log.warn("属性获取失败", e);
				}
			}
		} catch (Exception e) {
			log.warn("JavaBean信息获取失败", e);
		}
		out.append('}');
	}

	protected Map<String, Object> getAccessorMap(Class<? extends Object> clazz) {
		Map<String,Object> getterMap = new HashMap<String, Object>( ReflectUtil.getGetterMap(clazz));
		getterMap.putAll(ReflectUtil.getFieldMap(clazz));
		return getterMap;
	}

	protected void printMap(Map<?, ?> map,StringBuilder out) {
		Iterator<?> it = map.entrySet().iterator();
		if (it.hasNext()) {
			out.append('{');
			while (true) {
				Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
				printString(String.valueOf(e.getKey()), out);
				out.append(':');
				print(e.getValue(), out);
				if (it.hasNext()) {
					out.append(',');
				} else {
					break;
				}
			}
			out.append('}');
		} else {
			out.append("{}");
		}
	}

	protected void printList(Object array,StringBuilder out) {
		out.append('[');
		int len = Array.getLength(array);
		for (int i = 0; i < len; ++i) {
			if (i > 0) {
				out.append(',');
			}
			print(Array.get(array, i), out);
		}
		out.append(']');
	}

	protected void printList(Iterator<?> it, StringBuilder out) {
		if (it.hasNext()) {
			out.append('[');
			while (true) {
				print(it.next(), out);
				if (it.hasNext()) {
					out.append(',');
				} else {
					break;
				}
			}
			out.append(']');
		} else {
			out.append("[]");
		}
	}
}
