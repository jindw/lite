package org.xidea.el.fn;

import java.lang.reflect.Method;

import org.xidea.el.Invocable;

abstract class JSObject implements Invocable {
	protected Method method;
	public Class<?>[] params;
	public boolean directly;


	public Object invoke(Object thiz, Object... args) throws Exception {
		if(directly){
			return method.invoke(thiz, (Object)args);
		}else{
			Object[] args2 = new Object[params.length];
			for(int i = args2.length-1;i>0;i--){
				args2[i] = ECMA262Impl.ToValue(args.length>i?args[i]:null,params[1]);
			}
			return method.invoke(thiz, (Object)args2);
		}
	}


	public String toString() {
		return method.getName();
	}
}
