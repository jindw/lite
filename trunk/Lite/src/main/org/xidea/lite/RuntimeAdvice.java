package org.xidea.lite;

import java.io.Writer;

import org.xidea.el.ValueStack;

/**
 * @author jindw
 * 接口尚不成熟，第三方库暂勿使用
 */
interface RuntimeAdvice {
	public void execute(ValueStack context,Writer out);
}
