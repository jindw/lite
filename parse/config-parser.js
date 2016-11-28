/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
/**
 * 模板解析上下文对象实现
 * <lite>
 *    <extension namespace="http://www.w3.org/1999/xhtml"
 *               package="org.xidea.lite.xhtml"/>
 * 	  <include>**.xhtml</include>
 *    <group layout="/layout.xhtml">
 *       <config name="javascriptCompressor"
 *               package="org.jside.jsi.tools.JSACompressor"/>
 *       <include>/example/*.xhtml</include>
 *    </group>
 * </lite>
 * ==>
 * [
 * 	{
 * 		"includes":"^[\\\\/]example[\\\\/][^\\\\/]*\.xhtml$",
 * 		"excludes":"",
 * 		"config":{
 * 			"layout":"/layout.xhtml",
 * 			"encoding":"utf-8",
 * 			"contentType":"text/html;charset=UTF-8",
 * 			"javascriptCompressor":"org.jside.jsi.tools.JSACompressor"
 * 		},
 * 		"extensionMap":[
 * 			{
 * 				"namespace":"http://www.w3.org/1999/xhtml",
 * 				"package":"org.xidea.lite.xhtml"
 * 			}
 * 		]
 * 	},
 * 	{
 * 		"includes":"^.*\.xhtml$",
 * 		"excludes":"",
 * 		"config":{
 * 			"encoding":"utf-8",
 * 			"contentType":"text/html;charset=UTF-8"
 * 		},
 * 		"extensionMap":{
 * 			"http://www.w3.org/1999/xhtml":["org.xidea.lite.xhtml"]
 * 		]
 * 	}
 * ]
 */

function parseConfig(doc){
	if(typeof doc == 'string'){doc = loadLiteXML(doc)}
	var lites = doc.getElementsByTagName("lite");
	var len = lites.length;
	//console.log(new (require('xmldom').XMLSerializer)().serializeToString(doc))
	if(len >= 1){
		var root = new LiteGroup(lites.item(0))
		if(len>1){
			console.error("配置文件只允许一个lite节点","您的文档中包含"+len+"个节点，后续节点将作为第一个节点子节点解析。");
			for(var i=1;i<len;i++){
				root.children.push(new LiteGroup(lites[i],this));
			}
		}
		var json = root.toJSON();
		return json;
		
	}
	return null
}
function LiteGroup(node,parentConfig){
	this.parentConfig = parentConfig || null
	this.config = {}
	this.encoding= findXMLAttribute(node,'encoding','charset');
	this.type = findXMLAttribute(node,'type',"mime-type",'mimeType');
	this.contentType = findXMLAttribute(node,'contentType','contextType');
	this.layout = findXMLAttribute(node,'layout');
	this.extensionMap = {};
	this.children = [];
	this.includes = [];
	this.excludes = [];
	var child = node.firstChild;
	while(child){
		if(child.nodeType == 1){
			switch(child.nodeName){
			case 'feature':
			case 'attribute':
			case 'config':
				this.config[findXMLAttribute(child,'name','key','uri','url')] = 
						findXMLAttribute(child,'value','#text')
				break;
			case 'extension':
				var ns = findXMLAttribute(child,'namespace','name','key','uri','url');
				var p = findXMLAttribute(child,'package','impl','value','#text');
				var ps = this.extensionMap[ns];
				if(ps && ps instanceof Array){
					appendAfter(ps,p);
				}else{
					this.extensionMap[ns] = [p];
				}
				break;
			case 'include':
				this.includes.push(findXMLAttribute(child,'value','#text','pattern'));
				break;
			case 'exclude':
				this.excludes.push(findXMLAttribute(child,'value','#text','pattern'));
				break;
			case 'group':
				this.children.push(new LiteGroup(child,this))
				break;
			default:
				console.warn("unknow nodeName:"+child.nodeName);
			}
		}
		child = child.nextSibling;
	}
}
LiteGroup.prototype.toJSON = function(){
	var result = [];
	var len = this.children.length;
	var json = {}
	this.initialize();
	for(var i=0;i<len;i++){
		result.push.apply(result,this.children[i].toJSON());
	}
	json.includes = this.includes;
	json.excludes = this.excludes;
	json.config = this.config;
	json.extensionMap = this.extensionMap;
	result.push(json);
	return result;
}

