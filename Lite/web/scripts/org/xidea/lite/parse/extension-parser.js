/**
 * @see extension.js
 */

function ExtensionParser(node){
	this.packageMap = {};
	this._textParser = [this];
	var coreURL = "http://www.xidea.org/ns/lite/core";
	this.addExtensionObject(coreURL,Core);
	
}
var CURRENT_NODE_KEY = {}
var ATTRIBUTE_PARSED = "org.xidea.lite.parse#parsed"
function canParseByDom(context,attr){
	if(attr.setUserData){
		var parsed = attr.getUserData(ATTRIBUTE_PARSED)
		if(parsed){
			return false;
		}else{
			attr.setUserData(ATTRIBUTE_PARSED,true);
			return true;
		}
	}
	return null;
}
function canParseByMap(context,attr,clean){
	var map = context.getAttribute(ATTRIBUTE_PARSED);
	if(!map){
		map = [[],[]];
		context.setAttribute(CURRENT_NODE_KEY,map)
	}
	if(clean){
		removeByKey(attr);
	}else{
		var parsed = getByKey(attr);
		if(parsed){
			return false;
		}else{
			setByKey(attr,true);
			return true;
		}
	}
}
ExtensionParser.prototype = {
	parse:function(node,context,chain){
		var type = node.nodeType;
		if(type === 1){
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
					var needParse = canParseByDom(context,attr);
					var needClean = needParse !== null;
					if(needParse || needClean && canParseByMap(context,attr)){
						if(fp.isExclusive(attr)){
							exclusives.push([fp,attr]);
						}else if (fp.on(attr, context, chain.previousChain)) {
							needClean && canParseByMap(context,attr,true);
							return;
						}
					}
				}
			}
			if(attr = exclusives.pop()){
				if (fp[0].on(attr[1], context, chain.previousChain)) {
					needClean && canParseByMap(context,attr,true);
					return;
				}
			}
			var fp = this.packageMap[nns];
			if(fp && fp.parse(node,context,chain)){
				return;
			}
			parseDefaultXML(node,context,chain);
		}else if(type == null){//textParser
			//String text,int start,ParseContext context
			parseText(node,context,chain,this._textParser);
			return;
		}else{
			parseDefaultXML(node,context,chain);
		}
	},
	parseText:function(text,start,context){
		var text2 = text.substring(start+1);
		var match = text2.match(/^(?:(\w*)\:)?(\w*)[\$\{]/);
		if(match){
			var matchLength = match[0].length;
			var node = context.getAttribute(CURRENT_NODE_KEY)
			var prefix = match[1];
			var fn = match[2]
			if(prefix === null){
				var end = findELEnd(text2,matchLength);
				if(end>0){
					var el = context.parseEL(text2.substring(matchLength,end));
                    switch(context.textType){
                    case XML_TEXT_TYPE:
                    	context.appendXmlText(el);
                    	break;
                    case XML_ATTRIBUTE_TYPE:
                    	context.appendAttribute(null,el);
                    	break;
                    default:
                    	context.appendEL(el);
                    }
				}else{
					return -1;
				}
				var ns = ""
			}else{
				var ns = node.lookupNamespaceURI(prefix);
				if (ns == null) {
					var doc = node.getOwnerDocument();
					ns = doc && doc.documentElement.lookupNamespaceURI(prefix);
				}
			}
			if(ns == null){
				$log.warn("文本解析时,查找名称空间失败,请检查是否缺少XML名称空间申明：[code:$"+match[0]+",document:"+node.currentURI+"]")
			}else{
				var fp = this.packageMap[nns];
				if(fp){
					//{之后的位置，el内容
					var text3 = text2.substring(matchLength);
					var p = fp.seek(text3,context);
					if(p>=0){
						return start+1+matchLength+p
					}else{
						$log.warn("文本解析时,找不到相关的解析函数,请检查模板源码,是否手误：[code:$"+match[0]+",namespace:"+ns+",document:"+node.currentURI+"]")
					}
				}else{
					$log.warn("文本解析时,名称空间未注册实现程序,请检查lite.xml是否缺少语言扩展定义：[code:$"+match[0]+",namespace:"+ns+",document:"+node.currentURI+"]")
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
			var packageOject = $import(packageName+':');
		}else{
			packageOject = packageName;
		}
		for(var n in pkg.objectScriptMap){
			var match = n.match(/^(?:document|on|parse|seek).*/);
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