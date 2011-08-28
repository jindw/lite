var DataView = {
	defaultModel:'/WEB-INF/litecode/mock.json',
	render:function(serviceBase,templateModel,featureMap){
		this.serviceBase = serviceBase.replace(/^\//,location.href.replace(/([^\:\/])\/[\s\S]*/,'$1/'));
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
					+"<input type='button' value='删除缓存' onclick='DataView.refresh()'/>"
					+"<input type='button' value='测试模板' onclick='DataView.reload("+stringifyJSON(models)+")'/>"
				+"</div>");
		}else{
			var id = toname(this.defaultModel);
			document.write("<textarea autocomplete='off' id='"+id+"' style='width:99%;height:80%' onchange='DataView.checkJSON(this.value,this)'>"+encode(model)+"</textarea>");
			document.write("<div>"
					+"<input type='button' value='删除缓存' onclick='DataView.refresh()'/>"
					+"<input type='button' value='修改数据' onclick='DataView.mockModel([\""+this.defaultModel+"\"])'/>"
				+"</div>");
		}
	},
	refresh:function(){
		debugReload('refresh');
	},
	reload:function(){
		location.reload();
	},
	save:function(path,data,callback){
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function(){
			if(xhr.readyState == 4){
				callback(xhr);
				xhr = null;
			}
		};
		xhr.open("POST",this.serviceBase,true);
		var data = "LITE_ACTION=save&LITE_PATH="+encodeURIComponent(path)+"&data="+base64Encode(data);
		xhr.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
		xhr.setRequestHeader('Accept','text/javascript, */*');
		xhr.send(data);
	},
	mockModel:function(models){
		var thiz = this;
		var debug = this.defaultModel == models[0]?'model;'+this.serviceBase+''+this.defaultModel:'model';
		function callback(xhr){
			var fn = models.pop();
			if(fn){
				var data = document.getElementById(toname(fn)).value;
				thiz.save(fn,data,callback)
			}else{
				debugReload(debug);
			}
		}
		callback();
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