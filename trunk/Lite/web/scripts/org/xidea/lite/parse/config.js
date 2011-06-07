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
 * 		"featureMap":{
 *          "http://www.xidea.org/lite/features/output-encoding":"utf-8",
 * 			"http://www.xidea.org/lite/features/output-mime-type":"text/html",
 * 			"http://www.xidea.org/lite/features/html-javascript-compressor":"org.jside.jsi.tools.JSACompressor"
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
	this._root = new URI(root||(window.location?window.location.href.replace(/(.*?\w\/).*/,'$1'):'http://localhost/'));
	if(json){
		var result = [];
		var i = json.length
		while(i--){
			var item = {};
			copy(json[i],item);
			item.includes = new RegExp(item.includes||"^$");
			item.excludes = new RegExp(item.excludes||"^$");
			result[i] = item;
		}
		this._config = result;
	}else{
		this._config = defaultConfig;
	}
}
function copy(source,dest){
	for(var n in source){
		dest[n] = source[n];
	}
}
function findGroup(groups,path,require){
	for(var i=0,len = groups.length;i<len;i++){
		var g = groups[i];
		if(g.includes.test(path)){
			if(!g.excludes.test(path)){
				return g;
			}
		}
	}
	return require && groups[groups.length-1];
}
ParseConfig.prototype = {
//	getDecotatorPage:function(path){
//		var g = findGroup(this.config,path,null)
//		return g && g.featureMap['http://www.xidea.org/lite/features/layout'];
//	},
	getFeatureMap:function(path){
		var result = {}
		var g = findGroup(this._config,path,null);
		if(g){
			copy(g.featureMap,result);
		}
		return result;
	},
	getExtensions:function(path){
		var g = findGroup(this._config,path,null);
		if(g){
			return g.extensions;
		}
		return [];
		
	}
}

var defaultConfig = {
		"includes":/./,//"/example\\/*.xhtml"
		"excludes":/^$/,
		"featureMap":{
			//必要属性（控制xml编译）
			"http://www.xidea.org/lite/features/output-encoding":"utf-8",
			//必要属性（控制xml编译）
			"http://www.xidea.org/lite/features/output-mime-type":"text/html"
		},
		"extensions":[
			//core 自行编译
			{//xhtml 编译不是自带的，需要自己定义
   				"namespace":"http://www.w3.org/1999/xhtml",
   				"package":"org.xidea.lite.xhtml"
			},
			{//xhtml 编译不是自带的，需要自己定义
   				"namespace":"http://firekylin.my.baidu.com/ns/2010",
   				"package":"org.xidea.lite.xhtml"
			}
		]
	}
