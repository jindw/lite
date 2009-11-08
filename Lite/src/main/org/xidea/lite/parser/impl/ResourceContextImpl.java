package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.xidea.lite.parser.ResourceContext;

public class ResourceContextImpl implements ResourceContext {
	protected final URI base;

	public ResourceContextImpl(URI base) {
		this.base = base;
	}

	public URI createURI(String path, URI parentURI) {
		try {
			URI parent = parentURI != null ? parentURI : this.base;
			if (path.startsWith("/")) {
				if (parentURI == null
						|| parent.toString().startsWith(base.toString())) {
					String prefix = base.getPath();
					int p  =prefix.lastIndexOf('/');
					path = prefix.substring(0,p)+path;
				}
			}
			return parent.resolve(path);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public InputStream openInputStream(URI uri) {
		try {
			if ("classpath".equalsIgnoreCase(uri.getScheme())) {
				ClassLoader cl = this.getClass().getClassLoader();
				String path = uri.getPath();
				path = path.substring(1);
				InputStream in = cl.getResourceAsStream(path);
				if (in == null) {
					ClassLoader cl2 = Thread.currentThread()
							.getContextClassLoader();
					if (cl2 != null) {
						in = cl2.getResourceAsStream(path);
					}
				}
				return in;
			} else {
				return uri.toURL().openStream();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
