var DataView = {
	render:function(path,templateModel,featureMap,serviceBase){
		this.path = path;
		this.modelPath = path.replace(/.\w+$/,'.json');
		this.serviceBase = serviceBase.replace(/^\//,location.href.replace(/([^\:\/])\/[\s\S]*/,'$1/'));
		this.remoteModel = serviceBase == this.serviceBase;
		//.replace('//localhost/','//127.0.0.1/'));
		var model = stringifyJSON(templateModel,'\t');
		if(model instanceof Array){
			//文件集合
			//[[file1,source1],["file2","source2"]
			var len = model.length;
			var models = [];
			for(var i=0;i<len;i++){
				var item = model[i];
				var id = toname(item[0]);
				models.push(item[0]);
				document.write("<div>"
					+"<label>"
					+"<strong>"+item[0]+"</strong>"
					+"<input type='button' value='修改数据' onclick='DataView.save("+stringifyJSON(item[0])+","+id+")'/>"
				+"</label></div>");
				document.write("<textarea autocomplete='off' id='"+
					id+"' style='width:99%;height:"+parseInt(90 / len)+
					"%' onchange='DataView.checkJSON(this.value,this)'>"+
					encode(item[1])+"</textarea>");
			}
			document.write("<div>"
					+"<input type='button' value='刷新缓存' onclick='DataView.refresh()'/>"
					+"<input type='button' value='测试模板' onclick='DataView.reload("+stringifyJSON(models)+")'/>"
				+"</div>");
		}else{
			var id = toname(this.modelPath);
			document.write("<textarea autocomplete='off' id='"+id+"' style='width:99%;height:80%' onchange='DataView.checkJSON(this.value,this)'>"+encode(model)+"</textarea>");
			document.write("<div>"
					+"<input type='button' value='修改数据' onclick='DataView.mockModel([\""+this.modelPath+"\"])'/>"
					+"<input type='button' value='刷新缓存' onclick='DataView.refresh()'/>"
					+"<input type='button' value='查看源码' onclick='DataView.source()'/>"
				+"</div>");
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