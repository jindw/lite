package org.xidea.lite.parse;

import java.util.List;


/**
 * 插件标识接口,用来标识一个Plugin是否有独立的上下文(函数,前端模板)
 * @author jindw
 */
public interface OptimizeScope{
	/**
	 * 明确的函数调用,不包括变量/参数(var ,params)做为函数的调用(标记为不确定,添加'*' 至列表),不包括表达式返回的函数调用
	 * @return
	 */
	public List<String> getCalls();
	/**
	 * 本地申明变量表[参看:var,语法,不包括函数定义,不包括参数]
	 */
	public List<String> getVars();
	/**
	 * 参数列表
	 */
	public List<String> getParams();
	/**
	 * 全部引用变量表[不包含明确的函数调用]
	 */
	public List<String> getRefs();
	/**
	 * 明确的外部引用变量表[不包含前期已申明的变量的引用和明确的函数调用]
	 */
	public List<String> getExternalRefs();
}
