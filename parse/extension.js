/**
 * Lite JS 扩展规范：
 * 一个js包中：
 *            函数 document 为通用文档解释器  
 *            所有 intercept<Attribute> 为当前名称空间属性解析器
 *            函数 xmlns 为当前名称空间属性解释器,未定义或者空函数时则该属性不输出
 *            所有 on<Attribute> 为当前名称空间元素普通属性（无名称空间的属性）的解释器
 *            所有 parse<TagName> 为当前名称空间元素（Element）解释器
 *            所有 seek<Function Name> 为当前名称空间前缀的文本函数解释器
 */
function Extension(){
	this.namespaceParser = null;
	this.typeMap = null;
	
	this.interceptMap = null;
	this.patternInterceptMap = null;
	
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
function appendParser(extension,key,patternKey,fn,o){
	var namedMap = extension[key];
	if(fn.indexOf('*')>=0){//is pattern parser 
		var patternMap = extension[patternKey];
		if(!patternMap){
			extension[patternKey] = patternMap = {};
		}
		add(patternMap,fn,o);//添加 patternParser
		//console.info(patternKey,fn,pm)
		if(namedMap){//扫描已有 parser 添加 patternParser
			var p = new RegExp('^'+fn.replace(/\*/g,'.*')+'$');
			for(var n in namedMap){
				if(p.test(n)){
					add(namedMap,n,o);
				}
			}
		}
	}else{//普通parser
		if(!namedMap){//创建时，自动扫描已有pattern Parser
			extension[key] = namedMap = {};
			var patternMap = extension[patternKey];
			if(patternMap){
				for(var p in patternMap){
					if(new RegExp('^'+p.replace(/\*/g,'.*')+'$').test(fn)){
						add(namedMap,fn,patternMap[p]);
					}
				}
			}
		}
		add(namedMap,fn,o);
	}
}

Extension.prototype={
	initialize:function(objectMap){
		//console.dir(objectMap)
		for(var key in objectMap){
			var o = objectMap[key];
//			console.error("["+key+"]:"+o+"\n\n")
			if(o instanceof Function){
				var dest = null;
				var match = key.match(/^(parse|seek|intercept|xmlns|on)(.*)/);
				var prefix =match&& match[1];
				var fn = match &&formatName(match[2]);
				if(prefix == "parse"){
					var c = fn.charAt(0);
					fn = fn.replace(/^[12]/,'');
					if( c == '$'){//type parser(eg : comment parser,document parser)
						if(!this.typeMap){
							this.typeMap = {};
						}
						add(this.typeMap,fn,o);
					}else{
						appendParser(this,"tagMap","patternTagMap",fn,o);
					}
				}else if(prefix == "on"){
					appendParser(this,"attributeMap","patternAttributeMap",fn,o);
				}else if(prefix == "xmlns"){
					this.namespaceParser = o;
				}else if(prefix == "intercept"){
					appendParser(this,"interceptMap","patternInterceptMap",fn,o);
					//dest = this.interceptMap ||(this.interceptMap={});
					//dest[fn] = o;
				}else if(prefix == "seek"){//""?".."
					dest = this.seekMap ||(this.seekMap={});
					dest[fn] = o;
				}
			}
		}
		//this.interceptMap && console.log(Object.keys(this.interceptMap));
	}
}

function formatName(tagName){
	tagName = tagName.replace(/\$([\da-f]{4})/ig,function(a,c){
		return String.fromCharCode(parseInt(c,16))
	})
	//console.log(tagName)
	tagName = tagName.replace(/[\-]/g,"");
	
	return tagName.toLowerCase();
}

if(typeof require == 'function'){
exports.Extension=Extension;
}