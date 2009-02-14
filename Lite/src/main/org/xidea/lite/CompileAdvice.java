package org.xidea.lite;

import java.util.List;
import java.util.Map;

/**
 * @author jindw
 * 接口尚不成熟，第三方库暂勿使用
 */
interface CompileAdvice {
	public void compile(Map<String, Object> gloabls,List<Object> result);
}
