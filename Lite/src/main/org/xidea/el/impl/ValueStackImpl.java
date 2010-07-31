package org.xidea.el.impl;

import java.util.Map;

import org.xidea.el.Invocable;
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
			if (context instanceof Map<?,?>) {
				Map<?, ?> contextMap = (Map<?, ?>) context;
				Object result = contextMap.get(key);
				if (result !=null || contextMap.containsKey(key)){
					return result;
				}
			}else if(context!=null) {
				Object result = ReflectUtil.getValue(context, key);
				Class<?> clazz = context.getClass();
				if(result != null || ReflectUtil.getPropertyType(clazz, key) != null){
					return result;
				}
				if(key instanceof String){
					final Object thiz = context;
					final Invocable inc = ReferenceImpl.getInvocable(clazz,(String)key,-1);
					return new Invocable() {
						public Object invoke(Object thiz2, Object... args) throws Exception {
							return inc.invoke(thiz, args);
						}
					};
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
