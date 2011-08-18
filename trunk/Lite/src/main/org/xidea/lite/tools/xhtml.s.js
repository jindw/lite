function textFilterXHTML(path,text){
	//在模板domFilter中精准处理
	//text = replacePath(text);
	return Packages.org.xidea.lite.impl.ParseUtil.normalize(text,path);
}
function checkEmptyImg(dom){
	//检测空img属性
	var emptyImgs = selectByXPath(dom,"//xhtml:img[@src='']");
	var i = emptyImgs.length;
	if(i){
		var buf = [];
		while(i--){
			buf.push(getNodePosition(emptyImgs.item(i)));
		}
		$log.error("img 标签不能带空 src属性[性能问题:123]:\n"+buf.join('\n'))
	}
}
function checkUnknowTag(dom){
	var elements = 'h1|h2|h3|h4|h5|h6|h7|a|abbr|acronym|address|area|article|aside|audio|b|base|bdo|big|blockquote|body|br|button|canvas|caption|cite|code|col|colgroup|command|datagrid|datalist|datatemplate|dd|del|details|dfn|dialog|div|dl|dt|em|embed|event|fieldset|figure|footer|form|frame|frameset|h1|head|header|hr|html|i|iframe|img|input|ins|kbd|label|legend|li|link|m|map|meta|meter|nav|nest|noframes|noscript|object|ol|optgroup|option|output|p|param|pre|progress|q|rule|samp|script|section|select|small|source|span|strong|style|sub|sup|table|tbody|td|textarea|tfoot|th|thead|time|title|tr|tt|ul|var|video';
	var deprecatedElements = selectByXPath(dom,"//xhtml:*[not(contains('|"+elements+"|',concat('|',name(),'|')))]");
	var i = deprecatedElements.length;
	if(i){
		var buf = [];
		while(i--){
			var el = deprecatedElements.item(i);
			buf.push(el.tagName+':'+getNodePosition(el));
		}
		$log.warn("网页中使用了未知或不推荐的 html标签:\n"+buf.join('\n'))
	}
}
function checkUnknowAttr(dom){
	var attributes = 'abbr|accept|accept-charset|accesskey|action|align|alink|alt|archive|axis|background|bgcolor|border|cellpadding|cellspacing|char|charoff|charset|checked|cite|class|classid|clear|code|codebase|codetype|color|cols|colspan|compact|content|coords|data|datetime|declare|defer|dir|dir|disabled|enctype|face|for|frame|frameborder|headers|height|href|hreflang|hspace|http-equiv|id|ismap|label|lang|language|link|longdesc|marginheight|marginwidth|maxlength|media|method|multiple|name|nohref|noresize|noshade|nowrap|object|onblur|onchange|onclick|ondblclick|onfocus|onkeydown|onkeypress|onkeyup|onload|onmousedown|onmousemove|onmouseout|onmouseover|onmouseup|onreset|onselect|onsubmit|onunload|profile|prompt|readonly|rel|rev|rows|rowspan|rules|scheme|scope|scrolling|selected|shape|size|span|src|standby|start|style|summary|tabindex|target|text|title|type|usemap|valign|value|valuetype|version|vlink|vspace|width';
	var deprecatedAttributes = selectByXPath(dom,"//xhtml:*/@*[not(contains('|"+attributes+"|',concat('|',name(),'|')))]");
	var i = deprecatedAttributes.length;
	var buf = [];
	while(i--){
		var a = deprecatedAttributes.item(i);
		if(a.namespaceURI == null){
			var tag = a.ownerElement;
			buf.push(tag.tagName +"@" +a.name+':'+getNodePosition(tag));
		}
	}
	if(buf.length){
		$log.warn("网页中使用了未知或不推荐的 属性:\n"+buf.join('\n'))
	}

}
function checkXHTML(dom){
	checkEmptyImg(dom);
	checkUnknowAttr(dom);
	checkUnknowTag(dom);
}

function domFilterXHTML(path,dom){
	//var a = new Date();
	checkXHTML(dom);
	//$log.info(new Date() - a);
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
				value = addHashData(value,toAbsolutePath(value,path));
			}
			if(contextPath){
				value = addContextPath(value,contextPath);
			}
			item.value = value;
		}
	}
	return dom;
}