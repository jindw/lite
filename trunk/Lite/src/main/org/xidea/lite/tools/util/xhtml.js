function textFilterXHTML(path,text){
	//在模板domFilter中精准处理
	//text = replacePath(text);
	return text;
}
function domFilterXHTML(path,dom){
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
				value = processJS(value);
			}else if(tagName == 'style'){
				value = processCSS(value);
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
				item.value = processCSS(value);
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
					value = processJS(value);
					item.value = value;
				}else{
					
				}
				continue;
			}
			//这里会一起处理replacePath
			value = processURI(value)
			if(hash){
				value = addHashData(value);
			}
			if(contextPath){
				value = addContextPath(value,contextPath);
			}
			item.value = value;
		}
	}
	return dom;
}