package org.xidea.el.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;


class ProxyMap extends HashMap<String, Object> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ProxyMap(Object base, Set<String> ps) {
		for (String key : ps) {
			super.put(key, new ReferenceImpl(base,key));
		}
	}
	@Override
	public Object get(Object key) {
		Object v = super.get(key);
		if (v instanceof ReferenceImpl) {
			return ((ReferenceImpl) v).getValue();
		} else {
			return v;
		}
	}
	


	@Override
	public Set<Entry<String, Object>> entrySet() {
		Set<Entry<String, Object>> result = super.entrySet();
		for(Entry<String, Object> x : result){
			Object value = x.getValue();
			if(value instanceof ReferenceImpl) {
				x.setValue(((ReferenceImpl)value).getValue());
			}
		}
		return result;
	}
	@Override
	public Collection<Object> values() {
		entrySet();
		return super.values();
	}
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
