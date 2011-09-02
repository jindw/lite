package org.xidea.lite.parse;

import java.util.List;


/**
 * 插件标识接口,用来标识一个Plugin是否有独立的上下文(函数,前端模板)
 * @author jindw
 */
public interface OptimizeScope{
	/**
	 * 明确的函数调用(非var,params调用,非表达式调用)
	 * 
	 * 不包括变量/参数(var ,params)做为函数的调用
	 * 不包括表达式返回的函数调用
	 * (变量,做为函数调用的时候,scope的calls 不确定, 需要添加'*' 至列表加以区别);
	 * 仅仅是参数做为函数调用,这个调用不会被单独记录下来,而是由调用者记录该数据???
	 * 这个不靠谱啊!!!
	 * 
	 * 当存在不确定的函数调用的时候,
	 * 前端模板需要将有所出现的非var非params引用都标记为可能的函数调用.
	 * @return
	 */
	public List<String> getCalls();
	/**
	 * 本地申明变量表[参看:var,语法,不包括函数定义,不包括参数]
	 */
	public List<String> getVars();
	/**
	 * 本地申明的函数表[参看:def,语法,不包括变量,不包括参数]
	 */
	public List<String> getDefs();
	/**
	 * 参数列表,不能为空.
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
