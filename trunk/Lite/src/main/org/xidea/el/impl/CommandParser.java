package org.xidea.el.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionSyntaxException;
import org.xidea.el.Reference;
import org.xidea.el.ReferenceExpression;

public class CommandParser {
	private static final Log log = LogFactory.getLog(CommandParser.class);
	private static final ExpressionFactory factory = ExpressionFactoryImpl
			.getInstance();
	public Map<Class<?>, Convertor<? extends Object>> convertorMap = Convertor.DEFAULT_MAP;
	private Map<String, String[]> params;
	public CommandParser(String[] args) {
		if (args != null) {
			this.params = parseArgs(args);
		}
	}

	public Map<String, String[]> getParams() {
		return params;
	}

	public void setParams(Map<String, String[]> params) {
		this.params = params;
	}

	public void addConvertor(Class<? extends Object> clazz,
			Convertor<? extends Object> convertor) {
		if (!(convertorMap instanceof HashMap<?, ?>)) {
			convertorMap = new HashMap<Class<?>, Convertor<? extends Object>>(
					convertorMap);
		}
		convertorMap.put(clazz, convertor);
	}

	public static void setup(Object result, String[] args) {
		CommandParser parser = new CommandParser(args);
		parser.setup(result);
	}

	public void setup(final Object context) {
		this.setup(context, params);
	}

	public void setup(final Object context, Map<String, String[]> params) {
		for (String name : params.keySet()) {
			if (name != null && name.length() > 0) {
				if (Character.isJavaIdentifierStart(name.charAt(0))) {
					try {
						ReferenceExpression el = getReference(name);
						Reference result = el.prepare(context);
						if (result != null && result.getType() != null) {
							Class<? extends Object> type = result.getType();
							String[] values = params.get(name);
							result.setValue(getValue(values, type, context,
									name));
						} else if (context != null) {
							onMissedProperty(context, name);
						}
					} catch (ExpressionSyntaxException e) {
						log.debug("无效属性："+name,e);
					}
				}
			}
		}
	}

	protected void onMissedProperty(final Object context, String name) {
		if (log.isInfoEnabled()) {
			String msg = "找不到相关属性：" + name+"; 当前对象可能属性有：" + ReflectUtil.map(context).keySet();
			log.info(msg);
		}
	}

	private ReferenceExpression getReference(String name) {
		ReferenceExpression el = (ReferenceExpression) factory.create(name);
		return el;
	}

	@SuppressWarnings({ "unchecked","rawtypes" })
	protected Map<String, String[]> parseArgs(String[] args) {
		Map result = new HashMap();
		String name = null;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("-")) {
				name = arg.substring(1);
			} else {
				List<String> values = (List<String>) result.get(name);
				if (values == null) {
					values = new ArrayList<String>();
					result.put(name, values);
				}
				values.add(arg);
			}
		}
		for (Object item : result.entrySet()) {
			Map.Entry entry = (Map.Entry) item;
			List<String> value = (List<String>) entry.getValue();
			entry.setValue(value.toArray(new String[value.size()]));
		}
		return (Map<String, String[]>) result;
	}

	@SuppressWarnings({ "unchecked","rawtypes" })
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
					log.error("创建对象失败"+expectedType.getName(),e);
					throw new RuntimeException(e);
				}
			}
			for (int i = 0; i < values.length; i++) {
				buf.add(values[i]);
			}
			return (T) buf;
		} else {
			return getValue(values[values.length - 1], expectedType, context,
					key);
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

	public String toString() {
		if (params == null) {
			return "*EMPTY*";
		} else {
			StringBuilder buf = new StringBuilder();
			for (String key : params.keySet()) {
				buf.append(key);
				buf.append("=>\n");
				String[] list = params.get(key);
				if (params != null) {
					for (String value : list) {
						buf.append("\t");
						buf.append(value);
						buf.append("\n");
					}
				}
			}
			return buf.toString();
		}

	}

}
