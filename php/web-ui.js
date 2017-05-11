var base64Encode=require('lite/parse/resource').base64Encode;
var DataView = {
	render:function(path,templateModel,featureMap,serviceBase){
		this.path = path;
		this.modelPath = path.replace(/.\w+$/,'.json');
		this.serviceBase = serviceBase.replace(/^\//,location.href.replace(/([^\:\/])\/[\s\S]*/,'$1/'));
		this.remoteModel = serviceBase == this.serviceBase;
		//.replace('//localhost/','//127.0.0.1/'));
		var model = JSON.stringify(templateModel,'\t');
		document.write("<style>body{background:#ddd;}.data-view-skin{float:left;}"+
			"textarea.model{background:#333;width:98%;height:80%;color:#eee;font-weight:bold;}"+
			".toolbar{text-align:right;background:#EEE;width:98%;padding:2px;border:1px outset #333333}"+
			"</style>");
		if(model instanceof Array){
			//文件集合
			//[[file1,source1],["file2","source2"]
			var len = model.length;
			var models = [];
			document.write("<div class='toolbar'>"
					+"<div class='data-view-skin'>切换皮肤</div>"
					+"<input type='button' value='刷新缓存' onclick='DataView.refresh()'/>"
					+"<input type='button' value='测试模板' onclick='DataView.reload("+JSON.stringify(models)+")'/>"
				+"</div>");
			for(var i=0;i<len;i++){
				var item = model[i];
				var id = toname(item[0]);
				models.push(item[0]);
				document.write("<div class='modelbar'>"
					+"<label>"
					+"<strong>"+item[0]+"</strong>"
					+"<input type='button' value='修改数据' onclick='DataView.save("+JSON.stringify(item[0])+","+id+")'/>"
				+"</label></div>");
				document.write("<textarea class='model' autocomplete='off' id='"+
					id+"' style='height:"+parseInt(90 / len)+
					"%' onchange='DataView.checkJSON(this.value,this)'>"+
					encode(item[1])+"</textarea>");
			}
		}else{
			var id = toname(this.modelPath);
			document.write("<div id='toolbar' class='toolbar'>"
					+"<div class='data-view-skin'>当前数据视图实现 ["+(getCookie(LITE_MODEL_VIEW_IMPL) || '系统默认')+"]<a href='#' onclick='DataView.setViewImpl();return false'>自定制</a></div>"
					+"<input type='button' value='修改数据' onclick='DataView.mockModel([\""+this.modelPath+"\"])'/>"
					+"<input type='button' value='刷新缓存' onclick='DataView.refresh()'/>"
					+"<input type='button' value='查看源码' onclick='DataView.source()'/>"
				+"</div>");
			document.write("<textarea class='model' autocomplete='off' id='"+id+
					"' onchange='DataView.checkJSON(this.value,this)'>"+
					encode(model)+"</textarea>");
			
		}
	},
	refresh:function(){
		debugReload('refresh');
	},
	reload:function(){
		location.reload();
	},
	source:function(){
		debugReload('source');
	},
	save:function(path,data,callback){
		this.remoteModel = true;
		data = "LITE_ACTION=save&LITE_PATH="+encodeURIComponent(path)+"&LITE_DATA="+base64Encode(data);
		if(this.remoteModel){
			remoteSave(this.serviceBase,data,callback);
		}else{
			xhrSave(this.serviceBase,data,callback)
		}
	},
	mockModel:function(models){
		var thiz = this;
		var len = models.length;
		var debug = this.defaultModel == models[0]?+this.defaultModel:'model';
		function callback(xhr,remote){
			var fn = models.pop();
			if(fn){
				var data = document.getElementById(toname(fn)).value;
				thiz.save(fn,data,callback)
			}else{
				if(len == 1){//callback
					if(remote){
						var path = xhr.path;
					}else{
						var path = window.eval('('+xhr.responseText+')').path;
					}
					debug = 'model;'+thiz.serviceBase+'?LITE_ACTION=load&LITE_PATH='+encodeURIComponent(path)
					debugReload(debug);
				}else{
					location.reload();
				}
				
				
			}
		}
		len && callback();
	},
	setViewImpl:function(){
		var help = "(视图定制参考见:http://www.xidea.org/lite/doc/index.php/guide/dev-data-view.xhtml)";
		var currentImpl = getCookie(LITE_MODEL_VIEW_IMPL)
		var impl = prompt("请输入视图实现JavaScript脚本(为空表示回复默认视图):\n"+help,currentImpl||'');
		if(impl == null){
			confirm('修改取消');
			return;
		} else if ((currentImpl||'') == impl){
			confirm('未作修改');
			return;
		}
		if(impl){
			var date = new Date(+new Date()*2);
		}else{
			var date = new Date(0);
		}
		document.cookie = 
			LITE_MODEL_VIEW_IMPL+'='+encodeURIComponent(impl)+';expires='+date.toGMTString();
		var msg = "刷新后将附加新的视图实现脚本!\n\n"+help;
		if(impl){
			var rtv = confirm("视图Cookie设置成功!附加脚本("+impl+")将用来扩展视图功能,点击确认刷新页面.\n"+msg )
		}else{
			var rtv = confirm("视图Cookie删除成功,将只采用默认视图脚本,点击确认刷新页面!\n"+msg )
		}
		if(rtv){
			debugReload("model");
		}
	}
}
var LITE_MODEL_VIEW_IMPL = 'LITE_MODEL_VIEW_IMPL';
function getCookie(key){
	var c = document.cookie;
	var r = c.replace(new RegExp('^(?:.*;)?'+key+'=([^;]+).*$'),'$1');
	if(c != r){
		return decodeURIComponent(r);
	}
}
function toname(path){
	return encodeURIComponent(path).replace(/%/g,'$')
}
function debugReload(debug){
	document.cookie = 'LITE_DEBUG='+encodeURIComponent(debug)+';expires='+new(Date)(+new(Date)+1000).toGMTString();
	location.reload();
}
function encode(code){
	return code.replace(/&/g,'&amp;').replace(/</g,'&lt;');
}
function remoteSave(serviceBase,data,callback){
	DataView.callback = callback;
	var script = document.createElement('script');
	script.src = this.serviceBase+'?'+data+'&LITE_CALLBACK=DataView.callback';
	document.body.appendChild(script)
	return;
}
function xhrSave(serviceBase,data,callback){
	var xhr = new XMLHttpRequest();
	xhr.onreadystatechange = function(){
		if(xhr.readyState == 4){
			callback(xhr);
			xhr = null;
		}
	};
	xhr.open("POST",serviceBase,true);
	xhr.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
	xhr.setRequestHeader('Accept','text/javascript, */*');
	xhr.send(data);
}
exports.DataView=DataView;
