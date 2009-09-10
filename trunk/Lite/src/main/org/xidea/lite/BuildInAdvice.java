package org.xidea.lite;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BuildInAdvice implements CompileAdvice {
	private static Log log = LogFactory.getLog(BuildInAdvice.class);
	private String name;
//	private Object value;
	private String type;

	public void setName(String name) { 
		this.name = name;
	}
//
//	public void setValue(Object value) {
//		this.value = value;
//	}

	public void setType(String type) {
		this.type = type;
	}

	public List<Object> compile(final Map<String, Object> gloabls, final Object[] children) {
		if (type != null) {
			try {
				Object value = Class.forName(type).newInstance();
				gloabls.put(name, value);
			} catch (Exception e) {
				log.error("无法装载扩展：" + type, e);
			}
		}else{
//			gloabls.put(name,value);
		}
		return null;
	}


}
