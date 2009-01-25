package org.xidea.el.operation;

import java.util.Collection;
import java.util.Map;


/**
 * 一些模板内置内部函数的集合
 * 
 * @author jindw
 */
public class TemplateGlobal implements Invocable {
	public static final int ID_CONTAINS_STRING = 1;
	private static final Object[] IDMAP = { ID_CONTAINS_STRING,
			"__contains_string__" };

	public static void appendTo(Map<String, Invocable> globalInvocableMap) {
		for (int i = 0; i < IDMAP.length; i += 2) {
			globalInvocableMap.put((String) IDMAP[i + 1], new TemplateGlobal(
					(Integer) IDMAP[i]));
		}
	}

	private final int type;

	public TemplateGlobal(int type) {
		this.type = type;
	}

	public Object invoke(Object thizz,Object... args) throws Exception {
		switch (type) {
		case ID_CONTAINS_STRING:
			return containsString(args[0], args[1]);
		}
		throw new UnsupportedOperationException(toString());
	}

	public Boolean containsString(Object value, Object key) {
		key = String.valueOf(key);
		if (value instanceof Object[]) {
			for (Object item : (Object[]) value) {
				if (item != null && key.equals(String.valueOf(item))) {
					return Boolean.TRUE;
				}
			}
		} else if (value instanceof Collection) {
			for (Object item : (Collection<?>) value) {
				if (item != null && key.equals(String.valueOf(item))) {
					return Boolean.TRUE;
				}
			}
		}
		return Boolean.FALSE;
	}

	public String toString(){
		for (int i = 0; i < IDMAP.length; i += 2) {
			if((Integer) IDMAP[i] == type){
				return (String) IDMAP[i + 1];
			}
		}
		return null;
	}

}
