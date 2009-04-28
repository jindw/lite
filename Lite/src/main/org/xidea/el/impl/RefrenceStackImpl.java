package org.xidea.el.impl;

import java.util.Map;

import org.xidea.el.Reference;

class RefrenceStackImpl extends ValueStackImpl {
	public RefrenceStackImpl(Object... context) {
		super(context);
	}

	public Object get(Object key) {
		int i = stack.length;
		while (i-- > 0) {
			Object context = stack[0];
			if (context instanceof Map) {
				return new PropertyValue(context, key);
			} else if (ReflectUtil.getType(context.getClass(), key) != null) {
				return new PropertyValue(context, key);
			}
		}
		return null;
	}

	protected static Reference wrapResult(final Object realValue) {
		return new Reference() {
			public Class<? extends Object> getType() {
				return realValue == null ? null : realValue.getClass();
			}

			public Object getValue() {
				return realValue;
			}

			public Object setValue(Object value) {
				throw new UnsupportedOperationException();
			}

			public Reference next(Object key) {
				throw new UnsupportedOperationException();
			}

			public Object getBase() {
				return null;
			}

			public Object getName() {
				return null;
			}
		};
	}
}
