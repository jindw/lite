function Template(code,config){
 	//console.log(code)
 	
	try{
    	this.impl = eval('['+code+'][0]');
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
		this.impl.call(null,context,wrapResponse(response));
	}catch(e){
		console.warn(this.impl+'');
		var rtv = require('util').inspect(e,true)+'\n\n'+(e.message +e.stack);
		response.end(rtv);
		throw e;
	}
}
function wrapResponse(resp){
	
	var lazyList = [];
	var buf=[];
	var bufLen=0;
	return {
		push:function(){
			for(var len = arguments.length, i = 0;i<len;i++){
				//console.log(arguments[i])
				var txt = arguments[i];
				buf.push(txt)
				if((bufLen+=txt)>1024){
					resp.write(buf.join(''));
					buf = [];
					bufLen = 0;
				}
				//resp.write(arguments[i]);
			}
		},
		join:function(){
			resp.write(buf.join(''));
			buf = [];
			if(!doMutiLazyLoad(lazyList,resp)){
				resp.end();
			}
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
	var modelWait = function(){
		return {done:true}
	}
}

function doMutiLazyLoad(lazyList,resp){
	var len = lazyList.length;
	var dec = len;
	var first = true;
	for(var i = 0;i<len;i++){
		startModule(lazyList[i],[]);
	}

	//console.log('lazy module:',len,lazyList)
	function startModule(g,r){
		var id = g.name.replace(/^[^\d]+|[^\d]+$/g,'');//__lazy_module_\d+__
		r.flush = function(){};
		r.wait = modelWait;
		g = g(r);
		function next(){
			var n = g.next();
			//console.log('do next:',n)
			if(n.done){
				//console.log('done');
				var rtv = r.join('');
				//console.log('#$%$###### item',rtv)
				//
				//resp.write('<script>(this.__module_loaded__||function(id,h){document.getElementById(id).innerHTML=h})("'+id+'",'+JSON.stringify(rtv)+')</script>')
				resp.write('<script>'+
				(first?'!this.__widget_loaded__&&(this.__widget_loaded__=function(id,h){document.querySelector(id).innerHTML=h});':'')
				+'__widget_loaded__("*[data-lazy-widget-id=\''+id+'\']",'+JSON.stringify(rtv).replace(/<\/script>/ig,'<\\/script>')+')</script>')
				first = false;
				if(--dec == 0){
					//console.log('#$%$######end')
					resp.end();
				}
				return rtv;
			}else{
				 n.value.then(next);
				 //console.log('is promise',n.value)
			}
		}

		//console.log('lazy module:',id)
		next();
	}
	return len;
}
exports.wrapResponse = wrapResponse;
exports.Template = Template;