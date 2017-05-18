exports.parseDefName = parseDefName;
exports.findLiteParamMap = findLiteParamMap;
exports.parseChildRemoveAttr = parseChildRemoveAttr;


exports.setNodeURI = setNodeURI;
exports.appendForStart = appendForStart;
var getLiteTagInfo=require('./xml').getLiteTagInfo;

function parseDefName(name){
	var n = name;
	var i = n.indexOf('(');
	var defaults = [];
	var params = [];
	if(i>0){
		var args = n.substring(i+1);
		args = args.replace(/^\s+|\)\s*$/g,'')
		n = toid(n.substring(0,i));
		i = 0;
		while(args){
			i = args.indexOf(',',i);
			if(i>0){
				var arg = args.substring(0,i);
				try{
					new Function(arg);
					args = args.substring(i+1).replace(/^\s+|\s+$/g,'');
					i=0;
				}catch(e){
					i++;
					continue;
				}
			}else{
				arg = args;
				args = null;
				try{
					new Function(arg);
				}catch(e){
					console.error("函数定义中参数表语法错误:"+arg+name,e);
					throw e;
				}
			}
			var p = arg.indexOf('=',i);
			if(p>0){
				params.push(toid(arg.substring(0,p)));
				defaults.push(JSON.parse(arg.substring(p+1)));
			}else{
				if(defaults.length){
					var msg = "函数定义中参数表语法错误:默认参数值能出现在参数表最后:"+name;
					console.error(msg);
					throw new Error(msg);
				}
				params.push(toid(arg));
			}
			
			
		}
		
		return {"name":n,"params":params,"defaults":defaults};
	}else{
		return {"name":n}
	}
}


function findLiteParamMap(value){
	var result = {};
	while(value){
		var match = value.match(/^\s*([\w\$\_]+|'[^']*'|"[^"]*")\s*(?:[\:=]\s*([\s\S]+))\s*$/);
		if(!match){
			throw console.error("非法参数信息",value);
			return null;
		}
		value =match[2];
		var key = match[1].replace(/^['"]|['"]$/g,'');
		var p = findStatementEnd(value);
		var statment = value.substring(0,p);
		
		result[key] = statment;
		value = value.substring(p+1);
	}
	return result;
}
/**
 * @private
 */
function findStatementEnd(text){
	var end = 0;
	do{
		var end1 = text.indexOf(',',end + 1);
		var end2 = text.indexOf(';',end + 1);
		if(end2>0 && end1>0){
			end = Math.min(end1 , end2);
		}else{
			end = Math.max(end1,end2);
		}
		if(end<=0){
			break;
		}
		var code = text.substring(0,end);
		try{
			new Function(code);
			return end;
		}catch(e){
			end = end+1
		}
	}while(end>=0)
	return text.length;
}

function setNodeURI(context,node){
	if(!node.nodeType){
		if(node.length){
			node = node.item(0);
		}
	}
	var doc = node.nodeType == 9?node:node.ownerDocument;
	if(doc){
		
		var uri = doc.documentURI
		if(/^lite:\//.test(uri)){
			context.setCurrentURI(context.createURI(uri));
		}else if(uri){
			var info = getLiteTagInfo(doc.documentElement);
			//console.log(info)
			var i = info && info.indexOf('|@');
			if(i>0){
				uri = info.substring(i+2);
			}
			context.setCurrentURI(context.createURI(uri));
			//console.error(uri,info)
		}
	}
}
function parseChildRemoveAttr(context,node,ignoreSpace){
	if(node.nodeType == 1){//child
		var child = node.firstChild;
		if(ignoreSpace){
			while(child){
				if(child.nodeType != 3 || String(child.data).replace(/\s+/g,'')){
					context.parse(child)
				}
				child = child.nextSibling;
			}
		}else{
			while(child){
				context.parse(child)
				child = child.nextSibling;
			}
		}
	}else if(node.nodeType == 2){//attr
		//console.log('do child remove:'+node)
		//throw new Error();
		var el = node.ownerElement||node.selectSingleNode('..');
		//console.log(node.nodeName,node.namespaceURI);
		try{
			el.removeAttributeNode(node);
			context.parse(el);//||node.selectSingleNode('parent::*'));
		}finally{
			el.setAttributeNode(node)
		}
	}else {//other
		context.parse(node)
	}
}



//varName@status list
var FOR_PATTERN = /\s*([\$\w_]+)\s*(?:\(\s*([\w\$_]+)\))?\s*(?:\:|in)([\s\S]*)/;
function appendForStart(context,var_,list,status_){
	//appendForStart(this,var_,value,status_ || null);
	if(!list){
		var match = var_.match(FOR_PATTERN);
		if(!match){
			throw console.error("非法 for 循环信息",var_);
		}
		var var_ = match[1];
		var status_ =match[2];
		var list =match[3];
	}
	
	
	var be = _splitList(list);
	if(be.length==2){
		var begin = be[0];//list.substring(0,dd);
		var end = be[1];//list.substring(dd+2);
		list = "Math.abs("+begin+' - '+end+")+1";
		context.appendFor(var_,list,status_||null);
		context.appendVar(var_,'for.index+'+begin);
	}else if(be.length ==1){
		context.appendFor(var_,list,status_||null);
	}else{
		console.error("for表达式无效："+list);
		throw new Error('for 表达式无效：list='+list+';var='+var_);
	}
}


function _splitList(list){
	try{
		new Function("return "+list.replace(/\.\./g,'.%%.'));//for x4e
		return [list];
	}catch(e){
		var dd= 0
		while(true){
			dd = list.indexOf('..',dd+1);
			if(dd>0){
				try{
					var begin = list.substring(0,dd);
					var end = list.substring(dd+2);
					new Function("return "+begin+' - '+end);//for x4e
					var begin2 = begin.replace(/^\s*\[/,'');
					if(begin2 != begin){
						try{
							new Function("return "+begin);
							begin2 = begin;
						}catch(e){
						}
					}
					if(begin2 != begin){
						end = end.replace(/\]\s*$/,'');
						console.info("[start,last] 语法 不是通用表达式，只能在for循环中使用。",list);
						return [begin2,end];
					}else{
						console.warn("range for 表达式(非通用表达式)推荐模式为：[start:last]，您提供的表达式为"+list);
						return [begin,end];
					}
				}catch(e){
					console.error('invalid for range:'+list,e)
				}
				//value = list.substring(0,dd)+'-'+list.substring(dd+2)
			}else{
				return [];
			}
		}
	}
}

function toid(n){
	n = n.replace(/^\s+|\s+$/g,'');
	try{
		new Function("return "+n);
	}catch(e){
		console.error("无效id:"+n,e);
		throw e;
	}
	return n;
}