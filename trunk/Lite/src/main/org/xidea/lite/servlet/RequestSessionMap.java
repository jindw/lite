package org.xidea.lite.servlet;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

public class RequestSessionMap extends RequestMap {
	private static final long serialVersionUID = 1L;
	private Map<String, Object> sessionMap;
	class SessionMap extends AbstractMap<String, Object> {
		@Override
		public Object get(Object key) {
			return request.getSession().getAttribute(String.valueOf(key));
		}
		@SuppressWarnings("unchecked")
		public Set<String> keySet() {
			return new HashSet<String>( Collections.list(request.getAttributeNames()));
		}

		@Override
		public Set<java.util.Map.Entry<String, Object>> entrySet() {
			return new MapEntrySet(this);
		}

	}

	public RequestSessionMap(HttpServletRequest request) {
		super(request);
	}
	@Override
	public Object get(Object key) {
		Object o = super.get(key);
		if(o == null && "session".equals(key)){
			if(sessionMap == null){
				sessionMap = new SessionMap();
			}
			return sessionMap;
		}
		return o;
	}
}