package org.xidea.lite.servlet;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;

public class XWorkTempateServlet extends TempateServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected Map<Object, Object> createModel(final HttpServletRequest req) {
		OgnlContext context = (OgnlContext) com.opensymphony.xwork2.ActionContext
				.getContext().getValueStack().getContext();
		return new OgnlContextMap(context);
	}

	static class OgnlContextMap extends HashMap<Object, Object> {
		private static final long serialVersionUID = 1L;
		private OgnlContext context;

		public OgnlContextMap(OgnlContext context) {
			this.context = context;
		}

		@Override
		public Object get(Object key) {
			Object value = super.get(key);
			if (value == null && key instanceof String
					&& !super.containsKey(key)) {
				try {
					value = OgnlRuntime.getProperty(context, context.getRoot(),
							key);
				} catch (OgnlException e) {
				}
			}
			return value;
		}
	}

}
