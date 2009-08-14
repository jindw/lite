package org.xidea.lite.compiler;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.xidea.lite.plugin.PluginLoader;

class ProxyParseContext extends
		org.xidea.lite.parser.impl.ParseContextImpl {
	private static final String BASE = "http://localhost/";
	private URL base;

	@SuppressWarnings("unchecked")
	public ProxyParseContext(String base,
			Map<String, String> featrueMap,
			Map<String, String> resourceMap,String pluginSource)
			throws MalformedURLException {
		this.base = new URL(new URL(BASE), base);
		super.initialize(this.base, featrueMap, null, null);
		if(pluginSource != null && pluginSource.trim().length()>0){
			PluginLoader loader = new PluginLoader();
			loader.load(new StringReader(pluginSource));
			loader.setup(this);
		}
		resourceContext = new ProxyResourceContextImpl(this.base,resourceMap);
	}

	public void addMissedResource(String path) {
		((ProxyResourceContextImpl)resourceContext).addMissedResource(path);
	}

	public List<String> getMissedResources() {
		return ((ProxyResourceContextImpl)resourceContext).getMissedResources();
	}
}
