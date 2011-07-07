/**
 * @see extension.js
 */
var CORE_URI = "http://www.xidea.org/lite/core"
var CORE_INFO = "__i";
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
function ExtensionParser(newNodeLocal){
	if(newNodeLocal){
		nodeLocal = newNodeLocal;
	}else{
		nodeLocal = defaultNodeLocal;
	}
	this.packageMap = {};
	this.addExtension(CORE_URI,Core);
	
}
//Extension.defaults = [];
//Extension.addDefault = function(ns,object){
//	this.defaults.push([ns,object])
//}
function formatName(el){
	var tagName = el.localName|| el.nodeName ||''
	tagName = tagName.replace(/[\-]|^\w+\:/g,"");
	return tagName.toLowerCase();
}

function copyParserMap(mapClazz,p,p2,key){
	var map = p[key];
	if(map){
		var result = mapClazz.newInstance();
		p2[key] = result;
		for(var n in map){
			result[n]= map[n];
		}
	}
}
$log.filters.push(function(msg){
	if(nodeLocal){
		var currentNode = nodeLocal.get();
		if(currentNode){
			var p = getNodePosition(currentNode);
			if(p){
				msg = p+'\n'+msg;
			}
		}
	}
	return msg;
});
function getNodePosition(node){
	switch(node.nodeType){
	case 1://Node.ELEMENT_NODE:
		var el = node;
		break;
	case 2://Node.ATTRIBUTE_NODE:
		el =node.ownerElement;
		break;
	case 9://Node.DOCUMENT_NODE:
		el = node.documentElement;
	}
	if (el != null) {
		var doc = el.ownerDocument;
		var pos = el.getAttributeNS(CORE_URI, CORE_INFO);
		if(pos){
			var p = pos.indexOf('|');
			if(p>0){
				pos = pos.substring(0,p);
			}
			pos = "@"+node.nodeName+"["+pos+"]";
		}else{
			pos = "@"+node.nodeName;
		}
		if(doc!=null){
			var path = doc.documentURI;
			return path+pos;
		}else{
			return pos;
		}
		
	}
	return null;
}
function loadExtObject(source){
	try{
		var p = /\b(?:document|xmlns|(?:on|parse|before|seek)\w*)\b/g;
		var fn = new Function(source+"\n return function(){return eval(arguments[0])}");
		var m,o;
		var objectMap = {};
	}catch(e){
		$log.error("扩展源码语法错误:",e,source)
		throw e;
	}
	try{
		fn = fn();
	}catch(e){
		$log.error("扩展脚本装载失败：",source,e);
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

/**
 * 
	public boolean parseElement(Element el, ParseContext context,
			ParseChain chain, String name);
	public boolean parseDocument(Document node, ParseContext context,ParseChain chain);
	public boolean parseNamespace(Attr node, ParseContext context, ParseChain chain);
	public boolean parseAttribute(Attr attr, ParseContext context, ParseChain chain);
	public boolean parseBefore(Attr attr, ParseContext context,
			ParseChain previousChain, String name);
 */
ExtensionParser.prototype = {
	mapJava:function(mapClazz){
		var result = mapClazz.newInstance();
		for(var n in this.packageMap){
			var p = this.packageMap[n];
			var p2 = mapClazz.newInstance();
			result[n]=p2;
			p.documentParser && (p2.documentParser = p.documentParser);
			p.namespaceParser && (p2.namespaceParser = p.namespaceParser);
			copyParserMap(mapClazz,p,p2,"beforeMap")
			copyParserMap(mapClazz,p,p2,"onMap")
			copyParserMap(mapClazz,p,p2,"parserMap")
			copyParserMap(mapClazz,p,p2,"seekMap")
		}
		return result
	},
	parseDocument:function(node,context,chain){
		var ce = currentExtension;
		currentExtension = this;
		try{
			for(var ns in this.packageMap){
				//objectMap.namespaceURI = namespace
				var p = this.packageMap[ns];
				if(p.documentParser){
					return p.documentParser.call(chain,node,ns);
				}
			}
			return false;
		}finally{
			currentExtension = ce;
		}
	},
	parseNamespace:function(attr,context,chain){
		if(/^xmlns(?:\:\w+)?/.test(attr.name)){
			var v = attr.value;
			var fp = this.packageMap[v||''];
			if(fp && fp.namespaceParser){
				fp.namespaceParser.call(chain,attr);
				return true;
			}
			//$log.error(v,fp.namespaceParser);
		}
		return false;
	},
	parseComment:function(comm, context,chain){
		return true;
	},
	parseElement:function(el, context,chain,ns, name){
//		context.setAttribute(CURRENT_NODE_KEY,el)
		var nns = el.namespaceURI;
		var attrs = el.attributes;
		var len = attrs.length;
		var exclusiveMap = {};
		try{
//			var es = 0;
			for (var i =  len- 1; i >= 0; i--) {
				var attr = attrs.item(i);
				var ans = attr.namespaceURI;
				var ext = this.packageMap[ans || ''];
				var an = formatName(attr);
//				es = 2
				if (ext && ext.beforeMap) {
					var fn = ext.beforeMap[an];
					if(fn && an in ext.beforeMap){
//						es = 2.1
						//el.removeAttributeNode(attr);
						fn.call(chain,attr);
//						es =2.2
						return;
					}
				}
			}
//			es = 4;
		}catch(e){
			$log.error("元素扩展解析异常",e)
			throw e;
		}finally{
			
		}
		var ext = this.packageMap[nns||''];
		var nn = formatName(el);
		if(ext && ext.parserMap){
			var fn = ext.parserMap[nn];
			if(fn && (nn in ext.parserMap)
				 || (fn = ext.parserMap[''])){
				fn.call(chain,el);
				return true;
			}else if(nns && nns != 'http://www.w3.org/1999/xhtml'){
				$log.error("未支持标签：",el.tagName,context.currentURI)
			}
		}
	},
	parse:function(node,context,chain){
		try{
//			var es = 0;
			var type = node.nodeType;
//			var es = 1;
			if(type === 1){
//				var es = 1.1;
				var old = nodeLocal.get();
				nodeLocal.set(node);
//				var es = 1.2;
				try{
					if(!this.parseElement(node,context,chain)){
						chain.next(node);
					}
				}finally{
					nodeLocal.set(old);
				}
//				var es = 1.3;
			} else if(type === 9){
//				var es = 9.1;
				if(!this.parseDocument(node,context,chain)){
//				var es = 9.2;
					chain.next(node);
				}
			}else if(type === 2){//attribute
				try{
					if(this.parseNamespace(node,context,chain)){
						return;
					}
//					var es = 3;
					var el = node.ownerElement;
					//ie bug.no ownerElement
					var ns = node.namespaceURI || el && el.namespaceURI||'';
					var ext = this.packageMap[ns];
					var n = formatName(node);
					if(n == '__i' && ns == CORE_URI){
						return true;
					}
//					var es=4;
					if(ext && ext.onMap){
						if(fn in ext.onMap){
							var fn = ext.onMap[n];
							fn.call(chain,node);
							return true;
						}
					}
				}catch(e){
					$log.error("属性扩展解析异常：",e)
				}
				chain.next(node)
			}else{
//				var es = 10;
				chain.next(node)
			}
		}catch(e){
			$log.error("扩展解析异常：",e)
		}
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
							var doc = node.getOwnerDocument();
							ns = doc && doc.documentElement.lookupNamespaceURI(prefix);
						}
					}
//					es =2
				}
				
				if(!ns && (prefix == 'c' || !prefix)){
					ns = CORE_URI
				}
				if(ns == null){
					$log.warn("文本解析时,查找名称空间失败,请检查是否缺少XML名称空间申明：[code:$"+match[0]+",prefix:"+prefix+",document:"+context.currentURI+"]")
				}else{
					var fp = this.packageMap[ns||''];
					if(fp){
						//{开始的位置，el内容
						var text3 = text2.substring(matchLength-1);
						var p = fp.seek(text3,fn,context);
						if(p>=0){
							return start+matchLength+p+1
						}
					}else{
						$log.warn("文本解析时,名称空间未注册实现程序,请检查lite.xml是否缺少语言扩展定义：[code:$"+match[0]+",namespace:"+ns+",prefix:"+prefix+",document:"+context.currentURI+"]")
					}
				}
			}
		}catch(e){
			$log.error("文本解析异常：",e)
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
			if(/^[\w\.]+$/.test(packageName)){
				var objectMap = {};
				var packageObject = $import(packageName+':');
				for(var n in packageObject.objectScriptMap){
					var match = n.match(/^(?:document|xmlns|on|parse|before|seek).*/);
					if(match){
						var o = $import(packageObject.name+':'+n,objectMap);
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
	priority:2,
	getPriority:function() {
		//${ =>2
		//$!{ =>3
		//$end$ =>5
		return this.priority;
	}
}