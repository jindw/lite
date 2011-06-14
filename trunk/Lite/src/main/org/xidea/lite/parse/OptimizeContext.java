package org.xidea.lite.parse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OptimizeContext {
	public void walk(OptimizeWalker parseWalker);
	public OptimizePlugin getPlugin(List<Object> object);
	public OptimizeScope parseScope(List<Object> children, List<String> params);
	public Map<String, Set<String>> getDefCallMap();
	public void optimizeCallClosure(Map<String, Set<String>> callMap,
			Set<String> optimizedCall);
	public List<Object> optimize();
}
