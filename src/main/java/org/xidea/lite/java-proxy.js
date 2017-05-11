
//兼容 java8/IE console 缺失的情况
if(typeof console == 'undefined' || !console.dir){
	//console.log(typeof console ,!this.console, !console.dir)
	console = this.console || {
		log:print
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


var require = function(){
	moduleCached = {};
	function requireAbs(path,base){
		//console.error(path,base)
		var absPath = initPath(path,base);
		if(!absPath){
			return;
		}
        var modulePath = absPath.replace(/.*\/node_modules\//,'');
		var module = moduleCached[modulePath];
		if(!module){
			module = moduleCached[modulePath] = {exports:{},id:modulePath};
			var define = require.cached[modulePath];
			if(typeof define =='string'){
				if(typeof __java_engine == 'object') {
					try{
						var oldPath = this['javax.script.filename'];
						this['javax.script.filename'] = absPath
                        define = __java_engine.eval("function (require,exports,module,__filename,__dirname){"+define+"\n}");
                    }finally{
                        this['javax.script.filename'] =oldPath;
                    }
				}else{
                    define= new Function('require','exports','module','__filename','__dirname',define);
				}
			}
			//console.log(modulePath,JSON.stringify(require.cached))
			define(function(path){
				return requireAbs(path,absPath);
			},module.exports,module,absPath,absPath.replace(/[^\\\/]*$/,''));
			//console.log(script)
		}
        //console.log(path,module.exports)
		return module.exports;
	}
	function initPath(path,base){
		//console.log(path)
		if(path.charAt() == '.'){//relative path
            path = path.replace(/(?:\.js)?$/,'.js');
            var dir = require('fs').realpathSync(base).replace(/[^\\\/]*$/,'');
			var absPath = dir+path;
			//console.log('start',path)
			while(absPath != (absPath =absPath.replace( /[^\/]+\/\.\.\/|(\/)?\.\//,'$1')));
            var modulePath= absPath.replace(/\\/g,'/').replace(/.*\/node_modules\//,'');
            if(modulePath in require.cached ){

            }else{
                var define = require('fs').readFileSync(absPath).toString();
                require.cached[modulePath] = define;
			}
            return absPath;
		}else{
            if(path.indexOf('/')>=0){
                path = path.replace(/(?:\.js)?$/,'.js');
            }
            var modulePath = path;
            if(modulePath in require.cached ){
                return modulePath;
            }
            var dir = require('fs').realpathSync(base).replace(/[^\\\/]*$/,'');
            var absPath = findExternalModule(path,dir);
            if(absPath){
                absPath = absPath.replace(/\\/g,'/');
                var modulePath= absPath.replace(/\\/g,'/').replace(/.*\/node_modules\//,'');
                if(modulePath.indexOf('/')>0){
                    var define = require('fs').readFileSync(absPath).toString();
                    require.cached[modulePath] = define;
				}
                return absPath;
			}
		}
        //console.error('absPath:',path,base,absPath)
	}
	function findExternalModule(path,dir){
        //absolute node path
		var moduleName = path.replace(/\/.*/,'');
        //var dir = base.replace(/[^\\\/]*$/,'');
		while(dir){
			var moduleDir = dir+'node_modules/'+moduleName;
			if(require('fs').existsSync(moduleDir)){
				//console.log(moduleName , path,absPath)
				if(moduleName == path){
					var main = JSON.parse(require('fs').readFileSync(moduleDir+'/package.json')+'').main;
					var path = moduleName +  main.replace(/^(:?.\/)?/,'/')
					require.cached[moduleName] = 'module.exports = require("'+path+'");'
					//console.log(main,moduleName , path,absPath)
					return moduleDir
				}
				return dir+'node_modules/'+path.replace(/(?:\.js)?$/,'.js');
			}
			dir = dir.replace(/[^\/]*\/?$/,'');
		}
		if(!absPath){
			console.error('missed path! :',path,dir)
			return;
		}
	}
	var absPath ;


	return function requireMock(path){
        if(path in moduleCached ){
            return moduleCached[path].exports;
        }
		var exports =  requireAbs(path,'./')
		if(exports===undefined){
        	var absPath = require('fs').realpathSync('./').replace(/\.$/,'');
			console.error('module not found: '+path+"\t @"+absPath);
		}
		return exports;
	}
}()

//console.log(typeof java.io.File,typeof java)
require.cached = {
    fs:function(require,exports,m){
        var isJavaEnv = typeof java == 'object' && typeof java.io == 'object' && typeof java.io.File == 'function'
        if(!isJavaEnv){
            m.exports = module.require('fs');
            return;
        }
        exports.readFileSync = function(path){
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
        };
        exports.existsSync =function(path){
            return new java.io.File(path).exists();
        };
        exports.realpathSync = function(path){
            return new java.io.File(path).getAbsolutePath()
        }
    },
    path : function(require,exports,m,__filename,__dirname){
        var isJavaEnv = typeof java == 'object' && typeof java.io == 'object' && typeof java.io.File == 'function'
        if(!isJavaEnv){
            m.exports = module.require('path');
            return;
        }
        exports.resolve = function(){
            var i = 0;
            var file = new java.io.File(arguments[i++]);
            while(i<arguments.length){
                file = new java.io.File(file,arguments[i++]);
            }
            return file.getAbsoluteFile().getCanonicalPath();
        }
    }
}



if(typeof process =='object' && process.argv instanceof Array && typeof module == 'object'){//for node client
    module.exports = require;
    //require.cached.path = function(require,exports,m){m.exports = module.require('path')};

    //var code = require('./java-proxy').exports('lite/src/main/js/compiler');
    //console.log(code)
    function exportSource(){
        var buf = ["if(typeof console  == 'undefined'){",
            "console = {};'log,warn,error,info,debug'.replace(/\\w+/g,function(a){console[a] = print});",
            "console.dir = function(o){for(var n in o){console.log(n,o[n]);}};\n",
            "console.time = console.time || function(l){this['#'+l] = +new Date};\n",
            "console.timeEnd = console.timeEnd || function(l){console.log(l + (new Date-this['#'+l]));};\n",
            "console.assert = console.assert || function(l){if(!l){console.error('Assert Failed!!!')}};\n",
        "}"]
        var requireMock = require;//require('./java-proxy.js');
        for(var i =0;i<arguments.length;i++){
            //requireMock('lite/src/main/js/compiler');
			//console.log(arguments[i])
            requireMock(arguments[i]);
        }

        //console.log('~',arguments.callee,'()')
        buf.push(function require(id){
            id = id.replace(/(\/.*?)(?:\.js)?$/,'$1.js')
            if(id in require.module){
                return require.module[id].exports;
            }
            var module = require.module[id] = {exports:{},id:id};
            var dir = id.replace(/[^\/\\]+$/,'');
            //console.log(id)
            require.cached[id].call(null,function(id2){
                if(id2.charAt() == '.'){
                    id2 = dir+id2;
                    while(id2 != (id2 =id2.replace( /[^\/]+\/\.\.\/|(\/)?\.\//,'$1')));
                }
                return require(id2);
            },module.exports,module,id,dir);
            return module.exports;
        },'\nrequire.cached={}\nrequire.module={}\nif(typeof module == "object"){module.exports = require};\n');
        for(var n in requireMock.cached) {
            var v = requireMock.cached[n];
            if (typeof v == 'string') {
                //v = v.indexOf('`')<0?'`'+v+'`':JSON.stringify(v)
                v = 'function (require,exports,module,__filename,__dirname){' + v + '\n}'
            }
            buf.push('require.cached["' + n + '"]=', v, '\n;')
        }
        return buf.join('')
    }
    require.exportSource =exportSource;
    var exportModules = process.argv.slice(2);
    //console.log()
	if(exportModules.length){
		console.log("Exports modules:",JSON.stringify(exportModules))
        var source = exportSource.apply(null,exportModules);
        var file = __filename.replace(/[^\\\/]+$/,'java-packed.js');
        require('fs').writeFileSync(file,source);
		console.log('file updated!!',file)
	}
}