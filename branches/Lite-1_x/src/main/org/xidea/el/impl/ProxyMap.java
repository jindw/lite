package org.xidea.el.impl;

import java.util.HashMap;
import java.util.Set;


public class ProxyMap extends HashMap<String, Object> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ProxyMap(Object base, Set<String> ps) {
		for (String key : ps) {
			super.put(key, new ReferenceImpl(base,key));
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	public Object get(Object key) {
		Object v = super.get(key);
		if (v instanceof ReferenceImpl) {
			return (Object) ((ReferenceImpl) v).getValue();
		} else {
			return v;
		}
	}
//	public Object getPropertyValue(Object key){
//		return super.get(key);
//	}

	@Override
	public Object put(String key, Object value) {
		Object v = super.put(key, value);
		if (v instanceof ReferenceImpl) {
			ReferenceImpl pe = (ReferenceImpl) v;
			return pe.setValue(value);
		} else {
			return v;
		}
	}
}
