/**
 * @see extension.js
 */

function ExtensionParser(node){
	this.packageMap = {};
	
}
var CURRENT_NODE_KEY = {}
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
				var localName = attr.localName || attr.name;
				var ans = attr.namespaceURI;
				var fp = this.packageMap[ans || nns];
				if (fp != null) {
					if(fp.isExclusive(attr)){
						exclusives.push([fp,attr]);
					}else if (fp.on(attr, context, chain.previousChain)) {
						return;
					}
				}
			}
			if(attr = exclusives.pop()){
				if (fp[0].on(attr[1], context, chain.previousChain)) {
					return;
				}
			}
			var fp = this.packageMap[nns];
			if(fp && fp.parse(node,context,chain)){
				return;
			}
			chain.process(node);
		}else if(type == null && typeof node == 'string'){//textParser
			//String text,int start,ParseContext context
			var text = arguments[0];
			var start = arguments[1];
			var context = arguments[2];
			
			var text2 = text.substring(start+1);
			var match = text2.match(/^(?:(\w*)\:)?(\w+)[\$\{]/);
			if(match){
				var node = context.getAttribute(CURRENT_NODE_KEY)
				var prefix = match[1];
				var fn = match[2]
				if(prefix === null){
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
						var matchLength = match[0].length;
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
		}
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
			var match = text2.match(/^(?:\w*\:)?\w+[\$\{]/);
			if(match){
				return begin;
			}
			begin++;
		}
	},
	getPriority:function() {
		//${ =>2
		//$!{ =>3
		//$end$ =>5
		return 2;
	}
}