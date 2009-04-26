package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.xidea.lite.parser.ResourceContext;

public class ResourceContextImpl implements ResourceContext {
	private URL currentURL;
	protected final URL base;
	private int sourceType = 0;
	
	private HashMap<Object, Object> attributeMap = new HashMap<Object, Object>();
	private HashSet<URL> resources = new HashSet<URL>();

	public ResourceContextImpl(URL base) {
		this.base = base;
	}

	public void setAttribute(Object key, Object value) {
		this.attributeMap.put(key, value);
	}

	public Object getAttribute(Object key) {
		return this.attributeMap.get(key);
	}

	public int getSourceType() {
		return sourceType;
	}

	public void setSourceType(int sourceType) {
		this.sourceType = sourceType;
	}

	public URL getCurrentURL() {
		return currentURL;
	}

	public Set<URL> getResources() {
		return resources;
	}

	public void addResource(URL resource) {
		resources.add(resource);
	}

	public void setCurrentURL(URL currentURL) {
		if (currentURL != null) {
			resources.add(currentURL);
		}
		this.currentURL = currentURL;
	}

	public URL createURL(URL parentURL, String path) {
		try {
			if (path.startsWith("/")) {
				return new URL(this.base, path.substring(1));
			} else {
				return new URL(parentURL != null ? parentURL : this.base, path);
			}

		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public InputStream getInputStream(URL url) {
		try {
			return url.openStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
