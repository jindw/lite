package org.xidea.lite.tools.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.tools.ResourceFactory;

public class ResourceFactoryImpl implements ResourceFactory {
	private ParseConfig config;
	private final Map<String, ResourceItem> cached = new HashMap<String, ResourceItem>();
	private ResourceItem resource(String path){
		ResourceItem item = cached.get(path);
		if(item == null){
			item = new ResourceItem();
			try{
				InputStream in = ParseUtil.openStream(config.getRoot().resolve(path.substring(1)));
				item.rawData = new CachedInputStream(in);
				in.close();
			}catch(IOException ex){
				
			}
			cached.put(path, item);
		}
		return item;
	}
	public void addRelation(String currentPath,String relationPath){
		resource(currentPath).relations.add(relationPath);
	}
	public InputStream getResourceAsStream(String path){
		return null;
	}
	public String getEncoding(String path) {
		return config.getFeatureMap(path).get(ParseContext.FEATURE_ENCODING);
	}
	public InputStream getRawAsStream(String path) {
		return resource(path).rawData;
	}
}
class ResourceItem{
	ArrayList<String> relations = new ArrayList<String>();
	InputStream rawData;
	Object data;
	long lastModified;
}
