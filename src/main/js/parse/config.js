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
 * 		"config":{
 *          "encoding":"utf-8",
 * 			"contentType":"text/html;charset=UTF-8"
 * 		},
 * 		"extensions":[
 * 			{
 * 				"namespace":"http://www.xidea.org/lite/core",
 * 				"package":"lite/parse/i18n"
 * 			}
 * 		]
 * 	}
 * ]
 */
function ParseConfig(root,dom){
	this.root = new URI(root && root.replace(/[\\\/]*$/,'/') || 'lite:///');
	var json = dom && parseConfig(dom);
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
		this._groups = result;
	}else{
		this._groups = defaultConfig;
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
//		return g && g.config['layout'];
//	},
	getConfig:function(path){
		var result = {}
		var g = findGroup(this._groups,path,null);
		if(g){
			copy(g.config,result);
		}
		return result;
	},
	getExtensionMap:function(path){
		var g = findGroup(this._groups,path,null);
		if(g){
			return g.extensionMap;
		}
		return {};
		
	}
}

var defaultConfig = {
		"includes":/./,//"/example\\/*.xhtml"
		"excludes":/^$/,
		"config":{
			//必要属性（控制xml编译）
			"encoding":"utf-8",
			//必要属性（控制xml编译）
			"contentType":"text/html;charset=UTF-8"
		},
		"extensionMap":{
			////xhtml 编译不是自带的，需要自己定义
			//"http://www.w3.org/1999/xhtml":["org.xidea.lite.xhtml"],
			//core 自行编译
			//"http://firekylin.my.baidu.com/ns/2010":["org.xidea.lite.xhtml"],
			//"http://firekylin.my.baidu.com/ns/2010":["org.xidea.lite.xhtml"]
			
		}
	}

if(typeof require == 'function'){
exports.ParseConfig=ParseConfig;
var parseConfig = require('./config-parser').parseConfig;
var URI=require('./resource').URI;
}