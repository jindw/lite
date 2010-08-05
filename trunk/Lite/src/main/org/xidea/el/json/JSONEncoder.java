package org.xidea.el.json;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 改造自stringtree
 * 
 * @author stringtree.org
 * @author jindw
 */
public class JSONEncoder {
	private static Log log = LogFactory.getLog(JSONEncoder.class);
	private static JSONEncoder encoder = new JSONEncoder();
	private final boolean printClassName;
	private final Object[] parent;
	private int index = 0;
	private boolean valueCheck;

	public JSONEncoder(boolean printClassName) {
		this(printClassName, 64,false);

	}

	public JSONEncoder(boolean printClassName, int depth, boolean checkEquals) {
		this.printClassName = printClassName;
		this.parent = new Object[depth];
	}

	private JSONEncoder() {
		this(false);
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

	public void encode(Object value, StringBuilder out) throws IOException {
		if(this.parent == null){
			print(value, out);
		}else{
			synchronized (parent) {
				print(value, out);
			}
		}
		
	}

	public void encode(Object value, Writer out) throws IOException {
		StringBuilder buf = new StringBuilder();
		print(value, buf);
		out.write(buf.toString());
	}
	protected void print(Object object, StringBuilder out) throws IOException {
		if (object == null) {
			out.append("null");
		} else if (object instanceof Boolean) {
			out.append(String.valueOf(object));
		} else if (object instanceof Number) {
			out.append(String.valueOf(object));
		} else if (object instanceof Class<?>) {
			// Class 系列化容易导致死循环
			print(((Class<?>) object).getName(), out);
		} else if (object instanceof String) {
			print((String) object, out);
		} else if (object instanceof Character) {
			print(String.valueOf(object), out);
		} else {
			if (parent != null) {
				int i = index;
				while (i-- > 0) {
					if (parent[i] == object || valueCheck && object.equals(parent[i])) {
						log.error("JSON 数据源中发现递归行为，递归数据将当null处理");
						out.append("null");
						return;
					}
				}
				if (index >= parent.length) {
					log.error("深度超出许可范围："
							+ Arrays.asList(parent));
					out.append("null");
					return ;
				}
				parent[index++] = object;
				
			}
			if (object instanceof Map<?, ?>) {
				print((Map<?, ?>) object, out);
			} else if (object instanceof Object[]) {
				print((Object[]) object, out);
			} else if (object instanceof Iterator<?>) {
				print((Iterator<?>) object, out);
			} else if (object instanceof Collection<?>) {
				print(((Collection<?>) object).iterator(), out);
			} else {
				printBean(object, out);
			}
			if (parent != null) {
				parent[--index] = null;
			}
		}
	}

	protected void print(String text, StringBuilder out) throws IOException {
		out.append('"');
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
			case '"':
				// case '\'':
				// case '/':
			case '\\':
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

	protected void printBean(Object object, StringBuilder out) throws IOException {
		out.append('{');
		BeanInfo info;
		boolean addedSomething = false;
		try {
			info = Introspector.getBeanInfo(object.getClass());
			PropertyDescriptor[] props = info.getPropertyDescriptors();
			for (int i = 0; i < props.length; ++i) {
				PropertyDescriptor prop = props[i];
				String name = prop.getName();
				Method accessor = prop.getReadMethod();
				if (accessor != null
						&& (!"class".equals(name) || printClassName)) {
					if (!accessor.isAccessible()) {
						accessor.setAccessible(true);
					}
					Object value = accessor.invoke(object);
					if (addedSomething) {
						out.append(',');
					}
					print(name, out);
					out.append(':');
					print(value, out);
					addedSomething = true;
				}
			}
		} catch (IllegalAccessException iae) {
			iae.printStackTrace();
		} catch (InvocationTargetException ite) {
			ite.getCause().printStackTrace();
			ite.printStackTrace();
		} catch (IntrospectionException ie) {
			ie.printStackTrace();
		}
		out.append('}');
	}

	protected void print(Map<?, ?> map, StringBuilder out,
			Collection<Object> cached) throws IOException {
		out.append('{');
		Iterator<?> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
			print(String.valueOf(e.getKey()), out);
			out.append(':');
			print(e.getValue(), out);
			if (it.hasNext()) {
				out.append(',');
			}
		}
		out.append('}');
	}

	protected void print(Object[] object, StringBuilder out,
			Collection<Object> cached) throws IOException {
		out.append('[');
		for (int i = 0; i < object.length; ++i) {
			if (i > 0) {
				out.append(',');
			}
			print(object[i], out);
		}
		out.append(']');
	}

	protected void print(Iterator<?> it, StringBuilder out,
			Collection<Object> cached) throws IOException {
		out.append('[');
		while (it.hasNext()) {
			print(it.next(), out);
			if (it.hasNext()) {
				out.append(',');
			}
		}
		out.append(']');
	}
}
