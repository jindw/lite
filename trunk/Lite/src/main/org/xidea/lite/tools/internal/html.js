//导入调试服务器编译上下文
var Env = require("./env");

function scriptCssFilter(path,text){
	return cssFilter(jsFilter(text,path));
}
function cssFilter(text,path){
	return text.replace(/(<style\b[^>]*>)([\s\S]*?)<\/style>|<!\[CDATA\[([\s\S]*?)\]\]>/g,
		function(a,prefix,css){
			if(css){
				return prefix+processCSS(css,path)+'</style>'
			}else{
				return a;
			}
		}
	);
}
function jsFilter(text,path){
	return text.replace(/<script\b[^>]*\/>|(<script\b[^>]*>)([\s\S]*?)<\/script>|<!\[CDATA\[([\s\S]*?)\]\]>/g,
		function(a,prefix,js){
			if(js){
				//$log.error(js)
				if(/\/>/.test(prefix)){
					return prefix+jsFilter(js+'</script>',path)
				}
				return prefix+processJS(js,path)+'</script>'
			}else{
				//$log.error(a)
				return a;
			}
		}
	);
}
exports.scriptCssFilter = scriptCssFilter;