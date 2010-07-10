package org.xidea.lite.parse;

import java.net.URI;
import java.util.Map;

public interface ParseConfig {
	public String getDecotatorPage(String path);
	public URI getRoot();
	public Map<String,String> getFeatrueMap(String path);
	public TextParser[] getTextParsers(String path);
	public NodeParser<? extends Object>[] getNodeParsers(String path);
//	public Extensions[] getExtensions(String path);
}