package org.xidea.lite.compiler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ProxyResourceContextImpl extends
		org.xidea.lite.parser.impl.ResourceContextImpl {
	private Map<String, String> resourceMap;
	private ArrayList<String> missedResources = new ArrayList<String>();
	private URL base;

	public ProxyResourceContextImpl(URL base,Map<String, String> resourceMap) {
		super(base);
		this.resourceMap = resourceMap;
	}

	public List<String> getMissedResources() {
		return missedResources;
	}

	public void addMissedResource(String path) {
		missedResources.add(path);
	}

	public String findXMLEncode(String xml) {
		// <?xml version="1.0" encoding="utf-8"?>
		if (xml.startsWith("<?xml")) {
			int end = xml.indexOf('>');
			int begin = xml.lastIndexOf("encoding", end);
			if (begin > 0 && begin < end) {
				begin = 1 + Math.min(xml.indexOf('\'', begin), xml.indexOf(
						'\"', begin));
				end = Math.min(xml.indexOf('\'', begin), xml.indexOf('\"',
						begin));
				if (begin > 0 && begin < end) {
					return xml.substring(begin, end);
				}
			}
		}
		return "utf-8";
	}

	@Override
	public InputStream getInputStream(URL url) {
		String path = url.getPath().substring(this.base.getPath().length() - 1);
		String result = resourceMap.get(path);
		try {
			if (result != null) {
				result = result.trim();
				String encoding = findXMLEncode(result);
				if (result.length() == 0) {
					result = "<body><div>empty : " + url + "</div></body>";
				}
				return new ByteArrayInputStream(result.getBytes(encoding));
			}
			if (!url.getHost().equals(this.base.getHost())) {
				InputStream in = super.getInputStream(url);
				if (in == null) {
					return in;
				}
			}
			this.missedResources.add(path);
			return new ByteArrayInputStream("<empty/>".getBytes());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
