package org.xidea.webwork;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.opensymphony.xwork.ActionContext;
import com.opensymphony.xwork.util.OgnlValueStack;

/**
 * <!-- START SNIPPET: javadoc -->
 * 
 * All WebWork requests are wrapped with this class, which provides simple JSTL
 * accessibility. This is because JSTL works with request attributes, so this
 * class delegates to the value stack except for a few cases where required to
 * prevent infinite loops. Namely, we don't let any attribute name with "#" in
 * it delegate out to the value stack, as it could potentially cause an infinite
 * loop. For example, an infinite loop would take place if you called:
 * request.getAttribute("#attr.foo").
 * 
 * <!-- END SNIPPET: javadoc -->
 * 
 * @since 2.2
 */
public class WebworkRequestWrapper extends HttpServletRequestWrapper {
	public WebworkRequestWrapper(HttpServletRequest req) {
		super(req);
	}

	public Object getAttribute(String key) {
		if (key != null && key.startsWith("javax.servlet")) {
			// see WW-953 and the forums post linked in that issue for more info
			return super.getAttribute(key);
		}

		Object attribute = super.getAttribute(key);

		if (attribute == null && key.indexOf("#") == -1) {
			// WW-1365
			ActionContext ctx = ActionContext.getContext();
			// note: we don't let # come through or else a request for
			// #attr.foo or #request.foo could cause an endless loop
			if (ctx.get(WebworkRequestWrapper.class) != Boolean.TRUE) {
				try {
					// If not found, then try the ValueStack
					ctx.put(WebworkRequestWrapper.class, Boolean.TRUE);
					OgnlValueStack stack = ctx.getValueStack();
					if (stack != null) {
						attribute = stack.findValue(key);
					}
				} finally {
					ctx.put(WebworkRequestWrapper.class, Boolean.FALSE);
				}
			}
		}
		return attribute;
	}
}
