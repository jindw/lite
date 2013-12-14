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
	this.typeMap = null;
	this.tagMap = null;
	this.patternTagMap = null;
	this.attributeMap = null;
	this.patternAttributeMap = null;
	this.seekMap = null;
}
function add(m,fn,o){
	if(fn in m){
		m[fn].push(o);
	}else{
		m[fn] = [o];
	}
}
function appendParser(ext,key,patternKey,fn,o){
	var m = ext[key];
	if(fn.indexOf('*')>=0){//is pattern parser 
		var pm = ext[patternKey];
		if(!pm){
			ext[patternKey] = pm = {};
		}
		add(pm,fn,o);//添加 patternParser
		//console.info(patternKey,fn,pm)
		if(m){//扫描已有 parser 添加 patternParser
			var p = new RegExp('^'+fn.replace(/\*/g,'.*')+'$');
			for(var n in m){
				if(p.test(n)){
					add(m,n,o);
				}
			}
		}
	}else{//普通parser
		if(!m){//创建时，自动扫描已有pattern Parser
			ext[key] = m = {};
			var pm = ext[patternKey];
			if(pm){
				for(var p in pm){
					if(new RegExp('^'+p.replace(/\*/g,'.*')+'$').test(fn)){
						add(m,fn,pm[p]);
					}
				}
			}
		}
		add(m,fn,o);
	}
}

Extension.prototype={
	initialize:function(objectMap){
		for(var key in objectMap){
			var o = objectMap[key];
//			console.error("["+key+"]:"+o+"\n\n")
			if(o instanceof Function){
				var dest = null;
				var match = key.match(/^(parse|seek|before|xmlns)(.*)/);
				var prefix = match[1];
				var fn = formatName(match[2]);
				if(prefix == "parse"){//""?".."
					var c = fn.charAt(0);
					fn = fn.replace(/^[12]/,'');
					if(c == '2'){
						appendParser(this,"attributeMap","patternAttributeMap",fn,o);
					}else if(c <'0' || c > '9' || c == '1'){
						appendParser(this,"tagMap","patternTagMap",fn,o);
					}else{
						if(!this.typeMap){
							this.typeMap = {};
						}
						add(this.typeMap,fn,o);
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

if(typeof require == 'function'){
exports.Extension=Extension;
}