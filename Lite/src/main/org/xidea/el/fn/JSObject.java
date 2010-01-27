package org.xidea.el.fn;

import java.lang.reflect.Method;

import org.xidea.el.Invocable;

abstract class JSObject implements Invocable {
	protected Method method;
	public Class<?>[] params;
	public boolean directly;


	public Object invoke(Object thiz, Object... args) throws Exception {
		if(directly){
			return method.invoke(this,thiz, (Object)args);
		}else{
			Object[] args2 = new Object[params.length];
			for(int i = args2.length-1;i>0;i--){
				args2[i] = ECMA262Impl.ToValue(args.length>=i?args[i-1]:null,params[1]);
			}
			args2[0] = thiz;
			return method.invoke(this,args2);
		}
	}


	public String toString() {
		return method.getName();
	}
}
