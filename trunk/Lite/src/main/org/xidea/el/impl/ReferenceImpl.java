package org.xidea.el.impl;


import org.xidea.el.Reference;

class ReferenceImpl implements Reference {
	private Object base;
	private Object name;
	private Class<? extends Object> type;
	public ReferenceImpl(Object base, Object name) {
		this.base = base;
		this.name = name;
	}

	public String toString() {
		return String.valueOf(this.getValue());
	}

	public Reference next(Object key) {
		if (base != null) {
			Object context2 = getValue();
			if (context2 == null) {
				type = ReflectUtil.getPropertyClass(base.getClass(), name);
				base = null;
			} else {
				base = context2;
			}
		} else if (type != null) {
			type = ReflectUtil.getPropertyClass(type, name);
		}
		name = key;
		return this;
	}

	public Object getBase() {
		return base;
	}

	public Object getValue() {
		return ReflectUtil.getValue(base, name);
	}

	public Class<? extends Object> getType() {
		if (type != null) {
			return ReflectUtil.getPropertyClass(type, name);
		} else  if(base !=null){
			Object value = getValue();
			if (value != null) {
				return value.getClass();
			} else {
				Class<? extends Object> t = ReflectUtil.getPropertyClass(base
						.getClass(), name);
				return t == null ? Object.class : t;
			}
		}else{
			return null;
		}
	}

	public Object setValue(Object value) {
		ReflectUtil.setValue(base, name, value);
		return null;
	}

	public Object getName() {
		return name;
	}
}