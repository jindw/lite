package org.xidea.el.operation;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

class ProxyMap extends HashMap<String, Object> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Object base;
	ProxyMap(Object base, Map<String, PropertyDescriptor> ps) {
		this.base = base;
		for (String key : ps.keySet()) {
			super.put(key, new PropertyEntry(key));
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	public Object get(Object key) {
		Object v = super.get(key);
		if (v instanceof PropertyEntry) {
			return (Object) ((PropertyEntry) v).getValue();
		} else {
			return v;
		}
	}

	@Override
	public Object put(String key, Object value) {
		Object v = super.put(key, value);
		if (v instanceof PropertyEntry) {
			PropertyEntry pe = (PropertyEntry) v;
			return pe.setValue(value);
		} else {
			return v;
		}
	}
//
//	@Override
//	public Set<Entry<Object, Object>> entrySet() {
//		@SuppressWarnings("unchecked")
//		Set<Entry<Object, Object>> result = new HashSet<Entry<Object,Object>>(super.entrySet());
//		for(Entry<Object, Object> entry : result){
//			Object value = entry.getValue();
//			if(value instanceof PropertyValue){
//				entry.setValue(((PropertyValue)value).getValue());
//			}
//		}
//		return result;
//	}


//
//	@Override
//	public void putAll(Map<? extends Object, ? extends Object> m) {
//		for (Map.Entry<? extends Object, ? extends Object> entry : m.entrySet()) {
//			put(entry.getKey(), entry.getValue());
//		}
//	}


	protected class PropertyEntry implements Map.Entry<Object, Object> {
		private Object key;
		public PropertyEntry(String key) {
			this.key = key;
		}

		public Object getKey() {
			return key;
		}

		public Object getValue() {
			return ReflectUtil.getValue(base, key);
		}

		public Object setValue(Object value) {
			Object old =  getValue();
			ReflectUtil.setValue(base, key,value);
			return old;
		}

	}
}
