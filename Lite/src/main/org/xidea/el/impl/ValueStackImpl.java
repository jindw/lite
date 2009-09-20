package org.xidea.el.impl;

import java.util.Map;

import org.xidea.el.ValueStack;

public class ValueStackImpl implements ValueStack {
	protected Object[] stack;
	public ValueStackImpl(Object... stack) {
		this.stack = stack;
	}
	public Object get(Object key){
		int i = stack.length;
		while(i-->0){
			Object context = stack[i];
			if (context instanceof Map) {
				Map<?, ?> contextMap = (Map<?, ?>) context;
				Object result = contextMap.get(key);
				if (result !=null || contextMap.containsKey(key)){
					return result;
				}
			}else if(context!=null) {
				Object result = ReflectUtil.getValue(context, key);
				if(result != null || ReflectUtil.getPropertyType(context.getClass(), key) != null){
					return result;
				}
			}
		}
		return null;
	}
	public void put(Object key,Object value){
		put(key,value,-1);
	}
	public void put(Object key,Object value,int level){
		if(level<0){
			level = level + stack.length;
		}
		ReflectUtil.setValue(stack[level], key,value);
	}

}
