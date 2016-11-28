var fs = require('fs')
var path = require('path')
var buildURIMatcher = require('../parse/resource').buildURIMatcher
var LiteCompiler = require('../compiler').LiteCompiler;
exports.compile = function(root,dest,translator,includes,excludes){
	root = root || './';
	dest = dest || path.join(root,'.litecode');
	var compiler = new LiteCompiler(root);
	includes = includes && includes.length && new RegExp(includes.map(buildURIMatcher).join('|'))
	excludes = excludes && excludes.length && new RegExp(excludes.map(buildURIMatcher).join('|'))
	function loadFile(dir){
		fs.readdir(dir,function(err,files){
			for(var i=0;i<files.length;i++){
				var n = files[i];
				var file = dir+'/'+n;
				var stat = fs.statSync(file);
				if(stat.isFile()){
					var p = '/'+path.relative(root,file).replace(/\\/g,'/');
					if(excludes && excludes.test(p)){
						continue;
					}
					if(includes ? includes.test(p):/\.xhtml$/.test(p)){
						//console.log(path.join(dest,p))
						
						var result = compiler.compile(p);
						var id = p.slice(1).replace(/[^\w\-]/g,'^');
						fs.writeFile(path.join(dest,id)+'.js',result.jscode,function(err){
							//console.log(err)
						})
						//dest.writeFile(path.join(dest,p))
					}
					//console.log(p)
				}else if(n.charAt() != '.' &&stat.isDirectory() ){
					loadFile(file)
				}
			}
		});
	}
	loadFile(root);
}