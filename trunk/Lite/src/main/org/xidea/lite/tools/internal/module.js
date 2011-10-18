var exportsMap = {};
function require(absPath){
	var exports = exportsMap[absPath];
	if(!exports){
		var source = resourceManager.loadScript(absPath+'.js');
		if(!source){
			$log.error(absPath+'.js not found')
		}
		var loader = resourceManager.eval('(function(require,exports){'+source+'\n})',absPath+'.js')
		//var loader = new Function('require','exports',''+source);
		exports = exportsMap[absPath]={};
		loader(function(path){
			if(/^\./.test(path)){
				path =absPath.replace(/[^\/]+$/,'')+path
				while(path != (path = path.replace(/(?:[^\/]+(\/)\.)\.[\/]/,'$1')));
			}
			return require(path);
		},exports);
	}
	return exports;
}
