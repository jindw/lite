/**
 * @see extension.js
 */

function ExtensionParser(){
	this.packageMap = {};
	this.addExtensionObject("http://www.xidea.org/ns/lite/core",Core);
	this.addExtensionObject("",defaultTextSeeker);
	
}

function formatName(tagName){
	tagName = tagName.replace(/[\-]/g,"");
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
var CURRENT_NODE_KEY = {}
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
		for(var p in this.packageMap){
			p = this.packageMap[p];
			if(p.documentParser){
				p.documentParser.apply(chain,arguments);
				return true;
			}
		}
		return false;
	},
	parseNamespace:function(attr,context,chain){
		if(/^xmlns(?:\:\w+)?/.test(attr.name)){
			var v = attr.value;
			var fp = this.packageMap[v||''];
			if(fp && fp.namespaceParser){
				fp.namespaceParser.call(chain,attr,context,chain);
				return true;
			}
		}
		return false;
	},
	parseElement:function(el, context,chain,ns, name){
		context.setAttribute(CURRENT_NODE_KEY,el)
		var nns = el.namespaceURI;
		var attrs = el.attributes;
		var len = attrs.length;
		var exclusiveMap = {};
		for (var i =  len- 1; i >= 0; i--) {
			var attr = attrs.item(i);
			var ans = attr.namespaceURI;
			var ext = this.packageMap[ans || ''];
			var an = formatName(attr.name);
			if (ext && ext.beforeMap) {
				var fn = ext.beforeMap[an];
				if(fn && an in ext.beforeMap){
					el.removeAttributeNode(attr);
					if(fn.call(chain,attr,context,chain)){
						return;
					}else{
						el.setAttributeNode(attr);
					}
				}else{
					an+='$';
					if(an in ext.beforeMap){
						exclusiveMap[an] = attr;
					}
				}
			}
		}
		for(an in exclusiveMap){
			var attr = exclusiveMap[an];
			var ans = attr.namespaceURI;
			var ext = this.packageMap[ans || ''];
			el.removeAttributeNode(attr);
			if(ext.beforeMap[an].call(chain,attr,context,chain)){
				return;
			}else{
				el.setAttributeNode(attr);
			}
		}
		var ext = this.packageMap[nns||''];
		var nn = formatName(el.localName||el.nodeName);
		if(ext && ext.parserMap){
			var fn = ext.parserMap[nn];
			if(fn && (nn in ext.parserMap)
				 || (fn = ext.parserMap[''])){
				fn.call(chain,el,context,chain);
				return true;
			}
		}
	},
	parse:function(node,context,chain){
		var type = node.nodeType;
		if(type == 9){
			if(this.parseDocument(node,context,chain)){
				return ;
			}
		}else if(type == 2){
			if(node.namespaceURI == 'http://www.w3.org/2000/xmlns/'){
				if(this.parseNamespace(node,context,chain)){
					return;
				}
			}
			var el = node.ownerElement;
			var ns = el.namespaceURI||'';
			var ext = this.packageMap[ns];
			if(ext && ext.onMap){
				if(fn in ext.onMap){
					var fn = ext.onMap[fn];
					fn.call(chain,attr,context,chain);
					return true;
				}
			}
		}else if(type === 1){
			if(this.parseElement(node,context,chain)){
				return ;
			}
		}
		chain.next(node)
	},
	parseText:function(text,start,context){
		var text2 = text.substring(start+1);
		var match = text2.match(/^(?:(\w*)\:)?([\w!#]*)[\$\{]/);
		if(match){
			var matchLength = match[0].length;
			var currentNode = context.getAttribute(CURRENT_NODE_KEY)
			var prefix = match[1];
			var fn = match[2]
			if(prefix == null){
				var ns = ""
			}else{
				if(currentNode){
					var ns = currentNode.lookupNamespaceURI(prefix);
					if (ns == null) {
						var doc = currentNode.getOwnerDocument();
						ns = doc && doc.documentElement.lookupNamespaceURI(prefix);
					}
				}
				if(!ns && prefix == 'c'){
					ns = "http://www.xidea.org/ns/lite/core"
				}
			}
			if(ns == null){
				$log.warn("文本解析时,查找名称空间失败,请检查是否缺少XML名称空间申明：[code:$"+match[0]+",prefix:"+prefix+",document:"+context.currentURI+"]")
			}else{
				var fp = this.packageMap[ns||''];
				if(fp){
					//{之后的位置，el内容
					var text3 = text2.substring(matchLength);
					var p = fp.seek(text3,fn,context);
					if(p>=0){
						return start+1+matchLength+p
					}
				}else{
					$log.warn("文本解析时,名称空间未注册实现程序,请检查lite.xml是否缺少语言扩展定义：[code:$"+match[0]+",namespace:"+ns+",document:"+context.currentURI+"]")
				}
			}
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
			var match = text2.match(/^(?:\w*\:)?\w*[\$\{]/);
			if(match){
				return begin;
			}
			begin++;
		}
	},
	addExtensionPackage:function(namespace,packageName){
		var target = {};
		if(typeof packageName == 'string'){
			var packageObject = $import(packageName+':');
		}else{
			packageObject = packageName;
		}
		for(var n in packageObject.objectScriptMap){
			var match = n.match(/^(?:document|xmlns|on|parse|before|seek).*/);
			if(match){
				var o = $import(packageObject.name+':'+n,target);
			}
		}
		this.addExtensionObject(namespace,target);
	},
	addExtensionObject:function(namespace,objectMap){
		var ext = this.packageMap[namespace||''];
		if(ext == null){
			ext = this.packageMap[namespace||''] = new Extension();
		}
		ext.initialize(objectMap);
	},
	priority:2,
	getPriority:function() {
		//${ =>2
		//$!{ =>3
		//$end$ =>5
		return this.priority;
	}
}