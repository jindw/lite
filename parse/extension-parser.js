/**
 * @see extension.js
 */
var CORE_URI = "http://www.xidea.org/lite/core"
var HTML_EXT_URI = "http://www.xidea.org/lite/html-ext"
var HTML_URI = "http://www.w3.org/1999/xhtml"
var currentExtension;
var defaultNodeLocal={
	get:function(){
		return this.node
	},
	set:function(n){
		this.node = n;
	}
}
var nodeLocal = defaultNodeLocal;

var Extension=require('./extension').Extension;
var HTML=require('./syntax-html').HTML;
var HTML_EXT=require('./syntax-html').HTML_EXT;
var getLiteTagInfo=require('./xml').getLiteTagInfo;
//初始化 Core 模块
var Core=require('./syntax-core').Core;
exports.ExtensionParser=ExtensionParser;
copyTo(require('./alive'),Core);
copyTo(require('./syntax-core-block'),Core);

copyTo(require('./syntax-i18n'),Core);
copyTo(require('./syntax-text'),Core);

function copyTo(from,to){
	for(var n in from){
		to[n]=from[n];
	}
}
function ExtensionParser(newNodeLocal){
	if(newNodeLocal){
		nodeLocal = newNodeLocal;
	}else{
		nodeLocal = defaultNodeLocal;
	}
	this.packageMap = {};
	this.addExtension(CORE_URI,Core);
	this.addExtension(HTML_URI,HTML)
	this.addExtension(HTML_EXT_URI,HTML_EXT)
	
}
function formatName(el){
	var tagName = el.localName|| el.nodeName ||''
	tagName = tagName.replace(/[\-]|^\w+\:/g,"");
	return tagName.toLowerCase();
}

function loadExtObject(source){
	try{
		var p = /\b(?:document|xmlns|(?:parse|intercept|seek|on)\w*)\b/g;
		var fn = new Function("console","var window = this;"+source+"\n return function(){return eval(arguments[0])}");
		var m,o;
		var objectMap = {};
	}catch(e){
		console.error("扩展源码语法错误:",e,source)
		throw e;
	}
	try{
		fn = fn(console);
	}catch(e){
		console.error("扩展脚本装载失败：",source,e);
	}
	while(m = p.exec(source)){
		try{
			o = fn(m[0]);
			if(o instanceof Function){
				objectMap[m[0]] = o;
			}
		}catch(e){
		}
	}
	return objectMap;
}


function findPatternParser(map,key){
	var buf = [];
	for(var n in map){
		if(new RegExp('^'+n.replace(/\*/g,'.*')+'$').test(key)){
			buf.push.apply(buf,map[n]);
		}
	}
	return buf.length ? buf:null;
}

function copyParserMap(mapClazz,p,p2,key){
	var map = p[key];
	if(map){
		var result = mapClazz.newInstance();
		p2.put(key ,result);
		for(var n in map){
			result.put(n, map[n]);
		}
	}
}
/**
 * 
	public boolean parseElement(Element el, ParseContext context,
			ParseChain chain, String name);
	public boolean parseDocument(Document node, ParseContext context,ParseChain chain);
	public boolean parseNamespace(Attr node, ParseContext context, ParseChain chain);
	public boolean parseAttribute(Attr attr, ParseContext context, ParseChain chain);
	public boolean parseIntercept(Attr attr, ParseContext context,
			ParseChain previousChain, String name);
 */
