package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.regex.Pattern;

import org.xidea.lite.parser.ResourceContext;

public class ResourceContextImpl implements ResourceContext {
	private static Pattern URI_ROOT = Pattern.compile("^[\\w]+\\:");
	private static Pattern URI_CLEAR = Pattern
			.compile("\\/\\.\\/|\\/[^\\/]+\\/\\.\\.\\/");
	protected final URI base;

	public ResourceContextImpl(URI base) {
		this.base = base;
	}

	public URI createURI(String path, URI parentURI) {
		try {
			if (URI_ROOT.matcher(path).find()) {
				return URI.create(path);
			} else {
				URI parent = parentURI != null ? parentURI : this.base;
				if (path.startsWith("/")) {
					if (parent.toString().startsWith(base.toString())) {
						path = (base.getPath() + path).replaceFirst(
								"^\\/|\\/\\/", "");
					}
				} else {
					String dir = parent.getPath();
					if (dir.endsWith("/")) {
						dir = dir + "a";
					}
					path = dir + "/../" + path;
					int length = -1;
					while (true) {
						path = URI_CLEAR.matcher(path).replaceFirst("/");
						int length2 = path.length();
						if (length2 == length) {
							break;
						} else {
							length = length2;
						}
					}
				}
				return new URI(parent.getScheme(), parent.getUserInfo(), parent
						.getHost(), parent.getPort(), path, null, null);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public InputStream openInputStream(URI uri) {
		try {
			if ("classpath".equalsIgnoreCase(uri.getScheme())) {
				ClassLoader cl = this.getClass().getClassLoader();
				String path = uri.getPath();
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
