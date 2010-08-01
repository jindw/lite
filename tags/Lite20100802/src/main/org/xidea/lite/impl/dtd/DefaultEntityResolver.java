package org.xidea.lite.impl.dtd;

import java.io.IOException;
import java.util.HashMap;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

public class DefaultEntityResolver implements EntityResolver2 {

	// 默认值：link|input|meta|img|br|hr
	public String DTD_OUTPUT = "http://www.xidea.org/dtd/lite/dtd-output";
	public static final String OUTPUT_DTD = "org.xidea.lite.OUTPUT_DTD";
	private static HashMap<String, String> DEFAULT_DTD_MAP = new HashMap<String, String>();
	static {
		DEFAULT_DTD_MAP.put(OUTPUT_DTD,"xhtml1.dtd");
		DEFAULT_DTD_MAP.put("-//W3C//DTD XHTML 1.0 Transitional//EN",
				"xhtml1.dtd");
		DEFAULT_DTD_MAP.put("-//W3C//DTD XHTML 1.0 Strict//EN",
				"xhtml1.dtd");
		DEFAULT_DTD_MAP.put("-//W3C//DTD HTML 4.01//EN", "xhtml1.dtd");
		DEFAULT_DTD_MAP.put("-//W3C//DTD HTML 4.01//EN", "xhtml1.dtd");
		DEFAULT_DTD_MAP.put("-//W3C//DTD HTML 4.01 Transitional//EN", "xhtml1.dtd");
//		DEFAULT_DTD_MAP.put("-//W3C//DTD XHTML 1.0 Transitional//EN",
//				"xhtml1-transitional.dtd");
//		DEFAULT_DTD_MAP.put("-//W3C//DTD XHTML 1.0 Strict//EN",
//				"xhtml1-strict.dtd");
		DEFAULT_DTD_MAP.put("-//W3C//ENTITIES Latin 1 for XHTML//EN",
				"xhtml-lat1.ent");
		DEFAULT_DTD_MAP.put("-//W3C//ENTITIES Symbols for XHTML//EN",
				"xhtml-symbol.ent");
		DEFAULT_DTD_MAP.put("-//W3C//ENTITIES Special for XHTML//EN",
				"xhtml-special.ent");
		//无线
		DEFAULT_DTD_MAP.put("-//WAPFORUM//DTD WML 1.3//EN" , "xhtml1.dtd");
		DEFAULT_DTD_MAP.put("-//WAPFORUM//DTD XHTML Mobile 1.0//EN", "xhtml1.dtd");
	}

	public InputSource resolveEntity(String name, String publicId,
			String baseURI, String systemId) throws SAXException, IOException {
		return resolveEntity(publicId, systemId);
	}

	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, IOException {
		String path = DEFAULT_DTD_MAP.get(publicId);
		if (path == null) {
			return null;
		}
		InputSource source = new InputSource(this.getClass()
				.getResourceAsStream(path));
		if(source!=null){
			source.setSystemId(systemId);
		}
		return source;
	}

	public InputSource getExternalSubset(String name, String baseURI)
			throws SAXException, IOException {
		//<!doctype html>
		if("html".equalsIgnoreCase(name)){
			return resolveEntity(OUTPUT_DTD,".");
		}else{
			return null;
		}
	}

}
