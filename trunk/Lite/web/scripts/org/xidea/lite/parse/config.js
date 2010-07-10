/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
//add as default
/**
 * 模板解析上下文对象实现
 * [
 * 	{
 * 		"includes":["/example\\/*.xhtml"],
 * 		"excludes":[],
 * 		"featrueMap":{
 * 			"http://www.xidea.org/featrues/lite/output-encoding":"utf-8",
 * 			"http://www.xidea.org/featrues/lite/output-mime-type":"text/html",
 * 			"http://www.xidea.org/featrue/lite/html-javascript-compressor":"org.jside.jsi.tools.JSACompressor"
 * 		},
 * 		"extensions":[
 * 			{
 * 				"key":"",
 * 				"value":"org.xidea.lite.impl"
 * 			},
 * 			{
 * 				"key":"http://www.w3.org/1999/xhtml",
 * 				"value":"org.xidea.lite.xhtml"
 * 			},
 * 			{
 * 				"key":"http://www.xidea.org/featrue/lite/core",
 * 				"value":"org.xidea.lite.core"
 * 			}
 * 		]
 * 	}
 * ]
 */
function ParseConfig(root,groups){
	this.root = root;
	this.groups = groups;
}

ParseConfig.prototype = {
	/**
	 */
	getDecotatorPage:function(path){
		return null;
	},
	getFeatrueMap:function(path){
		
	},
	getNodeParsers:function(path){
		return buildParser(this,path)
	}
}

function buildParser(config,path){
	var extension
	return [
		function(node,context,chain){//extension
			if(!extension){
				context.setAttribute(ExtensionParser,extension = new ExtensionParser());
			}
			return text
		},
		function(node,context,chain){
			if(!extension){
				context.setAttribute(ExtensionParser,extension = new ExtensionParser());
			}
			return text
		}
	]
}
var defaultConfig = {
		"includes":[".*"],//"/example\\/*.xhtml"
		"excludes":[],
		"featrueMap":{
			"http://www.xidea.org/featrues/lite/output-encoding":"utf-8",
			"http://www.xidea.org/featrues/lite/output-mime-type":"text/html"
		},
		"extensions":[
			{
				"key":"http://www.w3.org/1999/xhtml",
				"value":"org.xidea.lite.xhtml"
			},
			{
				"key":"http://www.xidea.org/featrue/lite/core",
				"value":"org.xidea.lite.core"
			}
		]
	}
