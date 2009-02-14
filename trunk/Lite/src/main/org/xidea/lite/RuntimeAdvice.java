package org.xidea.lite;

import java.io.Writer;
import java.util.Map;

/**
 * @author jindw
 * 接口尚不成熟，第三方库暂勿使用
 */
interface RuntimeAdvice {
	public void execute(Map<? extends Object, ? extends Object> context,Writer out);
}
