function Template(code,config){
 	//console.log(code)
 	
	try{
    	this.impl = code instanceof Function?code:eval('['+code+'][0]');
    }catch(e){
    	//console.error(config.path,require('util').inspect(e,true)+'\n\n'+(e.message +e.stack));
    	this.impl = function(){throw e;};
    }
    this.config = config;
    this.contentType = config.contentType;
    this.encoding = config.encoding;
}
Template.prototype.render = function(context,response){
	try{
		this.impl.call(null,context,wrapResponse(response,this));
	}catch(e){
		console.warn(this.impl+'');
		var rtv = require('util').inspect(e,true)+'\n\n'+(e.message +e.stack);
		response.end(rtv);
		throw e;
	}
}
Template.prototype.lazyAppend =function(resp,id,content,index,count){
	//(first?'!this.__widget_arrived&&(this.__widget_arrived=function(id,h){document.querySelector(id).innerHTML=h});':'')
	content = JSON.stringify(content).replace(/<\/script>/ig,'<\\/script>');
	resp.write('<script>__widget_arrived("*[data-lazy-widget-id=\''+id+'\']",'+content+')</script>')
}
function wrapResponse(resp,tpl){
	var lazyList = [];
	var buf=[];
	var bufLen=0;
	return {
		push:function(){
			for(var len = arguments.length, i = 0;i<len;i++){
				//console.log(arguments[i])
				var txt = arguments[i];
				if(bufLen>1024){
					resp.write(buf.join(''));
					buf = [];
					bufLen = 0;
				}
				buf.push(txt)
				if(txt){
					bufLen+=txt.length
				}
				//resp.write(arguments[i]);
			}
		},
		join:function(){
			var last = buf.pop();
			var matchEnd = last && last.match(/<\/html>\s*$/i);
			if(matchEnd){
				matchEnd = matchEnd[0];
				buf.push(last.slice(0,-matchEnd.length))
			}
			resp.write(buf.join(''));
			buf = [];
			doMutiLazyLoad(tpl,lazyList,resp,function(){
				resp.end(matchEnd||last||'');
			})
		},
		flush:function(){
			resp.write(buf.join(''));
			buf = [];
			bufLen = 0;
		},
		wait:modelWait,
		lazy:function(g){
			lazyList.push(g);
		}
	}
}
/*
//俺的编辑器，混淆器有问题
function* modelWait(){
	var i = arguments.length;
	while(i--){
		if (arguments[i] instanceof Promise) {
			yield arguments[i]
		}
	}
}*/
try{
	var modelWait = Function('return function* modelWait(){' +
			'var i = arguments.length;while(i--){if (arguments[i] instanceof Promise) {' +
			'this.flush();'+
			'yield arguments[i]}}}')()
}catch(e){
	console.error('es6 yield is not support!!');
	var modelWait = function(value){
		return {done:true,value:value}
	}
}

function doMutiLazyLoad(tpl,lazyList,resp,onComplete){
	var len = lazyList.length;
	var index = 0;
	if(len){
		for(var i = 0;i<len;i++){
			startModule(lazyList[i],[]);
		}
		function startModule(g,r){
			var id = g.name.replace(/^[^\d]+|[^\d]+$/g,'');//__lazy_module_\d+__
			r.flush = function(){};
			r.wait = modelWait;
			g = g(r);
			function next(){
				var n = g.next();
				if(n.done){
					var content = r.join('');
					tpl.lazyAppend(resp,id,content,index,len)
					if(++index >= len){
						onComplete();
					}
					return content;
				}else{
					 n.value.then(next);
				}
			}
			next();
		}
	}else{
		onComplete();
	}
}
exports.wrapResponse = wrapResponse;
exports.Template = Template;