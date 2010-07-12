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
 *          "http://www.xidea.org/featrues/lite/output-encoding":"utf-8",
 * 			"http://www.xidea.org/featrues/lite/output-mime-type":"text/html",
 * 			"http://www.xidea.org/featrue/lite/html-javascript-compressor":"org.jside.jsi.tools.JSACompressor"
 * 		},
 * 		"extensions":[
 * 			{
 * 				"namespace":"http://www.w3.org/1999/xhtml",
 * 				"package":"org.xidea.lite.xhtml"
 * 			}
 * 		]
 * 	}
 * ]
 */
function ParseConfig(root,json){
	this.root = new URI(root||'.');
	this.config = defaultConfig;
	if(json){
		var result = [];
		var i = json.length
		while(i--){
			var item = {};
			copy(json[i],item);
			item.includes = new RegExp(item.includes.join('|')||"^$");
			item.excludes = new RegExp(item.excludes.join('|')||"^$");
			result[i] = item;
		}
		this.config = result;
	}
}
function copy(source,dest){
	for(var n in source){
		dest[n] = source[n];
	}
}
function findGroup(groups,path,require){
	for(var i=0,len = groups.length;i<len;i++){
		g = groups[i];
		if(g.includes.test(path)){
			if(!g.excludes.test(path)){
				return g;
			}
		}
	}
	return require && groups[groups.length-1];
}
ParseConfig.prototype = {
	/**
	 */
	getDecotatorPage:function(path){
		var g = findGroup(this.config,path,null)
		return g && g.featrueMap['http://www.xidea.org/featrues/lite/layout'];
	},
	getFeatrueMap:function(path){
		var result = {}
		var g = findGroup(this.config,path,null);
		if(g){
			copy(g.featrueMap,result);
		}
		return result;
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
				var g = findGroup(config.config,path,null);
				if(g){
					for(var es = g.extensions,len = es.length,i=0;i<len;i++){
						var ext = es[i];
						extension.addExtensionPackage(ext.namespace,ext['package'])
					}
				}
			}
			return extension.parse(node,context,chain);
		}
//		,
//		function(node,context,chain){
//			if(!extension){
//				context.setAttribute(ExtensionParser,extension = new ExtensionParser());
//			}
//			return text
//		}
	]
}
var defaultConfig = {
		"includes":/./,//"/example\\/*.xhtml"
		"excludes":/^$/,
		"featrueMap":{
			//必要属性（控制xml编译）
			"http://www.xidea.org/featrues/lite/output-encoding":"utf-8",
			//必要属性（控制xml编译）
			"http://www.xidea.org/featrues/lite/output-mime-type":"text/html"
		},
		"extensions":[
			//core 自行编译
			{//xhtml 编译不是自带的，需要自己定义
   				"namespace":"http://www.w3.org/1999/xhtml",
   				"package":"org.xidea.lite.xhtml"
			}
		]
	}
