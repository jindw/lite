package org.xidea.lite.parse;

import java.util.List;
import java.util.Map;

public interface OptimizePlugin{
	public void initialize(Map<String,Object> config,List<Object> childData,OptimizeContext context) ;
	public void before();
	public void optimize();
}
