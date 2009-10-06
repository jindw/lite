package org.jside.webserver;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

public class StringMap extends AbstractMap<String, String> implements
		Map<String, String> {
	private Map<String, String[]> baseMap;

	public StringMap(Map<String, String[]> baseMap) {
		this.baseMap = baseMap;
	}

	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set entrySet() {
		return baseMap.entrySet();
	}

	@Override
	public boolean containsKey(Object key) {
		return baseMap.containsKey(key);
	}

	@Override
	public String get(Object key) {
		String[] values = baseMap.get(key);
		if (values != null && values.length > 0) {
			return values[0];
		}
		return null;
	}

}
