package org.xidea.lite.parser;

public interface JSBuilder {
	public String buildJS(String id, Object liteCode);
	public String compress(String source);
}