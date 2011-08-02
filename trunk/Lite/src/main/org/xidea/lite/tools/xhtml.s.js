function textFilterXHTML(path,text){
	//在模板domFilter中精准处理
	//text = replacePath(text);
	return Packages.org.xidea.lite.impl.ParseUtil.normalize(text,path);
}
function domFilterXHTML(path,dom){
	var xpath="//*[local-name()='script' or local-name()='style'] | //@*[starts-with(local-name(),'on') or local-name()='style' or local-name()='href' or local-name()='src' or local-name()='action'  ]";
	var nodes = selectByXPath(dom,xpath);
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
			
			while(item.firstChild){
				item.removeChild(item.firstChild);
			}
			//TODO:add cdata
			var doc = item.ownerDocument;
			if(/[<>&]/.test(value) && value.indexOf("<![CDATA[")<0){
				var cdata = doc.createCDATASection("*/"+value+'\n/*');
				item.appendChild(doc.createTextNode("/*"))
				item.appendChild(cdata);
				item.appendChild(doc.createTextNode("*/"))
			}else{
				item.appendChild(doc.createTextNode(value));
			}
		}else{
			var attrName = item.localName;
			var value = item.value;
			var node = item.ownerElement;
			var tagName = node.tagName;
			switch(attrName){
			case 'style':
				value =  processCSS('style{'+value+'}');
				value = value.substring(value.indexOf('{')+1,value.lastIndexOf('}'))
				item.value = value;
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
					value = processJS('function(){'+value+'}');
					value = value.substring(value.indexOf('{')+1,value.lastIndexOf('}'))
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