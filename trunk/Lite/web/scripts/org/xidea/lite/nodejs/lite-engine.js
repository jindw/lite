function LiteEngine(root){
	root =root.replace(/[\\\/]?$/,'/');
	this.root = root;
	this.compiler = require('child_process').fork(__dirname + '/lite-setup.js',[root]);
	var templateMap = this.templateMap = {};
	var renderTask = this.renderTask = {};
	this.compiler.on('message', function(result) {
		var path = result.path;
		var code = result.code;
		if(code){
			//var featureMap = result[2];
			var tpl = new (Template)(eval('('+code+')'));
			tpl['#prefix'] = result.prefix;
			templateMap[path] = tpl; 
			var task = renderTask[path];
			if(task){
				delete renderTask[path];
				for(var i=0;i<task.length;i++){
					var args = task[i];
					args[0] = tpl;
					render.apply(null,args)
				}
			}
		}else{//clear cache
			delete templateMap[path];
			console.info('clear template cache:' ,path);
		}
	}); 
}
LiteEngine.prototype.render=function(path,data,response){
	var tpl = this.templateMap[path];
	if(tpl){
		render(tpl,data,response);
	}else{
		(this.renderTask[path] || (this.renderTask[path] =[])).push([path,data,response]);
		this.compiler.send({path:path });
	}
}
LiteEngine.prototype.startTestServer = function(host,port){
	require('./test-server').startTestServer(this,host,port);
}
function render(tpl,data,response){
	if(typeof data == 'function'){
		//TODO,需要引擎级别实现异步,这里知识兼容一下接口
		renderSync(tpl,callback,response)
	}else{
		var rtv = tpl.render(data);
		response.write(tpl['#prefix'],'utf-8');
		response.write(rtv,'utf-8');
		response.end();
	}
}
function renderSync(tpl,callback,response){
	response.write(tpl['#prefix'],'utf-8');
	var rtv = data(function(data){
		if(tpl){
			var rtv = tpl.render(data);
			response.write(rtv,'utf-8');
			response.end();
		}
	});
	if(rtv){
		var rtv = tpl.render(rtv);
		response.write(rtv,'utf-8');
		response.end();
		tpl = null;
	}
}
function Template(fn){
    this.render = fn(this);
}
var replaceMap = {'"':'&#34;','<':'&lt;','&':'&#38;'};
function replacer(c){return replaceMap[c]||c}
function dl(date,format){//3
    format = format.length;
    return format == 1?date : ("000"+date).slice(-format);
}
function tz(offset){
	return offset?(offset>0?'-':offset*=-1||'+')+dl(offset/60,'00')+':'+dl(offset%60,'00'):'Z'
}
Template.prototype = {
	//xt:0,xa:1,xp:2
	0:function(txt,type){
		return String(txt).replace(
			type==1?/[<&"]/g:
				type?/&(?:\w+|#\d+|#x[\da-f]+);|[<&"]/ig:/[<&]/g
			,replacer);
	},
	1:function(source,result,type) {
		if(source instanceof Array){
			return source;
		}
		var result = [];
		if(typeof source == 'number'){
			source = parseInt(source);
			while(source >0){
				result[--source] = source+1;
			}
		}else{
			for(source in source){
				result.push(source);
			}
		}
		return result;
	},
	2: function(pattern,date){
		//TODO:未考虑国际化偏移
		date = date?new Date(date):new Date();
        return pattern.replace(/([YMDhms])\1*|\.s|TZD/g,function(format){
            switch(format.charAt()){
            case 'Y' :
                return dl(date.getFullYear(),format);
            case 'M' :
                return dl(date.getMonth()+1,format);
            case 'D' :
                return dl(date.getDate(),format);
//	            case 'w' :
//	                return date.getDay()+1;
            case 'h' :
                return dl(date.getHours(),format);
            case 'm' :
                return dl(date.getMinutes(),format);
            case 's' :
                return dl(date.getSeconds(),format);
            case '.':
            	return '.'+dl(date.getMilliseconds(),'000');
            case 'T'://tzd
            	//国际化另当别论
            	return tz(date.getTimezoneOffset());
            }
        });
	}
}
exports.LiteEngine = LiteEngine;