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
	return {
		lazyList:[],
		push:function(){
			for(var len = arguments.length, i = 0;i<len;i++){
				//console.log(arguments[i])
				resp.write(arguments[i]);
			}
		},
		wait:modelWait,
		lazy:function(g){
			this.lazyList.push(g);
		},
		join:function(){
			if(!doMutiLazyLoad(this.lazyList,resp)){
				resp.end();
			}
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
	var modelWait = Function('return function* modelWait(){var i = arguments.length;while(i--){if (arguments[i] instanceof Promise) {yield arguments[i]}}}')()
}catch(e){
	console.error('es6 yield is not support!!');
	var modelWait = function(){
		return {done:true}
	}
}

function doMutiLazyLoad(lazyList,resp){
	var len = lazyList.length;
	var dec = len;
	for(var i = 0;i<len;i++){
		startModule(lazyList[i],[]);
	}

	//console.log('lazy module:',len,lazyList)
	function startModule(g,r){
		var id = g.name;
		r.wait = modelWait;
		g = g(r);
		function next(){
			var n = g.next();
			//console.log('do next:',n)
			if(n.done){
				//console.log('done');
				var rtv = r.join('');
				//console.log('#$%$###### item',rtv)
				resp.write('<script>moduleLoaded("'+id+'",'+JSON.stringify(rtv)+')</script>')
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