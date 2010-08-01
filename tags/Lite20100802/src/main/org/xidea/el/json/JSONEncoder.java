package org.xidea.el.json;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
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
	private static JSONEncoder encoder = new JSONEncoder();
	private final boolean printClassName ;
	private final int depth;

	public JSONEncoder(boolean printClassName) {
		this.printClassName = printClassName;
		this.depth = 64;
	}
	public JSONEncoder(boolean printClassName,int depth) {
		this.printClassName = printClassName;
		this.depth = depth;
	}
	private JSONEncoder() {
		this(false,64);
	}

	public static String encode(Object value) {
		StringBuilder buf = new StringBuilder();
		try {
			encoder.encode(value, buf,new HashSet<Object>());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return buf.toString();
	}

	public void encode(Object value, Appendable out, Collection<Object> cached)
			throws IOException {
		print(value, out, cached);
		if (cached != null) {
			cached.clear();
		}
	}

	protected void print(Object object, Appendable out, Collection<Object> cached)
			throws IOException {
		if (object == null) {
			out.append("null");
		} else if (object instanceof Boolean) {
			out.append(String.valueOf(object));
		} else if (object instanceof Number) {
			out.append(String.valueOf(object));
		} else if (object instanceof Class<?>) {
			//Class 系列化容易导致死循环
			print(((Class<?>) object).getName(), out);
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
					if(cached.size()>depth){
						throw new RuntimeException("深度超出许可范围："+cached);
					}
					cached.add(object);
				}
			}
			if (object instanceof Map<?, ?>) {
				print((Map<?, ?>) object, out, cached);
			} else if (object instanceof Object[]) {
				print((Object[]) object, out, cached);
			} else if (object instanceof Iterator<?>) {
				print((Iterator<?>) object, out, cached);
			} else if (object instanceof Collection<?>) {
				print(((Collection<?>) object).iterator(), out, cached);
			} else {
				printBean(object, out, cached);
			}
			if (cached != null) {
				cached.remove(object);
			}
		}
	}

	protected void print(String text, Appendable out) throws IOException {
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
			// case '\f'://\u000c
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

	protected void printBean(Object object, Appendable out,Collection<Object> cached) throws IOException {
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
				if (accessor != null &&
						(!"class".equals(name) || printClassName)
						 ) {
					if (!accessor.isAccessible()) {
						accessor.setAccessible(true);
					}
					Object value = accessor.invoke(object);
					if (addedSomething) {
						out.append(',');
					}
					print(name, out);
					out.append(':');
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
		out.append('}');
	}

	protected void print(Map<?, ?> map, Appendable out,Collection<Object> cached)
			throws IOException {
		out.append('{');
		Iterator<?> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
			print(String.valueOf(e.getKey()), out);
			out.append(':');
			print(e.getValue(), out, cached);
			if (it.hasNext()) {
				out.append(',');
			}
		}
		out.append('}');
	}

	protected void print(Object[] object, Appendable out,Collection<Object> cached)
			throws IOException {
		out.append('[');
		for (int i = 0; i < object.length; ++i) {
			if (i > 0) {
				out.append(',');
			}
			print(object[i], out, cached);
		}
		out.append(']');
	}

	protected void print(Iterator<?> it, Appendable out,Collection<Object> cached)
			throws IOException {
		out.append('[');
		while (it.hasNext()) {
			print(it.next(), out, cached);
			if (it.hasNext()) {
				out.append(',');
			}
		}
		out.append(']');
	}
}
