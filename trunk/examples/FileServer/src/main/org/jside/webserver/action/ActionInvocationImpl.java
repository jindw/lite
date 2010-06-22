package org.jside.webserver.action;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jside.webserver.RequestContext;
import org.xidea.el.impl.CommandParser;
import org.xidea.el.impl.ReflectUtil;

public class ActionInvocationImpl implements ActionInvocation {
	private static Log log = LogFactory.getLog(ActionInvocationImpl.class);
	private Class<? extends Object> type;
	private Method method;
	private Object action;
	private URLMatcher pattern;



	public ActionInvocationImpl(String path, Object action) {
		initialize(path, action);
	}
	public static URLMatcher toPathPattern(String path) {
		return URLMatcher.createMatcher(path);
	}

	@SuppressWarnings("unchecked")
	private void initialize(String path, Object object) {
		if(object instanceof Class<?>){
			this.type = (Class<? extends Object>)object;
		}else{
			this.type = object.getClass();
			this.action = object;
		}
		this.pattern = toPathPattern(path);
		try {
			this.method = type.getMethod("execute");
			try{
				this.method.setAccessible(true);
			}catch(Exception e){}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public boolean match(String uri) {
		return pattern.match(uri);
	}

	public void execute(RequestContext context) throws Exception {
		Object action = this.action != null ? this.action : type.newInstance();
		doExecute(context, action, method);
	}

	static void doExecute(RequestContext context, Object action, Method method)
			throws IllegalAccessException, InvocationTargetException {
		try {
			new CommandParser(null).setup(action, context.getParams());
		} catch (Exception e) {
			log.warn("无效参数:" + context.getParam(), e);
		}
		Object result = method.invoke(action);
		if (result instanceof String) {
			context.getContextMap().putAll(ReflectUtil.map(result));
			String result2 = (String) result;
			// if (result2.matches("[\\w+"+Pattern.quote("\\/-.")+"]")) {
			String path;
			if (result2.startsWith("/")) {
				path = result2;
			} else {
				path = context.getRequestURI();
				path = path.substring(0, path.lastIndexOf('/') + 1) + result;
			}
			context.dispatch(path);
			// }
		}
	}
}