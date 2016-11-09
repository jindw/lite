var editorMap = {};
function renderSource(source,model,path){
	if(!path){
		path = '_'+new Date().getTime()+'_'+(Math.random()*100 |0);
	}
	if(model){
		document.write("<div class='runner' onclick=\"editorMap['"+path+"'].showExample()\">执行</div>");
	}
	editorMap[path] = (new CodeEditor(source,model,path));
}


function placeMirror(node){
	var s = document.getElementsByTagName('script');
	var s = s[s.length-1];
	s.parentNode.appendChild(node);
}

function CodeEditor(source,model,path){
	this.source = source;
	this.model = model;
	this.path = path;
	this.editor = CodeMirror(placeMirror, {
		value: source,
		//readOnly :true,
		mode:  {name:/^\s*</.test(source)?"litexml":"javascript",json:true}
	});
}
CodeEditor.prototype.getValue = function(){
	return this.editor.getValue();
}
CodeEditor.prototype.showExample = function(){
	showExample(this.model,this.source)
}
function updateMirror(){
	for(var n in editorMap){
		editorMap[n].editor.refresh();
	}
}
function showNext(thiz,n,show,hide){
	while(n = n.nextSibling){
		if(n.nodeType == 1){
			if(n.style.display=='none'){
				n.style.display = 'block';
				thiz.innerHTML = hide;
				updateMirror();
			}else{
				n.style.display = 'none';
				thiz.innerHTML = show;
			}
			break;
		}
	}
}


var resultEditor = CodeMirror(placeMirror, {
	value: '',
	readOnly:true,
	lineNumbers: true,
	mode: {name:"litexml"}
});

