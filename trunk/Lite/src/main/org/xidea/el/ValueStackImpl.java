package org.xidea.el;

import java.util.Map;

import org.xidea.el.operation.ReflectUtil;

public class ValueStackImpl implements ValueStack {
	protected Object[] stack;
	public ValueStackImpl(Object... stack) {
		this.stack = stack;
	}
	public Object get(Object key){
		int i = stack.length;
		while(i-->0){
			Object context = stack[0];
			if (context instanceof Map) {
				Map<?, ?> contextMap = (Map<?, ?>) context;
				Object result = contextMap.get(key);
				if (result !=null || contextMap.containsKey(key)){
					return result;
				}
			}else  {
				Object result = ReflectUtil.getValue(context, key);
				if(result != null || ReflectUtil.getType(context.getClass(), key) != null){
					return result;
				}
			}
		}
		return null;
	}
	public void put(Object key,Object value){
		int i = stack.length;
		while(i-->0){
			Object context = stack[0];
			if (context instanceof Map) {
				Map<?, ?> contextMap = (Map<?, ?>) context;
				Object result = contextMap.get(key);
				if (result !=null || contextMap.containsKey(key)){
					return result;
				}
			}else  {
				Object result = ReflectUtil.getValue(context, key);
				if(result != null || ReflectUtil.getType(context.getClass(), key) != null){
					return result;
				}
			}
		}
		return null;
	}
	public void put(Object key,int level,Object value){
		
	}

}
