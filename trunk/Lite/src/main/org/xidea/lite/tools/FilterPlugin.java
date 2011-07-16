package org.xidea.lite.tools;

public interface FilterPlugin<T> {
	public ResourceManager getFactory();
	public T doFilter(String path,T in);
}
