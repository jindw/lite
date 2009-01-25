package org.xidea.el.json;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * 改造自stringtree，将类改成线程安全的方式。 并提供简单的静态编码方法{@see JSONEncoder#encode(Object)}
 * 
 * @author stringtree.org
 * @author jindw
 */
public class JSONEncoder {
	private static JSONEncoder encoder = new JSONEncoder(true);
	boolean emitClassName = true;

	public JSONEncoder(boolean emitClassName) {
		this.emitClassName = emitClassName;
	}

	public JSONEncoder() {
	}

	public static String encode(Object value) {
		StringWriter buf = new StringWriter();
		try {
			encoder.encode(value, buf,new HashSet<Object>());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return buf.toString();
	}

	public void encode(Object value, Writer out, Collection<Object> cached)
			throws IOException {
		print(value, out, cached);
		if (cached != null) {
			cached.clear();
		}
	}

	protected void print(Object object, Writer out, Collection<Object> cached)
			throws IOException {
		if (object == null) {
			out.write("null");
		} else if (object instanceof Boolean) {
			out.write(String.valueOf(object));
		} else if (object instanceof Number) {
			out.write(String.valueOf(object));
		} else if (object instanceof String) {
			print((String) object, out);
		} else if (object instanceof Character) {
			print(String.valueOf(object), out);
		} else {
			if (cached != null) {
				if (cached.contains(object)) {
					print(object.toString(), out);
					return;
				}else{
					cached.add(object);
				}
			}
			if (object instanceof Map) {
				print((Map<?, ?>) object, out, cached);
			} else if (object instanceof Object[]) {
				print((Object[]) object, out, cached);
			} else if (object instanceof Iterator) {
				print((Iterator<?>) object, out, cached);
			} else if (object instanceof Collection) {
				print(((Collection<?>) object).iterator(), out, cached);
			} else {
				printBean(object, out, cached);
			}
			if (cached != null) {
				cached.remove(object);
			}
		}
	}

	protected void print(String text, Writer out) throws IOException {
		out.write('"');
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
			case '"':
				// case '\'':
				// case '/':
			case '\\':
				out.write('\\');
				out.write(c);
				break;
			case '\b':// \u0008
				out.write("\\b");
				break;
			case '\n'://

				// case '\v'://\u000b
				out.write("\\n");
				break;
			// case '\f'://\u000c
			// out.write("\\f");
			// break;
			case '\r'://

				out.write("\\r");
				break;
			case '\t':// \u0009
				out.write("\\t");
				break;
			default:
				if (Character.isISOControl(c)) {
					// if ((c >= 0x0000 && c <= 0x001F)|| (c >= 0x007F && c <=
					// 0x009F)) {
					out.write("\\u");
					out.write(Integer.toHexString(0x10000 + c), 1, 5);
				} else {
					out.write(c);
				}
			}
		}
		out.write('"');
	}

	protected void printBean(Object object, Writer out,Collection<Object> cached) throws IOException {
		out.write('{');
		BeanInfo info;
		boolean addedSomething = false;
		try {
			info = Introspector.getBeanInfo(object.getClass());
			PropertyDescriptor[] props = info.getPropertyDescriptors();
			for (int i = 0; i < props.length; ++i) {
				PropertyDescriptor prop = props[i];
				String name = prop.getName();
				Method accessor = prop.getReadMethod();
				if ((emitClassName == true || !"class".equals(name))
						&& accessor != null) {
					if (!accessor.isAccessible()) {
						accessor.setAccessible(true);
					}
					Object value = accessor.invoke(object);
					if (addedSomething) {
						out.write(',');
					}
					print(name, out);
					out.write(':');
					print(value, out, cached);
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
		out.write('}');
	}

	protected void print(Map<?, ?> map, Writer out,Collection<Object> cached)
			throws IOException {
		out.write('{');
		Iterator<?> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
			print(String.valueOf(e.getKey()), out);
			out.write(':');
			print(e.getValue(), out, cached);
			if (it.hasNext()) {
				out.write(',');
			}
		}
		out.write('}');
	}

	protected void print(Object[] object, Writer out,Collection<Object> cached)
			throws IOException {
		out.write('[');
		for (int i = 0; i < object.length; ++i) {
			if (i > 0) {
				out.write(',');
			}
			print(object[i], out, cached);
		}
		out.write(']');
	}

	protected void print(Iterator<?> it, Writer out,Collection<Object> cached)
			throws IOException {
		out.write('[');
		while (it.hasNext()) {
			print(it.next(), out, cached);
			if (it.hasNext()) {
				out.write(',');
			}
		}
		out.write(']');
	}
}
