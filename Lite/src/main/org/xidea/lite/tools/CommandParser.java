package org.xidea.lite.tools;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.impl.ExpressionImpl;
import org.xidea.el.Reference;
import org.xidea.el.ReferenceExpression;

public class CommandParser {
	private static final Log log = LogFactory.getLog(CommandParser.class);

	public static final Map<Class<?>, Convertor<? extends Object>> CONVERTOR_MAP;
	public Map<Class<?>, Convertor<? extends Object>> convertorMap = CONVERTOR_MAP;

	private Map<String, List<String>> params;

	public CommandParser(String[] args){
		this.params = parseArgs(args);
		;
	}

	public void setup(Object config) {
		setup(config, params);
	}


	protected void setup(final Object context, Map<String, ? extends Object> params) {
		for (String name : params.keySet()) {
			if (name!=null && name.length() > 0) {
				ReferenceExpression el = new ExpressionImpl(name);
				Reference result = el.prepare(context);
				if (result != null && result.getType()!=null) {
					Class<? extends Object> type = result.getType();
					Object object = params.get(name);
					String[] values = null;

					// values.toArray(new String[values.size()])
					if (object instanceof Collection) {
						@SuppressWarnings("unchecked")
						Collection<String> col = (Collection) object;
						values = (String[]) col.toArray(new String[col.size()]);
					} else {
						values = (String[]) object;
					}
					result.setValue(getValue(values, type, context, name));
				} else {
					log.warn("找不到相关属性：" + name);
					if (log.isInfoEnabled()) {
						if (context == null) {
							return;
						}
						ArrayList<String> properties = new ArrayList<String>();
						try {
							PropertyDescriptor[] ps = Introspector.getBeanInfo(
									context.getClass())
									.getPropertyDescriptors();

							for (PropertyDescriptor p : ps) {
								properties.add(p.getName());
							}
						} catch (IntrospectionException e) {
							log.error(e);
						}
						log.info("当前对象可能属性有：" + properties);
					}
				}
			}
		}
	}

	protected static Map<String, List<String>> parseArgs(String[] args) {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		String name = null;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("-")) {
				name = arg.substring(1);
			} else {
				List<String> values = result.get(name);
				if (values == null) {
					values = new ArrayList<String>();
					result.put(name, values);
				}
				values.add(arg);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T> T getValue(String[] values, Class<? extends T> expectedType,
			Object context, String key) {
		if (expectedType.isArray()) {
			Class<? extends Object> ct = expectedType.getComponentType();
			T result = (T) Array.newInstance(ct, values.length);
			for (int i = 0; i < values.length; i++) {
				Array.set(result, i, getValue(values[i], ct, context, key));
			}
			return result;
		} else if (Collection.class.isAssignableFrom(expectedType)) {
			Collection buf;
			if (expectedType.isAssignableFrom(ArrayList.class)) {
				buf = new ArrayList();
			} else if (expectedType.isAssignableFrom(HashSet.class)) {
				buf = new HashSet();
			} else {
				try {
					buf = (Collection) expectedType.newInstance();
				} catch (Exception e) {
					log.error(e);
					throw new RuntimeException(e);
				}
			}
			for (int i = 0; i < values.length; i++) {
				buf.add(values[i]);
			}
			return (T) buf;
		} else {
			return getValue(values[0], expectedType, context, key);
		}
	}

	public <T> T getValue(String value, Class<? extends T> expectedType,
			Object context, String key) {
		@SuppressWarnings("unchecked")
		Convertor<T> c = (Convertor<T>) convertorMap.get(expectedType);
		if (c != null) {
			return c.getValue(value, expectedType, context, key);
		} else {
			log.error("unsuport type:" + expectedType + ":" + key);
		}
		return null;
	}
	public static interface Convertor<T> {
		public T getValue(String values, Class<? extends T> expectedType,
				Object context, String key);
	}

	static {
		Map<Class<?>, Convertor<?>> convertorMap = new HashMap<Class<?>, Convertor<?>>();

		convertorMap.put(File.class, new Convertor<File>() {
			public File getValue(String value,
					Class<? extends File> expectedType, Object context,
					String key) {
				return new File(value);
			}
		});

		convertorMap.put(String.class, new Convertor<String>() {
			public String getValue(String value,
					Class<? extends String> expectedType, Object context,
					String key) {
				return value;
			}
		});
		Convertor<? extends Object> c = new Convertor<Long>() {
			public Long getValue(String value,
					Class<? extends Long> expectedType, Object context,
					String key) {
				try {
					return Long.parseLong(value);
				} catch (Exception ex) {
					return 0l;
				}
			}
		};
		convertorMap.put(Long.TYPE, c);
		convertorMap.put(Long.class, c);
		c = new Convertor<Integer>() {
			public Integer getValue(String value,
					Class<? extends Integer> expectedType, Object context,
					String key) {
				try {
					return Integer.parseInt(value);
				} catch (Exception ex) {
					return 0;
				}
			}
		};
		convertorMap.put(Integer.TYPE, c);
		convertorMap.put(Integer.class, c);
		c = new Convertor<Double>() {
			public Double getValue(String value,
					Class<? extends Double> expectedType, Object context,
					String key) {
				try {
					return Double.parseDouble(value);
				} catch (Exception ex) {
					return 0d;
				}
			}
		};
		convertorMap.put(Double.TYPE, c);
		convertorMap.put(Double.class, c);
		convertorMap.put(Float.class, new Convertor<Float>() {
			public Float getValue(String value,
					Class<? extends Float> expectedType, Object context,
					String key) {
				try {
					return Float.parseFloat(value);
				} catch (Exception ex) {
					return 0f;
				}
			}
		});
		c = new Convertor<Boolean>() {
			public Boolean getValue(String value,
					Class<? extends Boolean> expectedType, Object context,
					String key) {
				try {
					return "true".equals(value);
				} catch (Exception ex) {
					return false;
				}
			}
		};

		convertorMap.put(Boolean.TYPE, c);
		convertorMap.put(Boolean.class, c);
		CONVERTOR_MAP = Collections.unmodifiableMap(convertorMap);
	}


}
