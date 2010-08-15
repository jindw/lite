package org.xidea.el.json;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
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
	private static JSONEncoder encoder = new JSONEncoder();
	private final boolean ignoreClassName;
	private final boolean addressEqual;
	private final boolean throwError;
	private final String dateFormat;
	private final Object[] parent;
	private int index = 0;

	public JSONEncoder(String dateFormat, boolean ignoreClassName, int depth,
			boolean addressEqual, boolean throwError) {
		this.dateFormat = dateFormat;
		this.ignoreClassName = ignoreClassName;
		this.parent = depth > 0 ? new Object[depth] : null;
		this.addressEqual = addressEqual;
		this.throwError = throwError;
	}

	private JSONEncoder() {
		this(W3C_DATE_TIME_MILLISECOND_FORMAT, true, 64, true, true);
	}

	public static String encode(Object value) {
		StringBuilder buf = new StringBuilder();
		try {
			encoder.encode(value, buf);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return buf.toString();
	}

	public void encode(Object value, Appendable out) throws IOException {
		if (this.parent == null) {
			print(value, out);
		} else {
			synchronized (parent) {
				index = 0;
				print(value, out);
			}
		}

	}

	protected void print(Object object, Appendable out) throws IOException {
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
				int i = index;
				if (i < parent.length) {
					if (addressEqual) {
						while (i-- > 0) {
							if (parent[i] == object) {
								break;
							}
						}
					} else {
						while (i-- > 0) {
							if (object.equals(parent[i])) {
								break;
							}
						}
					}
				}
				if (i < 0) {
					parent[index++] = object;
				} else {
					String error = i < parent.length ? "JSON 数据源中发现递归行为:" + out
							+ "，递归数据将当null处理" : "深度超出许可范围：" + out;
					log.error(error);
					if (throwError) {
						throw new IllegalStateException(error);
					}
					out.append("null");
					return;
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
					printMap(object, out);
				}
			} finally {
				if (parent != null) {
					parent[--index] = null;
				}
			}
		}
	}

	protected void printString(String text, Appendable out) throws IOException {
		out.append('"');
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
			case '"':
			// case '\'':
			// case '/':
			//case '\\':
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

	protected void printMap(Object object, Appendable out) throws IOException {
		out.append('{');
		try {
			Map<String, Method> props = ReflectUtil.getGetterMap(object.getClass());
			boolean first = true;
			for (String name : props.keySet()) {
				try {
					Method accessor = props.get(name);
					if (accessor == null
							|| (ignoreClassName && "class".equals(name))) {
						continue;
					}
					Object value = accessor.invoke(object);
					if (first) {
						first = false;
					}else{
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

	protected void printMap(Map<?, ?> map, Appendable out) throws IOException {
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

	protected void printList(Object array, Appendable out)
			throws IOException {
		out.append('[');
		int len = Array.getLength(array);
		for (int i = 0; i < len; ++i) {
			if (i > 0) {
				out.append(',');
			}
			print( Array.get(array, i), out);
		}
		out.append(']');
	}

	protected void printList(Iterator<?> it, Appendable out) throws IOException {
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
