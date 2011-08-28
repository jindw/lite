package org.xidea.lite.servlet;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class MapEntrySet extends
		AbstractSet<java.util.Map.Entry<String, Object>> {
	private Map<String, Object> map;

	public MapEntrySet(Map<String, Object> map) {
		this.map = map;
	}

	@Override
	public Iterator<Entry<String, Object>> iterator() {
		final Iterator<String> keys = map.keySet().iterator();
		return new Iterator<Entry<String, Object>>() {
			public boolean hasNext() {
				return keys.hasNext();
			}

			public Entry<String, Object> next() {
				final String key = keys.next();
				return new Entry<String,Object>() {
					public String getKey() {
						return key;
					}

					public Object getValue() {
						return map.get(key);
					}

					public Object setValue(Object value) {
						return null;
					}

				};
			}

			public void remove() {
			}
		};
	}

	@Override
	public int size() {
		return map.keySet().size();
	}

}
