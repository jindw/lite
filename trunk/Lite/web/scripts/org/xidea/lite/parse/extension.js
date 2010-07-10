/**
 * Lite JS 扩展规范：
 * 一个js包中：
 *            函数 document 为通用文档解释器  
 *            函数 on 为当前名称空间属性解释器,未空则该属性不输出
 *            所有 on<Attribute> 为当前名称空间属性解析器和当前名称空间节点的普通属性解释器（无名称空间属性）
 *            所有 parse<TagName> 为但前名称空间标记解释起
 *            所有 seek<Function Name> 为当前名称空间前缀的文本函数解释器
 */
function Extension(){
	this.documentParser = null;
	this.onMap = null;
	this.parserMap = null;
	this.seekMap = null;
}
function formatName(tagName){
	tagName = tagName.replace(/([A-Z])/g,"-$1");
	if(tagName.charAt(0) == '-'){
		tagName = tagName.substr(1);
	}
	return tagName;
}
Extension.prototype={
	seek:function(text,fn,context){
		if(fn in this.seekMap){
			fn = this.seekMap[fn];
			return fn.call(chain,text);
		}
		return -1;
	},
	parse:function(node,context,chain){
		if(this.parserMap){
			var n = formatName(node.tagName) ;
			for(n in this.parserMap){
				var fn = this.parserMap[n];
				break;
			}
			fn = fn || this.parserMap[''];
			if(fn){
				fn.call(chain,fn,node,context,chain);
				return true;
			}
		}
		return false;
	},
	on:function(node,context,chain){
		if(this.onMap){
			var n = formatName(node.name) ;
			if(n in this.onMap){
				var fn = this.onMap[n];
				fn.call(chain,fn,node,context,chain);
				return true;
			}
		}
		return false;
	},
	initialize:function(objectMap){
		for(var key in objectMap){
			var o = objectMap[key];
			if(o instanceof Function){
				var dest = null;
				var match = key.match(/^(document|on|parse|seek)(.*)/);
				var prefix = match[1];
				var fn = match[2];
				if(prefix == "document"){//""?".."
					documentParser = o;
					continue;
				}else if(prefix == "on"){
					dest = this.onMap ||(this.onMap={});
				}else if(prefix == "parse"){//""?".."
					dest = this.parserMap ||(this.parserMap={});
				}else if(prefix == "seek"){//""?".."
					dest = this.seekMap ||(this.seekMap={});
				}
				if(dest){
					dest[formatName(fn)] = o;
				}
			}
		}
	}
}
