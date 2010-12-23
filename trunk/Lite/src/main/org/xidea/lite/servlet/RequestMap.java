package org.xidea.lite.servlet;

import java.util.AbstractMap;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

public class RequestMap extends AbstractMap<Object, Object> {
	private static final long serialVersionUID = 1L;
	protected final HttpServletRequest request;

	public RequestMap(final HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public Object put(Object key, Object value) {
		request.setAttribute((String) key, value);
		return null;
	}

	@Override
	public Object get(Object key) {
		return request.getAttribute((String) key);
	}

	@Override
	public boolean containsKey(Object key) {
		return key instanceof String
				&& request.getAttribute((String) key) != null;
	}

	@Override
	public Set<java.util.Map.Entry<Object, Object>> entrySet() {
		throw new UnsupportedOperationException();
	}
}