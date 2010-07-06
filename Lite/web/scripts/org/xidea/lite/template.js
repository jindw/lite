/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

/**
 * 如果传入的是json 数组 或者是函数对象，直接作为编译结果初始化，否则，作为源代码编译。
 * @param data 模板源代码或者编译结果
 * @param parser 解析器对象，或者类名（通过jsi导入），可选
 * <a href="http://code.google.com/p/lite/wiki/Template"> 模版基础指令说明</a>
 * @public
 */
function Template(data,parser){
    if(":debug"){
        return new $import("org.xidea.lite.impl",{})(data,parser)
    }else{
	    /**
	     * 模板数据
	     * @private
	     * @tyoeof string
	     */
    	this.render = data;
    }
}
