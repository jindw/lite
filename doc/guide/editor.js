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

	if(/nodejs|js|javascript/.test(runStatus.type)){
		try{
			var code = runStatus.runcode;
			var tpl =  window.eval("("+(code||null)+")");
			var out = [];
			var resp = {
				write:function(t){
					out.push(t)
				},
				end:function(){
					
					showResult(out.join(''));
				}
			}
			var nodejsMock = wrapResponse(resp,tpl);
			var result = tpl(data,nodejsMock);
		}catch(e){
			console.error("测试失败：模板运行异常："+tpl+e);
			return;
		}
	}else {
		var loader = document.getElementById('resultLoader');
		loader.style.display = 'block';
		console.log(runStatus.type)
		if(true){
			showResult('无模拟环境，无法测试运用...');
			return;
		}
		try{
			if(runStatus.type == 'php'){
				var code = runStatus.runcode+'\nlite_template_test_xhtml(json_decode('+JSON.stringify(JSON.stringify(data))+',true));';
				var url = 'http://litetest.sinaapp.com/?php='+encodeURIComponent(code)+'&t='+ +new Date();
				loader.innerHTML = '<h2>load php result ....</h2><p>php 预览环境由新浪appengine提供。<a href="'+url+'">手动预览</a></p>';
			
			}else if(runStatus.type == 'java'){
				var code = runStatus.litecode;
				var model = JSON.stringify(data);
				var url = 'http://www.xidea.org:1981/lite/doc/runner.jsp?code='
					+encodeURIComponent(code)+'&model='
					+encodeURIComponent(model)+'&'+ +new Date();
				loader.innerHTML = '<h2>load java result ....</h2><p>java 预览环境由满江红开源组织提供。<a href="'+url+'">手动预览</a></p>';
			}
		}catch(e){
			console.error(e)
		}
		var s = document.createElement('script');
		s.src = url +'&callback=showResult'
		loader.appendChild(s);
	}
}
function showResult(result){
	var resultSource = document.getElementById('resultSource');
	exampleResult.style.display = 'block';
	resultEditor.setValue(result||'结果生成失败...');
	var n = 0;
	var i = setInterval(function a(){
		resultSource.style.opacity = n/10;
		if(n++>10){
			document.getElementById('resultLoader').style.display = 'none'
			resultSource.style.opacity = 1;
			clearInterval(i);
		}
	},30)
}


if(typeof CodeMirror == 'function'){
var exampleRunner;
var exampleResult;
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

}