ExtensionParser.prototype = {
	parseElement:function(el, context,chain){
//		context.setAttribute(CURRENT_NODE_KEY,el)
		var ns = el.namespaceURI;
		var attrs = el.attributes;
		var len = attrs.length;
		try{
//			var es = 0;
			for (var i =  len- 1; i >= 0; i--) {
				var attr = attrs.item(i);
				var ans = attr.namespaceURI;
				if(ans){
					var ext = this.packageMap[ans];
					var an = formatName(attr);
	//				es = 2
					if (ext && ext.interceptMap) {
						var fns = an in ext.interceptMap?ext.interceptMap[an]: findPatternParser(ext.patternInterceptMap,an);
						if(fns){
	//						es = 2.1
							//el.removeAttributeNode(attr);
							//attr.ownerElement = el;
							//fn.call(chain,attr);
							doParse (attr,fns,chain,ns)
							//
	//						es =2.2
							return true;
						}
					}
				}
			}
//			es = 4;
		//}catch(e){
		//	console.error("元素扩展解析异常",e)
		//	throw e;
		}finally{
		}
		var ext = this.packageMap[ns||''];
		var n = formatName(el);
		if(ext && ext.tagMap){
			if(n in ext.tagMap){
				var fns = ext.tagMap[n];
				return doParse(el,fns,chain);
			}else if(fns = findPatternParser(ext.patternTagMap,n)){
				return doParse(el,fns,chain);
			}
		}
	},
	parse:function(node,context,chain){
		//try{
//			var es = 0;
			var type = node.nodeType;
//			var es = 1;
			if(type === 1){
//				var es = 1.1;
				var old = nodeLocal.get();
//				var es = 1.2;
				try{
					nodeLocal.set(node);
					if(this.parseElement(node,context,chain)){
						return;
					}
				}finally{
					nodeLocal.set(old);
				}
//				var es = 1.3;
			}else if(type === 2){//attribute
				if(this.parseAttribute(node,context,chain)){
					return;
				}
			} else{
				if(type == 9 || type == 8){//NODE_DOCUMENT,NODE_COMMENT
					for(var ns in this.packageMap){
						//objectMap.namespaceURI = namespace
						var p = this.packageMap[ns];
						if(p && p.typeMap){
							var fns = p.typeMap['$'+type];
							if(fns){
								return doParse(node,fns,chain,ns);
							}
						}
					}
				}
			}
//			var es = 10;
			chain.next(node)
		//}catch(e){
		//	console.error("扩展解析异常：",e);
		//}
	},
	parseAttribute:function(node,context,chain){
		if(this.parseNamespace(node,context,chain)){
			return true;
		}
		try{
//			var es = 3;
			var el = node.ownerElement || node.selectSingleNode("..");//ie bug
			//ie bug.no ownerElement
			var ns = node.namespaceURI || el && el.namespaceURI||'';
			var ext = this.packageMap[ns];
			var n = formatName(node);
//			var es=4;
			if(ext && ext.attributeMap){
				if(n in ext.attributeMap){
					return doParse(node,ext.attributeMap[n],chain);
				}else{
					var fns = findPatternParser(ext.patternAttributeMap,n);
					if(fns){
						return doParse(node,fns,chain);
					}
				}
			}
		}catch(e){
			console.error("属性扩展解析异常：",e)
		}
	},
	parseNamespace:function(attr,context,chain){
		try{
			var es = 0;
			if(/^xmlns(?:\:\w+)?/.test(attr.name)){
			
				var v = attr.value;
				var fp = this.packageMap[v||''];
				if(fp){
					if(fp.namespaceParser){
						fp.namespaceParser.call(chain,attr);
						return true;
					}
					
					var el = attr.ownerElement ||  attr.selectSingleNode("..");//ie bug
					var info = getLiteTagInfo(el);
					if(info && info.length ==0 || info.indexOf("|"+attr.name+"|")>0){
						return fp!=null;
					}else{
						return true;//自动补全的xmlns 不处理!
					}
				}
				//console.error(v,fp.namespaceParser);
			}
		}catch(e){
			console.error("名称空间解析异常：",es,e)
		}
		return false;
	},
	parseText:function(text,start,context){
		var text2 = text.substring(start+1);
		var match = text2.match(/^(?:(\w*)\:)?([\w!#]*)[\$\{]/);
		try{
//			var es = 0;
			if(match){
				var matchLength = match[0].length;
				var node = nodeLocal.get();
				var prefix = match[1];
				var fn = match[2]
				if(prefix == null){
					var ns = ""
				}else{
//					es = 1;
					if(node && node.lookupNamespaceURI){
						var ns = node.lookupNamespaceURI(prefix);
						if (ns == null) {
							var doc = node.ownerDocument;
							ns = doc && doc.documentElement.lookupNamespaceURI(prefix);
						}
					}
//					es =2
				}
				
				if(!ns && (prefix == 'c' || !prefix)){
					ns = CORE_URI
				}
				if(ns == null){
					console.warn("文本解析时,查找名称空间失败,请检查是否缺少XML名称空间申明：[code:$"+match[0]+",prefix:"+prefix+",document:"+context.currentURI+"]")
				}else{
					var fp = this.packageMap[ns];
					if(fp){
						//{开始的位置，el内容
						var text3 = text2.substring(matchLength-1);
						var seekMap = fp.seekMap;
						if(fn in seekMap){
							fn = seekMap[fn];
							var rtv = fn.call(context,text3);
							if(rtv>0 || rtv === 0){
								return start+matchLength+rtv+1
							}
						}else{
							console.warn("文本解析时,找不到相关的解析函数,请检查模板源码,是否手误：[function:"+fn+",document:"+(context && context.currentURI)+"]")
							//return -1;
						}
					}else{
						console.warn("文本解析时,名称空间未注册实现程序,请检查lite.xml是否缺少语言扩展定义：[code:$"+match[0]+",namespace:"+ns+",prefix:"+prefix+",document:"+context.currentURI+"]")
					}
				}
			}
		}catch(e){
			console.error("文本解析异常：",e)
		}
		//seek
		return -1;
	},
	/**
	 * 查找EL或者模板指令的开始位置
	 * @param text
	 * @param start 开始查询的位置
	 * @param otherStart 其他的指令解析器找到的指令开始位置（以后必须出现在更前面，否则无效）
	 * @return 返回EL起始位置($位置)
	 */
	findStart:function(text,start,otherStart){
		var begin = start;
		while(true){
			begin = text.indexOf('$',begin);
			if(begin<0 || otherStart <= begin){
				return -1;
			}
			var text2 = text.substring(begin+1);
			var match = text2.match(/^(?:\w*\:)?[\w#!]*[\$\{]/);
			if(match){
				return begin;
			}
			begin++;
		}
	},
	addExtension:function(namespace,packageName){
		if(typeof packageName == 'string'){
			if(/^[\w\.\/]+$/.test(packageName)){
				var objectMap = {};
				var packageObject = require(packageName);
				for(var n in packageObject){
					if(n.match(/^(?:document|xmlns|on|parse|intercept|seek).*/)){
						objectMap[n] = packageObject[n];
					}
				}
			}else{
				objectMap = loadExtObject(packageName)
			}
		}else{
			objectMap = packageName;
		}
		var ext = this.packageMap[namespace||''];
		if(ext == null){
			ext = this.packageMap[namespace||''] = new Extension();
		}
		ext.initialize(objectMap,namespace||'');
	},
	getPriority:function() {
		//${ =>2
		//$!{ =>3
		//$end$ =>5
		return 2;
	}
}


function doParse (node,fns,chain){//,ns){
	var last = fns.length-1;
	if(last>0){
		var subIndex = chain.subIndex;
		if(subIndex <0){
			subIndex = last;
			chain = chain.getSubChain(last);
		}
//			console.info("##",subIndex,String(fns[subIndex]));
		fns[subIndex].call(chain,node)//,ns);
	}else{
		//if(node.name == 'onclick'){
		//	console.error(node.name,typeof fns[0],fns[0].call,fns[0])
		//}
		fns[0].call(chain,node);//,ns);
	}
	return true;
}