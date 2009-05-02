var TestCase = {
	runTest:function (){
	    var context = document.getElementById("context").value;
	    var templateSource = E("templateSource").value;
	    try{
	        context = JSON.decode(context);//JSON.decode(context);
	    }catch(e){
	        $log.error("数据源解析失败",e);
	        return false;
	    }
	    
	    try{
	    	var parser = new XMLParser(true);
	    	parser.parse(templateSource);
	    	var jsCode = parser.buildResult();
	        var jsTemplate = new Template(jsCode);
	        jsCode = jsCode.toString()
	    }catch(e){
	        $log.error("模板解析失败（JS）",e);
	        return false;
	    }
	    try{
	    	var parser = new XMLParser(false);
	    	parser.parse(templateSource);
	    	var jsonCode = parser.buildResult();
	        var jsonTemplate = new Template(jsonCode);
	    }catch(e){
	        $log.error("模板解析失败（JSON）",e);
	        return false;
	    }
	    E("templateCode").value = liteFormat(jsonCode,false);
	    E("optimizedResult").value = jsCode;
	    
	    var templateResult = E("templateResult");
	    var htmlResult = E("htmlResult");
	    var step = 5;
	    var interval = setInterval(function(){
	    	if(step--){
	    		templateResult.style.backgroundColor = (step&1)?'yellow':""
	    	}else{
	    		clearInterval(interval)
	    	}
	    	
	    },300);
	    var testCount = 100;
	    var jsTime
	    try{
	        var t1 = new Date();
	        var count = testCount;
	        while(count--){
	            var jsResult = jsTemplate.render(context);
	        }
	        jsTime = new Date()-t1;
	    }catch(e){
	        $log.error("Lite JS优化版本模板渲染错误",e)
	    }
	    try{
	        var t1 = new Date();
	        var count = testCount;
	        while(count--){
	            var jsonResult = jsonTemplate.render(context);
	        }
	        $log.info("模版消耗时间（？毫秒/"+testCount+"次）：",
	        "jsTime:"+jsTime,
	        "json:"+(new Date()-t1))
	        
	        templateResult.value = jsonResult;
	    }catch(e){
	        $log.error("Lite JSON模板渲染错误",e)
	    }
	    var doc = htmlResult.contentWindow.document;
	    doc.open();
	    doc.write('<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">')
	    doc.write(jsResult);
	    doc.close();
	    try{
    	    var a = doc.getElementsByTagName("a");
    	    var i = a && a.length;
    	    while(i--){
    	        a[i].target="_blank";
    	    }
    	}catch(e){}
	    if(jsResult!=jsonResult){
	        $log.error("优化后的jslite与优化前渲染结果有差别！！！","JS:"+jsResult,"JSON:"+jsonResult)
	    }
	    try{
	        templateResult.scrollIntoView();
	    }catch(e){}
	},
	initialize:function(){
		var url = window.location.href;
		url = url.replace(/[^\/]*$/,"menu.xml");
		var xhr = new XMLHttpRequest();
		xhr.open("GET",url,true);
		xhr.onreadystatechange = function(){
			if(xhr.readyState == 4){
				var xml = xhr.responseXML;
				if(window.ActiveXObject && !xml.xml && xhr.responseText){
					var xml = new ActiveXObject("Microsoft.XMLDOM");
					xml.loadXML(xhr.responseText);
				}
				updateData(xml)
				xhr = null;
			}
		}
		xhr.send("");
	},
	prepare:function(thiz,key){
		E("description").innerHTML = data[key]["description"];
		E("context").value = data[key]["context"];
		E("templateSource").value = data[key]["source"];
		
		var nodes = thiz.parentNode.childNodes;
		var i = nodes.length;
		while(i--){
			var node = nodes[i];
			if(node == thiz){
				node.className = "selected"
			}else{
				node.className = ""
			}
		}
	},
	liteFormat:function(text){
	    confirm(liteFormat(JSON.decode(text),true));
	}
}
var data;
function E(id){
	return document.getElementById(id);
}
function getText(parentNode,tagName){
	var node = parentNode.firstChild;
	while(node!=null){
		if(node.tagName == tagName){
			var node = node.firstChild;
			var buf = [];
			while(node!=null){
				buf.push(node.data);
				node = node.nextSibling;
			}
			return buf.join('');
		}
		node = node.nextSibling;
	}
}

function updateData(doc){
	var entrys = doc.getElementsByTagName("entry");
	var defaultContext = getText(doc.documentElement,"context")||"{}"
	var buf = [];
	data = {};
	for(var i=0;i<entrys.length;i++){
		var entry = entrys[i];
		var context = getText(entry,"context") || defaultContext;
		var source = getText(entry,"source");
		var description = getText(entry,"description");
		var key = entry.getAttribute("key");
		data[key] = {
			"context":context.replace(/^\s+|\s+$/g,''),
			"source":source.replace(/^\s+|\s+$/g,''),
			"description":description.replace(/^\s+|\s+$/g,'')
		}
		buf.push("<li onclick='TestCase.prepare(this,"+JSON.encode(key)+")'>"+key+"</li>")
	}
	//alert(buf.join(""))
	E("menuContent").innerHTML = buf.join("");
}