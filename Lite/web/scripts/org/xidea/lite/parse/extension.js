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
Extension.prototype={
	
	initialize:function(objectMap){
		for(var key in objectMap){
			var o = objectMap[key];
			if(o instanceof Function){
				var dest = null;
				var match = key.match(/^(document|on|parse|seek|xmlns)(.*)/);
				var prefix = match[1];
				var fn = match[2];
				if(prefix == "document"){//""?".."
					this.documentParser = o;
					continue;
				}else if(prefix == "xmlns"){
					this.xmlns = o;
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
	},
	document:function(node,context,chain){
		if(this.documentParser){
			this.documentParser.apply(chain,arguments);
			return true;
		}
	},
	seek:function(text,fn,context){
		if(fn in this.seekMap){
			fn = this.seekMap[fn];
			return fn.call(context,text);
		}else{
			$log.warn("文本解析时,找不到相关的解析函数,请检查模板源码,是否手误：[function:"+fn+",document:"+context.currentURI+"]")
			return -1;
		}
	},
	parse:function(node,context,chain){
		if(this.parserMap){
			var n = formatName(node.localName||node.name) ;
			if(n in this.parserMap){
				var fn = this.parserMap[n];
				fn.call(chain,node,context,chain);
				return true;
			}
			fn = fn || this.parserMap[''];
			if(fn){
				fn.call(chain,node,context,chain);
				return true;
			}
		}
		return false;
	},
	on:function(attr,context,chain,parseLeaf){
		if(this.onMap){
			var n = formatName(attr.name) ;
			if(n in this.onMap){
				var fn = this.onMap[n];
				return parseAttribute(attr,fn,context,chain,true)
			}else{
				n+='$';
				if(n in this.onMap){
					var fn = this.onMap[n];
					return parseAttribute(attr,fn,context,chain,parseLeaf)
				}
			}
		}
		return false;
	}
	
}

function parseAttribute(attr,fn,context,chain,parse){
	var needParse = canParseByDom(context,attr);
	var needClean = needParse === null;
	var rtv = false;
	if(needParse || needClean && canParseByMap(context,attr)){
		if(parse){
			fn.call(chain,attr,context,chain);
			rtv = true;
		}else{
			rtv = attr;
		}
	}
	needClean && canParseByMap(context,attr,true);
	return rtv;
}

function formatName(tagName){
	tagName = tagName.replace(/[_\-]/g,"");
	return tagName.toLowerCase();
}
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
		context.setAttribute(ATTRIBUTE_PARSED,map)
	}
	if(clean){
		removeByKey(map,attr);
	}else{
		var parsed = getByKey(map,attr);
		if(parsed){
			return false;
		}else{
			setByKey(map,attr,true);
			return true;
		}
	}
}
var ATTRIBUTE_PARSED = "org.xidea.lite.parse#parsed"