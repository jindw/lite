var $import;
var $JSI = function(){
	//status:0,1,2
	//path=>[exports,loader,dependenceMap]
	var loaderMap = {};
	var notifyMap = {};
	var taskMap = {};
	var async;
	function normalizeURI(url,base){
        var url = url.replace(/\\/g,'/');
        if(url.charAt(0) == '.'){
        	url = base.replace(/[^\/]+$/,'')+url
        	while(url != (url =url.replace( /[^\/]+\/\.\.\/|(\/?)\.\//,'$1')));
        }
        return url;
    }
	function require(path){
		var entry = loaderMap[path];
		var cache = {};
		entry[1].call(this,function(path2){
			if(path2 in cache){
				return cache[path2];
			}
			return cache[path2] = require(normalizeURI(path2,path));
		},entry[0]);
		return entry[0];
	}
	function define(path,dependences,impl){
		var dependenceMap = {};
		var loader = loaderMap[path] = [{},impl,dependenceMap];
		var len = dependences.length;
		while(len--){
			var dep = normalizeURI(dependences[len],path);
			if(!loaderMap[dep]){
				dependenceMap[dep] = 1;
				load(dep,path);
			}
		}
		onDefined(path)
	}
	function copy(src,dest){
		for(var p in src){
			dest[p] = src[p]
		}
	}
	//要求单线程模式
	function onDefined(path){
		var notifySet = notifyMap[path];
		var dependenceMap = loaderMap[path][2];
		var dependenceCount=0;
		for(var p in dependenceMap){
			if(loaderMap[p]){
				delete dependenceMap[p];
			}else{
				dependenceCount++;
			}
		}
		outer:for(p in notifySet){
			var notifyDependenceMap = loaderMap[p][2];
			if(delete notifyDependenceMap[path]){//has and deleted
				if(dependenceCount){
					copy(dependenceMap,notifyDependenceMap)
					_moveNodify(dependenceMap,p)
					//add nodify
				}else{
					for(var n in notifyDependenceMap){
						continue outer;
					}
					onComplete(p);
				}
			}
		}
		if(!dependenceCount){
			//notify
			onComplete(path);
		}
	}
	function onComplete(path){
		var task = taskMap[path];
		if(task){
			var len = task.length;
			while(len--){
				task[len].call(this,require(path))
			}
		}
	}
	function _moveNodify(loadingMap,path){//这里关联的　notifySet　一定有值，因为曾经添加过　
		for(var p in loadingMap){
			var notifySet = notifyMap[p];
			notifySet[path] = 1;
		}
	}
	function load(path,from){
		var notifySet = notifyMap[path];
		if(!notifySet){
			notifyMap[path] =notifySet = {};
			if(from){
				notifySet[from]=1
			}
			console.log(path)
			if(path.indexOf('imagebox')>=0){
				alert(path)
			}
			path = $JSI.scriptBase+path+'__define__.js';
			if(async){
				var s = document.createElement('script');
				s.setAttribute('src',path);
				s.onerror = $JSI.loadError;
				document.body.appendElement(s);
			}else{
				document.write('<script src="'+path+'" onerror="$JSI.loadError.apply(this)"><\/script>');
			}
		}else if(from){
			notifySet[from]=1;
		}
	}
	$import = function (path,target){
		var task = taskMap[path];
		if(!task){
			task = taskMap[path] = [];
		}
		var a = typeof target == 'function';
		task.push(a?target:function(result){
				copy(result,target ||this);
			});
		console.log(path)
		async = a;
		load(path);
	}
	/**
	 * $JSI.define('path',['deps'],function(require,exports){...})
	 */
	return {
		scriptBase:/scripts/,
		loadError:function(){
			//load error
			console.log('load error:'+this.src)
		},
		require : require,
		define : define
	}
}();
