package org.xidea.lite;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BuildInAdvice implements CompileAdvice {
	public final static String INSTANCE_MAP = "instanceMap";
	public final static String OBJECT_MAP = "objectMap";
	private static Log log = LogFactory.getLog(BuildInAdvice.class);
	private Map<String, ? extends Object> instanceMap = Collections.emptyMap();
	private Map<String, ? extends Object> objectMap = Collections.emptyMap();

	public void setInstanceMap(Map<String, ? extends Object> instanceMap) {
		this.instanceMap = instanceMap;
	}

	public void setObjectMap(Map<String, ? extends Object> objectMap) {
		this.objectMap = objectMap;
	}

	public void compile(Template template, List<Object> result) {
		for (String key : instanceMap.keySet()) {
			String type = (String) instanceMap.get(key);
			try {
				Object value = Class.forName(type).newInstance();
				template.gloabls.put(key, value);
			} catch (Exception e) {
				log.error("无法装载扩展：" + type, e);
			}
		}
		template.gloabls.putAll(objectMap);
	}

}
