/**
 * @see extension.js
 */

function ExtensionParser(node){
	this.packageMap = {};
	this._textParser = [this];
	var coreURL = "http://www.xidea.org/ns/lite/core";
	this.addExtensionObject(coreURL,Core);
	this.addExtensionObject("",DEFAULT_EL);
	
}
var DEFAULT_EL = {
	seek:function(text2){
		var end = findELEnd(text2,-1);
		$log.info(text2,end)
		if(end>0){
			try{
				var el = text2.substring(0,end);
				el = this.parseEL(el);
			}catch(e){
				$log.error("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
				return -1;
			}
            switch(this.getTextType()){
            case XML_TEXT_TYPE:
            	this.appendXmlText(el);
            	break;
            case XML_ATTRIBUTE_TYPE:
            	this.appendAttribute(null,el);
            	break;
            default:
            	this.appendEL(el);
            }
            return end+1;
		}else{
			return -1;
		}
	}
}
var CURRENT_NODE_KEY = {}

ExtensionParser.prototype = {
	parse:function(node,context,chain){
		var type = node.nodeType;
		if(type == 9){
			//documentParser return 
		}else if(type == 2){
			if(node.namespaceURI == 'http://www.w3.org/2000/xmlns/'){
				if(/^xmlns(?:\:\w+)?/.test(node.name)){
					var v = node.value;
					var fp = this.packageMap[v];
					if (fp != null && fp.xmlns) {
						fp.xmlns(node);
						return;
					}
				}
			}
		}else if(type === 1){
			context.setAttribute(CURRENT_NODE_KEY,node)
			var nns = node.namespaceURI;
			var attrs = node.attributes;
			var len = attrs.getLength();
			var exclusives = [];
			for (var i =  - 1; i >= 0; i--) {
				var attr = attrs.item(i);
				var ans = attr.namespaceURI;
				var fp = this.packageMap[ans || nns];
				if (fp != null) {
					var test = fp.on(attr, context, chain.previousChain,false);
					if(test){
						if(test == true){
							return;
						}else{
							exclusives.push([fp,attr]);
						}
					}
				}
			}
			if(attr = exclusives.pop()){
				if(fp[0].on(attr[1], context, chain.previousChain,true)) {
					return;
				}
			}
			var fp = this.packageMap[nns];
			if(fp && fp.parse(node,context,chain)){
				return;
			}
			parseDefaultXMLNode(node,context,chain);
			return;
		}else if(type == null){//textParser
			//String text,int start,ParseContext context
			parseText(node,context,chain,this._textParser);
			return;
		}
		parseDefaultXMLNode(node,context,chain);
	},
	parseText:function(text,start,context){
		var text2 = text.substring(start+1);
		var match = text2.match(/^(?:(\w*)\:)?(\w*)[\$\{]/);
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
				var fp = this.packageMap[ns];
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
			var match = n.match(/^(?:document|on|parse|seek|xmlns).*/);
			if(match){
				var o = $import(packageObject.name+':'+n,target);
			}
		}
		this.addExtensionObject(namespace,target);
	},
	addExtensionObject:function(namespace,objectMap){
		var ext = this.packageMap[namespace];
		if(ext == null){
			ext = this.packageMap[namespace] = new Extension();
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