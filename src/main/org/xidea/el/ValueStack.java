package org.xidea.el;

public interface ValueStack {
	public Object get(Object key);
	public void put(Object key,Object value);
}
