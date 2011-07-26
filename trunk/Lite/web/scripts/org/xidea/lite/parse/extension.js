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
	this.namespaceParser = null;
	this.beforeMap = null;
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
				var match = key.match(/^(parse|seek|before|xmlns)(.*)/);
				var prefix = match[1];
				var fn = formatName(match[2]);
				if(prefix == "parse"){//""?".."
					dest = this.parserMap ||(this.parserMap={});
					if(fn in dest){
						dest[fn].push(o);
					}else{
						dest[fn] = [o];
					}
				}else if(prefix == "xmlns"){
					this.namespaceParser = o;
				}else if(prefix == "before"){
					dest = this.beforeMap ||(this.beforeMap={});
					dest[fn] = o;
				}else if(prefix == "seek"){//""?".."
					dest = this.seekMap ||(this.seekMap={});
					dest[fn] = o;
				}
			}
		}
	}
	
}

function formatName(tagName){
	tagName = tagName.replace(/[\-]/g,"");
	return tagName.toLowerCase();
}
