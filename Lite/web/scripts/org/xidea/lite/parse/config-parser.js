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
 *       <featrue name="http://www.xidea.org/featrue/lite/html-javascript-compressor"
 *               package="org.jside.jsi.tools.JSACompressor"/>
 *       <include>/example/*.xhtml</include>
 *    </group>
 * </lite>
 * ==>
 * [
 * 	{
 * 		"includes":["^[\\\\/]example[\\\\/][^\\\\/]*\.xhtml$"],
 * 		"excludes":[],
 * 		"featrueMap":{
 * 			"http://www.xidea.org/featrues/lite/layout":"/layout.xhtml",
 * 			"http://www.xidea.org/featrues/lite/output-encoding":"utf-8",
 * 			"http://www.xidea.org/featrues/lite/output-mime-type":"text/html",
 * 			"http://www.xidea.org/featrue/lite/html-javascript-compressor":"org.jside.jsi.tools.JSACompressor"
 * 		},
 * 		"extensions":[
 * 			{
 * 				"namespace":"http://www.w3.org/1999/xhtml",
 * 				"package":"org.xidea.lite.xhtml"
 * 			}
 * 		]
 * 	},
 * 	{
 * 		"includes":["^.*\.xhtml$"],
 * 		"excludes":[],
 * 		"featrueMap":{
 * 			"http://www.xidea.org/featrues/lite/output-encoding":"utf-8",
 * 			"http://www.xidea.org/featrues/lite/output-mime-type":"text/html"
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
function parseConfig(doc){
	var doc = loadXML(doc);
	var lite = doc.getElementsByTagName("lite");
	if(lite == 1){
		lite = new LiteGroup(lite[0])
		return lite.toJSON();
	}else{
		$log.error("配置文件只允许一个lite节点","您的文档中包含"+lite.length+"个节点");
	}
	
}
function LiteGroup(dom,parentConfig){
	this.parentConfig = parentConfig || null
	this.featrueMap = {}
	this.encoding= getAttribute(node,'encoding','charset');
	this.mimeType = getAttribute(node,'mimeType','mimiType','metaType');
	this.layout = getAttribute(node,'layout');
	this.contentType = getAttribute(node,'contentType','contextType');
	this.extensions = [];
	this.children = [];
	this.includes = [];
	this.excludes = [];
	var child = dom.firstChild;
	while(child){
		if(child.nodeType == 1){
			switch(child.localName){
			case 'featrue':
				this.featrueMap[getAttribute(node,'name','key','uri','url')] = 
						getAttribute(node,'value','#text')
				break;
			case 'extension':
				this.extensions.push({
					"namespace":getAttribute(node,'namespace','name','key','uri','url'),
					"package":getAttribute(node,'package','impl','value','#text')
				});
				break;
			case 'include':
				this.includes.push(getAttribute(node,'value','#text','pattern'));
				break;
			case 'exclude':
				this.excludes.push(getAttribute(node,'value','#text','pattern'));
				break;
			case 'group':
				this.children.push(new LiteGroup(child,this))
				break;
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
		result.push.apply(this.children[i].toJSON());
	}
	json.includes = this.includes;
	json.excludes = this.excludes;
	json.featrueMap = this.featrueMap;
	json.extensions = this.extensions;
	result.push(thiz);
	return result;
}

LiteGroup.prototype.initialize = function(){
	this.initialize = Function.prototype;
	var parentConfig = this.parentConfig
	if(parentConfig){
		var featrueMap = {};
		copy(parentConfig.featrueMap,featrueMap);
		copy(this.featrueMap,featrueMap);
		this.featrueMap=featrueMap;
		this.extensions = [].concat(parentConfig.extensions,this.extensions);
	}
	if(this.encoding == null){
		this.encoding = parentConfig && parentConfig.encoding;
	}
	if(this.mimeType == null){
		this.mimeType = parentConfig && parentConfig.mimeType;
	}
	compilePatterns(this.includes)
	compilePatterns(this.excludes)
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
	this.featrueMap["http://www.xidea.org/featrues/lite/output-encoding"] = this.encoding;
	this.featrueMap["http://www.xidea.org/featrues/lite/output-mime-type"] = this.mimeType;
	if(this.layout != null){
		if(!this.layout || this.layout.charAt() == '/'){
			this.featrueMap["http://www.xidea.org/featrues/lite/layout"] = this.layout;
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
function compilePatterns(ps){
	var i = ps.length;
	while(i--){
		ps[i] = buildURIMatcher(ps[i]);
	}
}

function buildURIMatcher(pattern){
	var matcher = /\*+|[^\*\\\/]+?|[\\\/]/;
	var buf = ["^"];
	pattern.lastIndex = 0;
	while (matcher.exec(pattern)) {
		var item = matcher[0];
		var len = item.length;
		var c = item.charAt(0);
		if (c == '*') {
			if (length > 1) {
				buf.push(".*");
			} else {
				buf.push("[^\\\\/]+");
			}
		} else if(length == 1 && c == '/' || c == '\\') {
			buf.push("[\\\\/]");
		}else{
			buf.push(item.replace(/[^w]/g,quteReqExp));
		}
	}
	buf.push("$");
	return buf.join('');
}
function quteReqExp(x){
	return '\\x'+(0x100 + x.charCodeAt()).toString(16).substring(1);
}
    