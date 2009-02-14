package org.xidea.lite;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Expression;

public class BuildInAdvice implements CompileAdvice {
	private static Log log = LogFactory.getLog(BuildInAdvice.class);

	public void execute(Map<String, Object> gloabls, Expression expression,
			List<Object> result) {
		@SuppressWarnings("unchecked")
		Map<String, String> addOnMap = (Map<String, String>) expression
				.evaluate(null);
		for (Map.Entry<String, String> entry : addOnMap.entrySet()) {
			String key = entry.getKey();
			try {
				Object value = Class.forName(entry.getValue()).newInstance();
				gloabls.put(key, value);
			} catch (Exception e) {
				log.error("无法装载扩展：" + entry.getValue(), e);
			}
		}
	}
}
