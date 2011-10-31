function TemplateEngine(root){
	this.root = root;
	this.compiler = require('child_process').fork(__dirname + '/template-setup.js',[root]);
	this.templateMap = {};
	this.renderTask = {};
	this.compiler.on('message', function(result) {
		var res = result[0];
		var fn = result[1];
		//var featureMap = result[2];
		var tpl = new (Template)(new Function(fn));
		this.templateMap[res[0]] = tpl; 
		for(var i=0;i<res.length;i++){
//			fs.watchFile(filename, function(pre,next){
//				
//			})
		}
		var task = this.renderTask[path];
		if(task){
			delete this.renderTask[path];
			for(var i=0;i<task.length;i++){
				render.apply(null,task[i])
			}
		}
	}); 
}
TemplateEngine.prototype.render=function(path,data,response){
	var tpl = this.getTemplate(path);
	if(tpl){
		render(tpl,data,response);
	}else{
		(this.renderTask[path] || (this.renderTask[path] =[])).push([path,data,response]);
		this.compiler.send({path:path });
	}
}

TemplateEngine.prototype.getTemplate=function(path){
	var cache = this.templateMap[path];
	var res = cache[0];
	var tpl = cache[1];
	if(res){
		return tpl;
	}
}
function render(tpl,data,response){
	var rtv = tpl.render(data);
	response.write(rtv,'utf-8')
	return ;
}