package org.xidea.el.fn;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.xidea.el.Invocable;

class JSArray extends JSObject implements Invocable {
	public Object invoke(Object thiz, Object... args) throws Exception {
		return method.invoke(this, toList(thiz), args);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private final static Object toList(Object thiz) {
		if (thiz instanceof Object[]) {
			thiz = Arrays.asList((Object[])thiz);
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

	static int toSliceRange(int pos, int size) {
		if (pos < 0) {
			pos = Math.max(pos + size, 0);
		} else {
			pos = Math.min(pos, size);
		}
		return pos;
	};

	public Object splice(List<Object> thiz, Object... args) throws Exception {
		return slice(thiz, args);
	}

	public Object slice(List<Object> thiz, Object... args) throws Exception {
		int size = thiz.size();
		int begin = toSliceRange(JSObject.getIntArg(args, 0, 0), size);
		int end = toSliceRange(JSObject.getIntArg(args, 1, size), size);
		if (begin < end) {
			return thiz.subList(begin, end);
		} else {
			return Collections.EMPTY_LIST;
		}
	}

	public Object join(List<Object> thiz, Object... args) {
		StringBuilder buf = new StringBuilder();
		String joiner = null;
		boolean isFirst = true;
		for (Object o : thiz) {
			if(isFirst){
				isFirst = false;
				if(args.length>0){
					joiner = JSObject.getStringArg(args, 0, ",");
				}else{
					joiner = ",";
				}
			}else{
				buf.append(joiner);
			}
			if(o !=null){
				buf.append(ECMA262Impl.ToString(o));
			}

		}
		return buf.toString();
	}

	public Object toString(List<Object> thiz, Object... args) {
		return join(thiz, ",");
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
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
		Object o = JSObject.getArg(args, 0, null);
		Comparator<Object> c = null;
		if (o instanceof Comparator) {
			c = (Comparator<Object>) o;
		}
		Collections.sort(thiz, c);
		return thiz;
	}

}
