package org.xidea.lite;

import java.io.Writer;
import java.util.Map;

public interface RuntimeAdvice {
	public void execute(Map<? extends Object, ? extends Object> context,Writer out);
}
