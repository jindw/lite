package org.xidea.lite;

import java.util.List;
import java.util.Map;

import org.xidea.el.Expression;

public interface CompileAdvice {
	public void execute(Map<String, Object> gloabls,Expression expression, List<Object> result);
}
