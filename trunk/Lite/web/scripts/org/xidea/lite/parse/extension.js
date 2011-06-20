/**
 * Lite JS 扩展规范：
 * 一个js包中：
 *            函数 document 为通用文档解释器  
 *            所有 before<Attribute> 为当前名称空间属性解析器
 *            函数 xmlns 为当前名称空间属性解释器,未定义或者空函数时则该属性不输出
 *            所有 on<Attribute> 为当前名称空间元素普通属性（无名称空间的属性）的解释器
 *            所有 parse<TagName> 为当前名称空间元素（Element）解释器
 *            所有 seek<Function Name> 为当前名称空间前缀的文本函数解释器
 */
function Extension(){
	this.documentParser = null;
	this.namespaceParser = null;
	this.beforeMap = null;
	this.onMap = null;
	this.parserMap = null;
	this.seekMap = null;
}
Extension.prototype={
	initialize:function(objectMap){
		
		for(var key in objectMap){
			var o = objectMap[key];
//			$log.error("["+key+"]:"+o+"\n\n")
			if(o instanceof Function){
				var dest = null;
				var match = key.match(/^(parse|seek|xmlns|on|before)(.*)/);
				var prefix = match[1];
				var fn = match[2];
				if(prefix == "parse"){//""?".."
					dest = this.parserMap ||(this.parserMap={});
				}else if(prefix == "seek"){//""?".."
					dest = this.seekMap ||(this.seekMap={});
				}else if(prefix == "before"){
					dest = this.beforeMap ||(this.beforeMap={});
				}else if(prefix == "on"){
					dest = this.onMap ||(this.onMap={});
				}else if(prefix == "xmlns"){
					this.namespaceParser = o;
					continue;
				}
				if(dest){
					if(fn == "9"){//document
						this.documentParser = o;
					}else{
						dest[formatName(fn)] = o;
					}
				}
			}
		}
	},
	seek:function(text,fn,context){
		if(fn in this.seekMap){
			fn = this.seekMap[fn];
			var rtv = fn.call(context,text);
			return rtv == null?-1:rtv;
		}else{
			$log.warn("文本解析时,找不到相关的解析函数,请检查模板源码,是否手误：[function:"+fn+",document:"+(context && context.currentURI)+"]")
			return -1;
		}
	}
	
}

function formatName(tagName){
	tagName = tagName.replace(/[\-]/g,"");
	return tagName.toLowerCase();
}
