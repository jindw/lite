package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.xidea.lite.parser.ResourceContext;

public class ResourceContextImpl implements ResourceContext {
	private URI currentURI;
	protected final URI base;
	private int textType = 0;

	private HashMap<Object, Object> attributeMap = new HashMap<Object, Object>();
	private HashSet<URI> resources = new HashSet<URI>();

	public ResourceContextImpl(URI base) {
		this.base = base;
	}

	public void setAttribute(Object key, Object value) {
		this.attributeMap.put(key, value);
	}

	public Object getAttribute(Object key) {
		return this.attributeMap.get(key);
	}

	public int getTextType() {
		return textType;
	}

	public void setTextType(int textType) {
		this.textType = textType;
	}

	public URI getCurrentURI() {
		return currentURI;
	}

	public Set<URI> getResources() {
		return resources;
	}

	public void addResource(URI resource) {
		resources.add(resource);
	}

	public void setCurrentURI(URI currentURI) {
		if (currentURI != null) {
			resources.add(currentURI);
		}
		this.currentURI = currentURI;
	}

	public URI createURI(String path, URI parentURI) {
		try {
			if (path.startsWith("/")) {
				return new URL(this.base.toURL(), path.substring(1)).toURI();
			} else {
				return new URL(parentURI != null ? parentURI.toURL() : this.base.toURL(), path).toURI();
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public InputStream openInputStream(URI url) {
		try {
			return url.toURL().openStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