LiteGroup.prototype.initialize = function(){
	this.initialize = Function.prototype;
	var parentConfig = this.parentConfig
	if(parentConfig){
		var config = {};
		copy(parentConfig.config,config);
		copy(this.config,config);
		this.config=config;
		this.extensionMap = margeExtensionMap(parentConfig.extensionMap,this.extensionMap);
	}
	this.includes = compilePatterns(this.includes)
	this.excludes = compilePatterns(this.excludes)
	mergeContentType(this,parentConfig);
	this.config["encoding"] = this.encoding;
	this.config["contentType"] = this.contentType;
	if(this.layout != null){
		if(!this.layout || this.layout.charAt() == '/'){
			this.config["layout"] = this.layout;
		}else{
			console.error("layout 必须为绝对地址('/'开始),你的设置为："+this.layout);
		}
		
	}
}
function mergeContentType(thiz,parentConfig){
	var type=thiz.type;
	var encoding = thiz.encoding;
	var contentType = thiz.contentType;//不从parent继承
	/*========= init 3 vars==========*/
	if(contentType!=null){
		console.info("contentType 用于同时指定 type 和charset 属性，如此需求更推荐您采用type和encoding代替")
		var p = contentType.indexOf('charset=');
		if(p>0){
			var charset = contentType.substring(p+8);
			if(encoding){
				if(charset.toUpperCase() != encoding.toUpperCase()){
					console.info('encoding 与 contentType 不一致'+encoding+','+contentType
						+"; ");
				}
			}else{
				encoding = charset;
			}
		}
		var contentType0 = contentType.replace(/\s*;.*$/,'');
		if(type){
			if(type.toUpperCase() != contentType0.toUpperCase()){
				console.error('type 与 contentType 不一致'+type+','+contentType0
					+';type 设置将被忽略');
			}
		}
		type = contentType0
	}
	/*========== init from parent ==============*/
	if(encoding == null){
		encoding = parentConfig && parentConfig.encoding || 'UTF-8';
	}
	if(type == null){
		type = parentConfig && parentConfig.type;
	}
	if(contentType == null){//不继承
		if(type){
			contentType = type+";charset="+encoding;
		}
	}else{
		var p = contentType.indexOf('charset=');
		if(p<0){
			contentType +=";charset="+encoding;
		}
	}
	thiz.type = type;
	thiz.encoding = encoding;
	thiz.contentType = contentType;
}
function copy(source,dest){
	for(var n in source){
		dest[n] = source[n];
	}
}
function margeExtensionMap(parentExtMap,thisExtMap){
	var result = {};
	for(var n in thisExtMap){
		result[n] = [].concat(thisExtMap[n]);
	}
	for(var n in parentExtMap){
		var list = [].concat(parentExtMap[n]);
		var thisExt = result[n] ;
		if(thisExt){
			var i = thisExt.length;
			while(i--){
				appendAfter(list,thisExt[i]);
			}
		}
		result[n] = list;
	}
	return result;
}
function appendAfter(ps,p){
	var i = ps.length;
	while(i--){
		if(ps[i] == p){
			ps.splice(i,1)
		}
	}
	ps.push(p);
}
function compilePatterns(ps){
	var i = ps.length;
	while(i--){
		ps[i] = buildURIMatcher(ps[i]);
	}
	return ps.join('|')||null;
}


if(typeof require == 'function'){
exports.parseConfig=parseConfig;
var buildURIMatcher = require('./resource').buildURIMatcher
var loadLiteXML = require('./xml').loadLiteXML;
var findXMLAttribute=require('./xml').findXMLAttribute;
}