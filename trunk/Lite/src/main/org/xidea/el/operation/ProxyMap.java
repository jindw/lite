package org.xidea.el.operation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

class ProxyMap extends HashMap<Object, Object> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Object base;
	ProxyMap(Object base) {
		this.base = base;
	}

	@Override
	public boolean containsValue(Object value) {
		for(Object key : super.keySet()){
			Object value2 = get(key);
			if(value == null){
				if(value == value){
					return true;
				}
			}else{
				if(value.equals(value2)){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Set<Entry<Object, Object>> entrySet() {
		@SuppressWarnings("unchecked")
		Set<Entry<Object, Object>> result = super.entrySet();
		for(Entry<Object, Object> entry : result){
			Object value = entry.getValue();
			if(value instanceof PropertyValue){
				
			}
		}
		return result;
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
	public Object put(Object key, Object value) {
		Object v = super.put(key, value);
		if (v instanceof PropertyEntry) {
			PropertyEntry pe = (PropertyEntry) v;
			Object result = (Object) pe.getValue();
			pe.setValue(value);
			return result;
		} else {
			return v;
		}
	}

	@Override
	public void putAll(Map<? extends Object, ? extends Object> m) {
		for (Map.Entry<? extends Object, ? extends Object> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public Collection<Object> values() {
		return super.values();
	}

	private static Object NULL = new Object();
	class PropertyEntry implements Map.Entry<Object, Object> {
		private Object key;
		private Object value = NULL;

		public PropertyEntry(String key) {
			this.key = key;
		}

		public Object getKey() {
			return key;
		}

		public Object getValue() {
			if (NULL == value) {
				value = ReflectUtil.getValue(base, key);
			}
			return value;
		}

		public Object setValue(Object value) {
			return this.value = value;
		}

	}
}
