package org.xidea.el.fn;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.xidea.el.Invocable;

public class JSArray extends JSObject implements Invocable {
	public static final String MEMBERS =
	// 0-3
	"slice,splice,concat,join" +
	// 4-7
			",push,pop,shift,unshift" +
			// 8-9
			",reverse,sort";

	@SuppressWarnings("unchecked")
	public Object invoke(Object thiz, Object... args) throws Exception {
		List<Object> list = (List<Object>) toList(thiz);
		switch (type) {
		case 0:
		case 1:
			return slice(list, args);
		case 2:
			return concat(list, args);
		case 3:
			return join(list, args);
		case 4:
			return push(list, args);
		case 5:
			return pop(list, args);
		case 6:
			return shift(list, args);
		case 7:
			return unshift(list, args);
		case 8:
			return reverse(list, args);
		case 9:
			return sort(list, args);
		}
		throw new UnsupportedOperationException("不支持方法：" + type);
	}

	@SuppressWarnings("unchecked")
	private static Object toList(Object thiz) {
		if (thiz instanceof Object[]) {
			thiz = Arrays.asList(thiz);
		} else if (thiz.getClass().isArray()) {
			int length = Array.getLength(thiz);
			List buf = new ArrayList(length);
			for (int i = 0; i < length; i++) {
				buf.add(Array.get(thiz, 1));
			}
			thiz = buf;
		}
		return thiz;
	}

	public Object slice(List<Object> thiz, Object... args) throws Exception {
		int begin = getIntArg(args, 0, 0);
		int size = thiz.size();
		int end = getIntArg(args, 1, size);
		return thiz.subList(toValid(begin, size), toValid(end, size));
	}

	private int toValid(int begin, int size) {
		if(begin<0){
			begin = Math.max(begin+size, 0);
		}else{
			begin = Math.min(begin, size);
		}
		return begin;
	};

	public Object join(List<Object> thiz, Object... args) throws Exception {
		StringBuilder buf = new StringBuilder();
		String joiner = null;
		for (Object o : thiz) {
			if (joiner == null) {
				joiner = getStringArg(args, 0, ",");
			} else {
				buf.append(joiner);
			}
			buf.append(ToPrimitive(o, String.class));

		}
		return buf.toString();
	}

	public Object push(List<Object> thiz, Object... args) {
		for (Object o : args) {
			thiz.add(o);
		}
		return thiz.size();
	}

	public Object pop(List<Object> thiz, Object... args) {
		int size = thiz.size();
		if (size > 0) {
			return thiz.remove(size - 1);
		}
		return null;
	}

	public Object shift(List<Object> thiz, Object... args) {
		int size = thiz.size();
		if (size > 0) {
			return thiz.remove(0);
		}
		return null;
	}

	public Object unshift(List<Object> thiz, Object... args) {
		for (int i = 0; i < args.length; i++) {
			thiz.add(i, args[i]);
		}
		return thiz.size();
	}

	@SuppressWarnings("unchecked")
	public Object concat(List<Object> thiz, Object... args) {
		List<Object> result = new ArrayList<Object>(thiz);
		for (Object o : args) {
			o = toList(o);
			if (o instanceof Collection) {
				result.addAll((Collection) o);
			} else {
				result.add(o);
			}
		}
		return result;
	}

	public Object reverse(List<Object> thiz, Object... args) {
		Collections.reverse(thiz);
		return thiz;
	}

	@SuppressWarnings("unchecked")
	public Object sort(List<Object> thiz, Object... args) {
		Object o = getArg(args, 0, null);
		Comparator<Object> c = null;
		if (o instanceof Comparator) {
			c = (Comparator<Object>) o;
		}
		Collections.sort(thiz, c);
		return thiz;
	}

}
