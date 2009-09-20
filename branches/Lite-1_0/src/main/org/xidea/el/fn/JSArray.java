package org.xidea.el.fn;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.xidea.el.Invocable;

abstract class JSArray extends ECMA262Impl implements Invocable {
	@SuppressWarnings("unchecked")
	public Object invoke(Object thiz, Object... args) throws Exception {
		thiz = toList(thiz);
		return this.invoke((List<Object>) thiz, args);
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

	public abstract Object invoke(List<Object> thiz, Object... args)
			throws Exception;

	public static final Invocable slice = new JSArray() {
		@Override
		public Object invoke(List<Object> thiz, Object... args)
				throws Exception {
			return thiz.subList(getIntArg(args, 0, 0), getIntArg(args, 1, thiz
					.size()));
		}
	};
	public static final Invocable join = new JSArray() {
		@Override
		public Object invoke(List<Object> thiz, Object... args)
				throws Exception {
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
	};
	public static final Invocable splice = slice;

	public static final Invocable push = new JSArray() {
		@Override
		public Object invoke(List<Object> thiz, Object... args) {
			for (Object o : args) {
				thiz.add(o);
			}
			return null;
		}
	};
	public static final Invocable pop = new JSArray() {
		@Override
		public Object invoke(List<Object> thiz, Object... args) {
			int size = thiz.size();
			if (size > 0) {
				return thiz.remove(size - 1);
			}
			return null;
		}
	};
	public static final Invocable shift = new JSArray() {
		@Override
		public Object invoke(List<Object> thiz, Object... args) {
			int size = thiz.size();
			if (size > 0) {
				return thiz.remove(0);
			}
			return null;
		}
	};
	public static final Invocable unshift = new JSArray() {
		@Override
		public Object invoke(List<Object> thiz, Object... args) {
			for (int i = 0; i < args.length; i++) {
				thiz.add(i,args[i]);
			}
			return thiz.size();
		}
	};
	public static final Invocable concat = new JSArray() {
		@SuppressWarnings("unchecked")
		@Override
		public Object invoke(List<Object> thiz, Object... args) {
			List<Object > result = new ArrayList<Object>(thiz);
			for (Object o:args) {
				o = toList(o);
				if(o instanceof Collection){
					result.addAll((Collection)o);
				}else{
					result.add(o);
				}
			}
			return result;
		}
	};
	public static final Invocable reverse = new JSArray() {
		@Override
		public Object invoke(List<Object> thiz, Object... args) {
			Collections.reverse(thiz);
			return thiz;
		}
	};
	public static final Invocable sort = new JSArray() {
		@SuppressWarnings("unchecked")
		@Override
		public Object invoke(List thiz, Object... args) {
			Object o = getArg(args, 0, null);
			Comparator c = null;
			if(o instanceof Comparator){
				c = (Comparator)o;
			}
			Collections.sort(thiz,c);
			return thiz;
		}
	};

}
