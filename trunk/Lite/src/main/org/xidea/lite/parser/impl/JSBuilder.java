package org.xidea.lite.parser.impl;

import java.util.List;

public interface JSBuilder {
	public String buildJS(List<Object> liteCode, String name);
	public String compress(String source);
}