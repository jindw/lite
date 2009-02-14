package org.xidea.lite;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BuildInAdvice implements CompileAdvice {
	private static Log log = LogFactory.getLog(BuildInAdvice.class);
	private Map<String, ? extends Object> instanceMap = null;

	public void setInstanceMap(Map<String, ? extends Object> instanceMap) {
		this.instanceMap = instanceMap;
	}

	public void compile(Map<String, Object> gloabls, List<Object> result) {
		for (String key : instanceMap.keySet()) {
			String type = (String) instanceMap.get(key);
			try {
				Object value = Class.forName(type).newInstance();
				gloabls.put(key, value);
			} catch (Exception e) {
				log.error("无法装载扩展：" + type, e);
			}
		}
	}

}
