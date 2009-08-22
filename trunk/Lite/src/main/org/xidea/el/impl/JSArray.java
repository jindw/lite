package org.xidea.el.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xidea.el.Invocable;

public abstract class JSArray extends ECMA262Impl implements Invocable {
	@SuppressWarnings("unchecked")
	private static Class[] classes = new Class[] { List.class, Object[].class,
			int[].class, float[].class, double[].class, long[].class,
			short[].class, byte[].class, char[].class };

	@SuppressWarnings("unchecked")
	public static void appendTo(CalculaterImpl calculater) {
		Field[] fields = JSArray.class.getFields();
		for (Field field : fields) {
			if (field.getType().isAssignableFrom(Invocable.class)
					&& (field.getModifiers() & Modifier.STATIC) > 0) {
				try {
					Invocable inv = (Invocable) field.get(null);
					for (Class type : classes) {
						calculater.addMethod(type, field.getName(), inv);
					}
				} catch (Exception e) {
				}

			}
		}

	}

	@SuppressWarnings("unchecked")
	public Object invoke(Object thiz, Object... args) throws Exception {
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
		return this.invoke((List<Object>) thiz, args);
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

}
