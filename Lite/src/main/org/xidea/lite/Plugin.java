package org.xidea.lite;


import java.io.Writer;


/**
 * @author jindw
 * 接口尚不成熟，第三方库暂勿使用
 */
public interface Plugin {
	public void initialize(Object[] children);
	public void execute(Context context,Writer out);
}
