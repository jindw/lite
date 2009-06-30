package org.xidea.lite;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class StaticAdvice implements CompileAdvice {
	private static Log log = LogFactory.getLog(StaticAdvice.class);

	public List<Object> compile(final Map<String, Object> gloabls,
			final Object[] children) {
		log.warn("未实现");
		return null;
	}

}
