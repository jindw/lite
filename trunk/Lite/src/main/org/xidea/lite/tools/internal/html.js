//导入调试服务器编译上下文
var Env = require("./env");

var inc = 0;
function scFilter(path,text){
	return cssFilter(jsFilter(text,path));
}
function cssFilter(text,path){
	return text.replace(/(<style\b[^>]*>)([\s\S]*?)<\/style>|<!\[CDATA\[([\s\S]*?)\]\]>/g,
		function(a,prefix,css){
			if(css){
				var css = Env.doFilter(path+'#'+ inc++ +'.js',css);
				return prefix+css+'</style>'
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
				var js = Env.doFilter(path+'#'+ inc++ +'.js',js);
				return prefix+js+'</script>'
			}else{
				//$log.error(a)
				return a;
			}
		}
	);
}
exports.scFilter = scFilter;