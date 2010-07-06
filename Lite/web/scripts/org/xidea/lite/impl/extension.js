/**
 * Lite JS 扩展规范：
 * 一个js包中，所有 parse<TagName> 为但前名称空间标记解释起
 *            所有 on<NS Attribute> 为当前名称空间属性解析器
 *            所有 seek<Function Name> 为当前名称空间前缀的文本函数解释器
 *            函数 on 为当前名称空间属性解释器,未空则该属性不输出
 *            对象 parse 的所有on开头的属性，均为当前名称空间节点的普通属性解释器（无名称空间属性）
 */
function Extension(namespace,loader){
	this.namespace = namespace;
	this.onMap = null;
	this.saMap = null;//parse:{onSelected:...,onChecked:...}
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
	parse:function(node,context,chain){
		var type = node.nodeType;
		if(type == 1){//element
			var n = formatName(node.tagName) ;
			if(n in this.parserMap){
				var fn = this.parserMap[n];
				return fn.call(this,fn,node,context,chain);
			}
		}else if(type == 2){//attr
			if(node.namespaceURI){
				if(this.onMap!=null){
					var n = formatName(node.name) ;
					if(n in this.onMap){
						var fn = this.onMap[n];
						return fn.call(this,fn,node,context,chain.previousChain);
					}
				}
			}else{
				if(this.saMap!=null){
					var n = formatName(node.name) ;
					if(n in this.saMap){
						var fn = this.saMap[n];
						return fn.call(this,fn,node,context,chain.previousChain);
					}
				}
			}
		}
		chain.process(node);
	},
	/**
	 */
	setup:function(packageObject){
		for(var n in pkg.objectScriptMap){
			var match = n.match(/^(?:on(.*)|sa(.+)|parse(.+)|seek(.*))/);
			if(match){
				var o = $import(packageObject.name+':'+n,{});
				if(o instanceof Function){
					var dest = null,key;
					if((key = match[1])!=null){//""?".."
						dest = this.onMap ||(this.onMap={});
					}else if((key = match[2])){//""?".."
						dest = this.saMap ||(this.saMap={});
					}else if((key = match[3])){//""?".."
						dest = this.parserMap ||(this.parserMap={});
					}else if((key = match[4])!=null){//""?".."
						dest = this.seekMap ||(this.seekMap={});
					}
					if(dest){
						dest[formatName(key)] = o;
					}
					return {parse:o};
				}
			}
		}
	}
}
