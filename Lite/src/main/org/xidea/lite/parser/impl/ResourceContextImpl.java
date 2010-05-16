package org.xidea.lite.parser.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;

import org.xidea.lite.parser.ResourceContext;

public class ResourceContextImpl implements ResourceContext {
	protected URI base;
	public ResourceContextImpl(URI base) {
		this.base = base;
	}
	public URI createURI(String path, URI parentURI) {
		try {
			URI parent = parentURI != null ? parentURI : this.base;
			if (path.startsWith("/")) {
				if (parentURI == null
						|| parent.toString().startsWith(base.toString())) {
					String prefix = base.getRawPath();
					int p  =prefix.lastIndexOf('/');
					path = prefix.substring(0,p)+path;
				}
			}
			return parent.resolve(path);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public InputStream openStream(URI uri) {
		try {
			if ("data".equalsIgnoreCase(uri.getScheme())) {
				String data = uri.getRawSchemeSpecificPart();
				int p = data.indexOf(',')+1;
				String h = data.substring(0,p).toLowerCase();
				String charset = "UTF-8";
				data = data.substring(p);
				p = h.indexOf("charset=");
				if(p >0){
					charset = h.substring(h.indexOf('=',p)+1,h.indexOf(',',p));
				}
				return new ByteArrayInputStream(URLDecoder.decode(data,charset).getBytes(charset));
				//charset=
			}else if ("classpath".equalsIgnoreCase(uri.getScheme())) {
				ClassLoader cl = this.getClass().getClassLoader();
				uri = uri.normalize();
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
