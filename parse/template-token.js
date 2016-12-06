/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var EL_TYPE = 0;// [0,'el']
var IF_TYPE = 1;// [1,[...],'test']
var BREAK_TYPE = 2;// [2,depth]
var XA_TYPE = 3;// [3,'value','name']
var XT_TYPE = 4;// [4,'el']
var FOR_TYPE = 5;// [5,[...],'items','var']
var ELSE_TYPE = 6;// [6,[...],'test']//test opt?
var PLUGIN_TYPE =7;// [7,[...],'el','clazz']
var VAR_TYPE = 8;// [8,'value','name']
var CAPTURE_TYPE = 9;// [9,[...],'var']

var IF_KEY = "if";
var FOR_KEY = "for";
var PLUGIN_DEFINE = "org.xidea.lite.DefinePlugin";

exports.PLUGIN_DEFINE=PLUGIN_DEFINE;
exports.VAR_TYPE=VAR_TYPE;
exports.XA_TYPE=XA_TYPE;
exports.ELSE_TYPE=ELSE_TYPE;
exports.PLUGIN_TYPE=PLUGIN_TYPE;
exports.CAPTURE_TYPE=CAPTURE_TYPE;
exports.IF_TYPE=IF_TYPE;
exports.EL_TYPE=EL_TYPE;
exports.BREAK_TYPE=BREAK_TYPE;
exports.XT_TYPE=XT_TYPE;
exports.FOR_TYPE=FOR_TYPE;
