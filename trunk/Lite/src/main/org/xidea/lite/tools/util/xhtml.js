function streamFilterPNG(data){
	return data;
}
function textFilterJS(text){
	text = processJS(this,text);
	return text;
}
function textFilterCSS(text){
	text = processCSS(this,text);
	return text;
}
function textFilterXHTML(text){
	//在模板domFilter中精准处理
	//text = replacePath(this,text);
	return text;
}
function domFilterXHTML(dom){
	var xpath="//*[local-name()='script' || local-name()='style']|//@*[starts-with(local-name(),'on') or local-name()='style' or local-name()='href' or local-name()='src' or local-name()='action'  ]";
	var nodes = selectNodeByXPath(dom,xpath);
	var hash = true;
	var contextPath= null;
	
	for(var i=0,len = nodes.length;i<len;i++){
		var item = nodes.item(i);
		if(item.nodeType == 1){
			//script|style
			var tagName = item.tagName;
			var value = item.textContent;
			if(tagName == 'script'){
				value = processJS(this,value);
			}else if(tagName == 'style'){
				value = processCSS(this,value);
			}else{
				$log.warn('unknow tag:'+tagName);
			}
			item.textContent = value;
		}else{
			var attrName = item.localName;
			var value = item.value;
			var node = item.ownerElement;
			var tagName = node.tagName;
			switch(attrName){
			case 'style':
				item.value = processCSS(this,value);
				continue;
			case 'href':
				if(tagName=='link'){
					var rel = node.getAttribute("rel");
					var type = node.getAttribute("type");
					if (/\/css$/i.test(type) || /^stylesheet$/i.test(rel)) {
						
					}else{
						continue;
					}
				}else if(/^a/i.test(tagName)){
					hash = false;
				}else{
					$log.warn("unknow href:");
					continue;
				}
				break;
			case 'action':
				if(/^form$/i.test(tagName)){
					hash = false;
				}else{
					$log.warn("unknow action:");
					continue;
				}
				break;
			case 'src':
				if(/^(?:script|img|button)$/i.test(tagName)){
				}else if(/^(?:a|frame|iframe)$/i.test(tagName)){
					hash = false;
				}else{
					$log.warn("unknow src:");
					continue;
				}
				break;
			default://on
				if(/^on.+/.test(attrName)){
					value = processJS(this,value);
					item.value = value;
				}else{
					
				}
				continue;
			}
			//这里会一起处理replacePath
			value = processURI(value)
			if(hash){
				value = addHashData(this,value);
			}
			if(contextPath){
				value = addContextPath(this,value,contextPath);
			}
			item.value = value;
		}
	}
	return dom;
}
function processJS(manager,text){
	//TODO:merge...
	//replace js:	encodeURI("/module/static/img/a/_/8.png")
	text = replacePath(this,text);
	//TODO:autoEncodeScript
	//${..} == > ${JSON.stringify(..)} ==> /$(tghjk)/
	//compress
	//manager.compressJS(value);
	return text.replace(/(\\(?:\r\n?|\n).)|^\s+/m,'$1')
}
function processCSS(manager,text){
	//TODO:merge,absolute uri
	//replace css:	url("/module/static/img/a/_/8.png")
	text = replacePath(this,text);
	//compress 
	//manager.compressCSS(value);
	return text.replace(/(\\(?:\r\n?|\n).)|^\s+/m,'$1')
}
function processURI(manager,path){
	//replace exact
	path = replacePath(manager,path);
	//TODO:autoEncode
	//${..} == > ${encodeURIComponent(..)} 
	return path;
}
function replacePath(manager,path){
	//exact: /module/static/img/a/_/8.png
	//manager.replacePath(path);
	return path;
}
function addHashData(manager,path){
	if(path.indexOf('?')>=0){
		return path;//not add hash on dynamic
	}
	if(path.indexOf('#')>=0){
		return path;//not add hash on hash url
	}
	//TODO:use result md5 is better?
	//manager.getContentHash(path);
	return path+'?@='+(+new Date()).toString(32)
}
/**
 * 这个工作比较难啊，模板要定位所有资源路径。
 * js/css中通过规范解决（css 资源都用url(...),js 资源地址都用encodeURI(...)），
 */
function addContextPath(manager,path,contextPath){
	if(path.charAt() == '/'){
		return contextPath + path;
	}
	return path;
}