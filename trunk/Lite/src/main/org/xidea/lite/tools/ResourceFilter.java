package org.xidea.lite.tools;

public interface ResourceFilter<T> {
	//public ResourceManager getFactory();
	public T doFilter(String path,T in);
}
