package org.xidea.lite;

import java.util.List;

/**
 * @author jindw
 * 接口尚不成熟，第三方库暂勿使用
 */
interface CompileAdvice {
	public List<Object> compile(Template template,Object[] children);
}