var exampleRunner;
var exampleResult
function closeExample(){
	exampleRunner.parentNode.style.display = 'none';
}	
function showExample(json,template){
	exampleRunner = document.getElementById("exampleRunner");
	exampleResult = document.getElementById("exampleResult");
	exampleRunner.parentNode.style.display = 'block';
	exampleResult.style.display = 'none';
	if(typeof json != 'string'){
		json = JSON.stringify(json);
	}
	jsonEditor.setValue(json);
	templateEditor.setValue(template);
	var runButton = document.getElementById("runButton");
	runButton.disabled=true;
}
function buildContext(){
	var context = new ParseContext();
	var cached = {};
	for(var path in xmlSourceMap){
		cached[path] = xmlSourceMap[path].getValue();
	}
	cached["/source.xhtml"] = templateEditor.getValue();
	var baseXMLLoader = context.loadXML;
	context.loadXML = function(uri){
		if(uri.path){
			if(uri.path in cached){
				uri = cached[uri.path];
			}else{
				console.warn("未知文件路径",uri.path)
			}
		}
		return baseXMLLoader.call(context,uri);
	}
	context.parse(context.createURI('/source.xhtml'));
	return context;
}
function compileToJS(){
	try{
		var context = buildContext();
		var litecode = context.toList();
		var translator = new JSTranslator();//'.','/','-','!','%'
		translator.liteImpl = 'liteImpl';//avoid inline jslib 
		var jscode = translator.translate(litecode);
	}catch(e){
		console.error("测试失败：模板编译异常：",e);
		return;
	}
	showResult(jscode);
	updateResultRunner('js',litecode,jscode);
}
function compileToNodeJS(){
	try{
		var context = buildContext();
		var litecode = context.toList();
		var prefix = extractStaticPrefix(litecode);
		
		var translator = new JSTranslator();
		translator.liteImpl = 'liteImpl';
		var jscode = translator.translate(litecode);
	}catch(e){
		console.error("测试失败：模板编译异常：",e);
		return;
	}
	var nodecode = ['[(function(liteImpl){return (',jscode,')}),',
				stringifyJSON(context.config),',',
				stringifyJSON(prefix),']'].join('');
	showResult(nodecode);
	updateResultRunner('NodeJS',litecode,nodecode);
}
function compileToPHP(){
	try{
		var context = buildContext();
		var litecode = context.toList();
		var pt = new PHPTranslator("/test.xhtml".replace(/[\/\-\$\.!%]/g,'_'));//'.','/','-','!','%'
		var phpcode = pt.translate(litecode);
	}catch(e){
		console.error("测试失败：模板编译异常：",e);
		return;
	}
	showResult(phpcode);
	updateResultRunner('php',litecode,phpcode);
}
function compileToLite(){
	try{
		var context = buildContext();
		var litecode = context.toList();
		var litecode = stringifyJSON(litecode);
	}catch(e){
		console.error("测试失败：模板编译异常：",e);
		return;
	}
	showResult(litecode);
	updateResultRunner('java',litecode,null);
}
function updateResultRunner(type,litecode,runcode){
	runStatus = {type:type.toLowerCase(),litecode:litecode,runcode:runcode}
	var runButton = document.getElementById("runButton");
	runButton.disabled=false
	runButton.value="运行("+type+")"
}
var runStatus = {}
function runTemplate(){
	try{
		var js = jsonEditor.getValue();
		var data = window.eval('('+js+')');
	}catch(e){
		console.error("测试失败：模拟数据异常："+e);
		return;
	}

	if(runStatus.type == 'js'){
		try{
			var code = runStatus.runcode;
			var tpl =  window.eval("("+(code||null)+")");
			//alert(code)
			
			var result = tpl(data);
		}catch(e){
			console.error("测试失败：模板运行异常："+tpl+e);
			return;
		}
		showResult(result);
	}else {
		var loader = document.getElementById('resultLoader');
		loader.style.display = 'block';
		try{
			if(runStatus.type == 'php'){
				var code = runStatus.runcode+'\nlite_template_test_xhtml(json_decode('+stringifyJSON(stringifyJSON(data))+',true));';
				var url = 'http://litetest.sinaapp.com/?php='+encodeURIComponent(code)+'&t='+ +new Date();
				loader.innerHTML = '<h2>load php result ....</h2><p>php 预览环境由新浪appengine提供。<a href="'+url+'">手动预览</a></p>';
			
			}else if(runStatus.type == 'java'){
				var code = runStatus.litecode;
				var model = stringifyJSON(data);
				var url = 'http://www.xidea.org:1981/lite/doc/runner.jsp?code='
					+encodeURIComponent(code)+'&model='
					+encodeURIComponent(model)+'&'+ +new Date();
				loader.innerHTML = '<h2>load java result ....</h2><p>java 预览环境由满江红开源组织提供。<a href="'+url+'">手动预览</a></p>';
			}else if(runStatus.type == 'nodejs'){
				var code = runStatus.runcode;
				var model = stringifyJSON(data);
				var url = 'http://www.xidea.org:1985/runner?code='
					+encodeURIComponent(code)+'&model='
					+encodeURIComponent(model)+'&'+ +new Date();
				loader.innerHTML = '<h2>load nodejs result ....</h2><p>nodejs 预览环境由满江红开源组织提供。<a href="'+url+'">手动预览</a></p>';
			}
		}catch(e){
		}
		var s = document.createElement('script');
		s.src = url +'&callback=showResult'
		loader.appendChild(s);
	}
}
function showResult(result){
	var resultSource = document.getElementById('resultSource');
	exampleResult.style.display = 'block';
	resultEditor.setValue(result);
	var n = 0;
	var i = setInterval(function a(){
		resultSource.style.opacity = n/10;
		if(n++>10){
			document.getElementById('resultLoader').style.display = 'none'
			resultSource.style.opacity = 1;
			clearInterval(i);
		}
	},100)
}

CodeMirror.defineMode("litexml", function(config, parserConfig) {
  var litexmlOverlay = {
    token: function(stream, state) {
      if (stream.match("c:")) {
        while ((ch = stream.next()) != null)
          if (!/\w/.test(ch)) break;
        return "lite-tag";
      }else if (stream.match(/^(?:\$\!?\{)/)) {
        while ((ch = stream.next()) != null)
          if (ch == "}") break;
        return "lite-el";
      }
      while (stream.next() != null && !stream.match(/^\$\!?\{/, false)) {}
      return null;
    }
  };
  return CodeMirror.overlayParser(CodeMirror.getMode(config, parserConfig.backdrop || "xml"), litexmlOverlay);
});