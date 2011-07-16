package org.xidea.lite.parse;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;


public interface ParseConfig {
	public URI getRoot();
	public Collection<URI> getResources();



	public String loadText(URI uri) throws IOException ;
	/**
	 * 装载指定XML文档。
	 * 
	 * @param uri
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public Document loadXML(URI uri) throws SAXException, IOException;
	/**
	 * @see org.xidea.lite.impl.ParseConfigImpl#getFeatureMap(String)
	 */
	public Map<String,String> getFeatureMap(String path);
	public Map<String, List<String>> getExtensions(String path);
}