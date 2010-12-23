package org.xidea.lite.parse;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ParseConfig {
	public URI getRoot();
	public Collection<URI> getResources();
//	public String getDecotatorPage(String path);
	public Map<String,String> getFeatrueMap(String path);
	public Map<String, List<String>> getExtensions(String path);
}