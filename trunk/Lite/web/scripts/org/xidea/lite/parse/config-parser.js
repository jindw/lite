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
 *       <feature name="http://www.xidea.org/feature/lite/html-javascript-compressor"
 *               package="org.jside.jsi.tools.JSACompressor"/>
 *       <include>/example/*.xhtml</include>
 *    </group>
 * </lite>
 * ==>
 * [
 * 	{
 * 		"includes":"^[\\\\/]example[\\\\/][^\\\\/]*\.xhtml$",
 * 		"excludes":"",
 * 		"featureMap":{
 * 			"http://www.xidea.org/lite/features/layout":"/layout.xhtml",
 * 			"http://www.xidea.org/lite/features/output-encoding":"utf-8",
 * 			"http://www.xidea.org/lite/features/output-mime-type":"text/html",
 * 			"http://www.xidea.org/lite/features/html-javascript-compressor":"org.jside.jsi.tools.JSACompressor"
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
 * 		"featureMap":{
 * 			"http://www.xidea.org/lite/features/output-encoding":"utf-8",
 * 			"http://www.xidea.org/lite/features/output-mime-type":"text/html"
 * 		},
 * 		"extensionMap":{
 * 			"http://www.w3.org/1999/xhtml":["org.xidea.lite.xhtml"]
 * 		]
 * 	}
 * ]
 */
//function parseConfigToJSON(doc){
//	var result = stringifyJSON(parseConfig(doc));
//	return result;
//}
function parseConfig(doc){
	var doc = doc.nodeType?doc:loadXML(doc);
	var lites = doc.getElementsByTagName("lite");
	var len = lites.length;
	if(len >= 1){
		var root = new LiteGroup(lites.item(0))
		if(len>1){
			$log.error("配置文件只允许一个lite节点","您的文档中包含"+len+"个节点，后续节点将作为第一个节点子节点解析。");
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
	this.featureMap = {}
	this.encoding= getAttribute(node,'encoding','charset');
	this.mimeType = getAttribute(node,'mime-type','mimeType','mimiType','metaType');
	this.layout = getAttribute(node,'layout');
	this.contentType = getAttribute(node,'contentType','contextType');
	this.extensionMap = {};
	this.children = [];
	this.includes = [];
	this.excludes = [];
	var child = node.firstChild;
	while(child){
		if(child.nodeType == 1){
			switch(child.nodeName){
			case 'feature':
				this.featureMap[getAttribute(child,'name','key','uri','url')] = 
						getAttribute(child,'value','#text')
				break;
			case 'extension':
				var ns = getAttribute(child,'namespace','name','key','uri','url');
				var p = getAttribute(child,'package','impl','value','#text');
				var ps = this.extensionMap[ns];
				if(ps && ps instanceof Array){
					appendAfter(ps,p);
				}else{
					this.extensionMap[ns] = [p];
				}
				break;
			case 'include':
				this.includes.push(getAttribute(child,'value','#text','pattern'));
				break;
			case 'exclude':
				this.excludes.push(getAttribute(child,'value','#text','pattern'));
				break;
			case 'group':
				this.children.push(new LiteGroup(child,this))
				break;
			default:
				$log.warn("unknow nodeName:"+child.nodeName);
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
	json.featureMap = this.featureMap;
	json.extensionMap = this.extensionMap;
	result.push(json);
	return result;
}

LiteGroup.prototype.initialize = function(){
	this.initialize = Function.prototype;
	var parentConfig = this.parentConfig
	if(parentConfig){
		var featureMap = {};
		copy(parentConfig.featureMap,featureMap);
		copy(this.featureMap,featureMap);
		this.featureMap=featureMap;
		this.extensionMap = margeExtensionMap(parentConfig.extensionMap,this.extensionMap);
	}
	if(this.encoding == null){
		this.encoding = parentConfig && parentConfig.encoding;
	}
	if(this.mimeType == null){
		this.mimeType = parentConfig && parentConfig.mimeType;
	}
	this.includes = compilePatterns(this.includes)
	this.excludes = compilePatterns(this.excludes)
	var contentType = this.contentType;
	if(contentType!=null){
		$log.warn("ContentType属性不被推荐，请采用mimeType和encoding代替")
		var p = contentType.indexOf('charset=');
		var encoding = this.encoding;
		if(p>0){
			var charset = contentType.substring(p+8);
			if(encoding){
				if(charset.toUpperCase() != encoding.toUpperCase()){
					$log.error('encoding 与 contentType 不匹配'+encoding+','+contentType+'contentType 的设置将覆盖encoding 设置');
					this.encoding=charset;
				}
			}else{
				this.encoding = charset;
			}
			contentType =  contentType.substring(0,contentType.lastIndexOf(';'))
		}
		var mimeType = this.mimeType;
		if(mimeType){
			if(mimeType.toUpperCase() != contentType.toUpperCase()){
				$log.error('mimeType 与 contentType 不匹配'+mimeType+','+contentType+'contentType 的设置将覆盖mimeType 设置');
				this.mimeType=contentType;
			}
		}else{
			this.mimeType=contentType;
		}
	}
	this.featureMap["http://www.xidea.org/lite/features/output-encoding"] = this.encoding;
	this.featureMap["http://www.xidea.org/lite/features/output-mime-type"] = this.mimeType;
	if(this.layout != null){
		if(!this.layout || this.layout.charAt() == '/'){
			this.featureMap["http://www.xidea.org/lite/features/config-layout"] = this.layout;
		}else{
			$log.error("layout 必须为绝对地址('/'开始),你的设置为："+this.layout);
		}
		
	}
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

function buildURIMatcher(pattern){
	var matcher = /\*+|[^\*\\\/]+?|[\\\/]/g;
	var buf = ["^"];
	var m
	matcher.lastIndex = 0;
	while (m = matcher.exec(pattern)) {
		var item = m[0];
		var len = item.length;
		var c = item.charAt(0);
		if (c == '*') {
			if (len > 1) {
				buf.push(".*");
			} else {
				buf.push("[^\\\\/]+");
			}
		} else if(len == 1 && c == '/' || c == '\\') {
			buf.push("[\\\\/]");
		}else{
			buf.push(item.replace(/[^\w]/g,quteReqExp));
		}
	}
	buf.push("$");
	return buf.join('');
}
function quteReqExp(x){
	switch(x){
	case '.':
		return '\\.';
	case '\\':
		return '\\\\';
	default:
		return '\\x'+(0x100 + x.charCodeAt()).toString(16).substring(1);
	}
}
