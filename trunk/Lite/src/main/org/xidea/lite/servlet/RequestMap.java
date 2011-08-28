package org.xidea.lite.servlet;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

public class RequestMap extends AbstractMap<String, Object> {
	private static final long serialVersionUID = 1L;
	protected final HttpServletRequest request;
	private Set<java.util.Map.Entry<String, Object>> entries = null;

	public RequestMap(final HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public Object put(String key, Object value) {
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

	@SuppressWarnings("unchecked")
	public Set<String> keySet() {
		return new HashSet<String>( Collections.list(request.getAttributeNames()));
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		if (entries == null) {
			entries = new MapEntrySet(this);
		}
		return entries;
	}
}