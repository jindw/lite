var require = function(){
	moduleCached = {};
	var fsModule = {
		readFileSync:function(path){
			var ins = new java.io.FileInputStream(path);
			//Buffer
			return {
				toString:function(encoding){
					var s = new java.io.InputStreamReader(ins,encoding||'utf-8')
					var buf = [],c;
					while((c = s.read())>=0){
						//console.log(c)
						buf.push(c)
					}
					//console.log(String.fromCharCode.apply(String,buf))
					return String.fromCharCode.apply(String,buf)
				}
			}
		},
		existsSync:function(path){
			return new java.io.File(path).exists();
		}
	}
	var pathModule = {
		resolve:function(){
			var i = 0;
			var file = new java.io.File(arguments[i++]);
			while(i<arguments.length){
				file = new java.io.File(file,arguments[i++]);
			}
			return file.getAbsoluteFile().getCanonicalPath();
		}
	}
	function requireAbs(path,base){
		if(path == 'path'){
			return pathModule;
		}
		if(path == 'fs'){
			return fsModule;
		}
		var absPath = findPath(path,base);
		if(!absPath){
			return;
		}
		if(!new java.io.File(absPath).exists()){
			absPath = absPath+'.js'
		}
		var module = moduleCached[absPath];
		if(!module){
			module = moduleCached[absPath] = {exports:{},id:absPath};
			var script = fsModule.readFileSync(absPath).toString();
			//print(absPath,script,'###')
			var define = new Function('module','require','__dirname','__filename','var exports=module.exports;'+script)
			define(module,function(path){
				return requireAbs(path,absPath);
			},absPath.replace(/[^\\\/]*$/,''),absPath);
			//console.log(script)
		}
		return module.exports;
	}
	function findPath(path,base){
		var dir = base.replace(/[^\\\/]*$/,'');
		if(path.charAt() == '.'){
			path = dir+path;
			//console.log('start',path)
			while(path != (path =path.replace( /[^\/]+\/\.\.\/|(\/)?\.\//,'$1')));
			//console.log('end',path)
			return path;
		}else if(path.charAt() == '/'){
			return path;
		}else{
			var moduleName = path.replace(/\/.*/,'');
			while(dir){
				if(new java.io.File(dir+'node_modules/'+moduleName).exists()){
					var absPath = dir+'node_modules/'+path;
					//console.log(moduleName , path,absPath)
					if(moduleName == path){
						var main = JSON.parse(fsModule.readFileSync(absPath+'/package.json')+'').main;
						absPath = absPath +  main.replace(/^.?\/?/,'/')
						//console.log(moduleName , path,absPath)
					}
					return absPath;
				}
				dir = dir.replace(/[^\/]*\/?$/,'');
			}
			return null;
		}
	}
	var absPath = new java.io.File('./').getAbsolutePath().replace(/\.$/,'');
	//print(absPath)
	return function require(path){
		return requireAbs(path,absPath)
	}
}()


//兼容 IE console 缺失的情况
if(!this.console || !console.dir){
	console = this.console || {
		log:print,
	};
	console.warn || "trace,debug,info,warn,error".replace(/\w+/g,function(n){
		console[n] = function(){
			arguments[0] = n + ':' + arguments[0]
			this.log.apply(this,arguments);
		}
	});
	console.dir = function(o){for(var n in o){console.log(n,o[n]);}}
	console.time = console.time || function(l){this['#'+l] = +new Date}
	console.timeEnd = console.timeEnd || function(l){console.log(l + (new Date-this['#'+l]));}
	console.assert = console.assert || function(l){if(!l){console.error('Assert Failed!!!')}}
}

//console.warn('123','232323232',2323343)
//require('./template.js')