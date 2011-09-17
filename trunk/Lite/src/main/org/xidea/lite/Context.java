//package org.xidea.lite;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import org.xidea.el.impl.ReflectUtil;
//import org.xidea.el.impl.ValueStackImpl;
//
//class Context extends ValueStackImpl {
//	private static final long serialVersionUID = 1L;
//	private Map<Object, Object> cache= null;
//	private Map<String, Object> base;
//	@SuppressWarnings("unchecked")
//	public Context(Object context) {
//		this.base = (Map<String, Object>) ReflectUtil.map(context);
//	}
//
//	public Object get(Object key) {
//		if(cache != null){
//			Object o = cache.get(key);
//			if(o != null || cache.containsKey(key)){
//				return o;
//			}
//		}
//		return base == null? null:base.get(key);
//	}
//
//	public void put(Object key, Object value) {
//		if(cache == null){
//			cache = new HashMap<Object, Object>();
//		}
//		cache.put(key, value);
//	}
//
//}