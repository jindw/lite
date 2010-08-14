package org.xidea.el.fn;

import java.util.Collection;

import org.xidea.el.Invocable;



/**
 * 一些模板内置内部函数的集合
 * 
 * @author jindw
 */
public class TextContains implements Invocable {
	public TextContains() {
	}

	public Object invoke(Object thizz,Object... args) throws Exception {
		return containsText(args[0], args[1]);
	}

	protected Boolean containsText(Object value, Object key) {
		key = String.valueOf(key);
		if (value instanceof Object[]) {
			for (Object item : (Object[]) value) {
				if (item != null && key.equals(String.valueOf(item))) {
					return Boolean.TRUE;
				}
			}
		} else if (value instanceof Collection<?>) {
			for (Object item : (Collection<?>) value) {
				if (item != null && key.equals(String.valueOf(item))) {
					return Boolean.TRUE;
				}
			}
		}else {
			if(String.valueOf(value).equals(key)){
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}
}
