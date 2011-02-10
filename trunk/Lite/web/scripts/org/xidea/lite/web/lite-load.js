var liteImpl = "http://lite.googlecode.com/svn/publish/script/lite-compiler.js";
function liteInitialize(){
	if(/\bLITE_COMPILER=true\b/.test(document.cookie)){
		document.write("<script src='"+impl+"'></script>");
		liteImpl = null;
	}
}
function liteLoad(){
	var ss = document.getElementsByName("script");
	var i = ss.length;
	while(i--){
		var s = ss[i];
		if(s.type == 'text/x-lite'){
			initTemplate(s);
		}
	}
}
function initTemplate(s){
	var n = s.name||s.id;
	var source = s.text;
	var m = n.match(/([\$_\w]+)\s*(?:\(\s*(.*?)\s*\))?/);
	var args = m[2];
	n = m[1];
	if(args != null){
		args = args?args.split(/\s*,\s*/):[];
	}
	if(liteImpl){
		liteImpl = null;
		var s2 = document.createElement("script");
		s2.src = liteImpl;
		src.parentNode.insertBefore(s2,s);
	}
	if(args){
		window[n] = buildFunction(source,args);
	}else{
		window[n] = buildTemplate(source);
	}
}
function buildFunction(s,args,f){
	return function(){
		if(f == null){
			_reloadFN(typeof liteFunction);
			f = liteFunction(s,args);
		}
		return f.apply(this,arguments)
	}
}
function buildTemplate(source,o){
	return {
		render:function(){
			if(o!=null){
				_reloadFN(typeof liteTemplate);
				o = liteTemplate(source);
			}
			return o.render.apply(o,arguments);
		}
	}
}
function _reloadFN(type){
	if(type != 'function'){
		document.cookie = "LITE_COMPILER=true";
		if(prompt("模板未编译，不能成功运行，尝试自动编译（调试期间有效）？")){
			location.reload();
		}
	}
}