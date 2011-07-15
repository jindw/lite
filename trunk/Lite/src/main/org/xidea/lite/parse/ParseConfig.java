package org.xidea.lite.parse;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public interface ParseConfig {
	public URI getRoot();
	public Collection<URI> getResources();
	/**
	 * @see org.xidea.lite.impl.ParseConfigImpl#getFeatureMap(String)
	 */
	public Map<String,String> getFeatureMap(String path);
	public Map<String, List<String>> getExtensions(String path);
}