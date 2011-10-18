//导入调试服务器编译上下文
var Env = require("./env");
function assertEmpty(msg,list){
	var i = list.length;
	if(i){
		var buf = [];
		while(i--){
			var node = list.item(i);
			buf.push(Env.getNodePosition(node)+node.name);
		}
		$log.error(msg+buf.join('\n'))
	}
}

function checkEmptyImg(dom){
	//检测空img属性
	var emptyImages = Env.selectByXPath(dom,"//xhtml:img[@src='']");
	assertEmpty("img 标签不能带空 src属性[参考 Issues：18]:\n",emptyImages)
	
}
function checkUnknowTag(dom){
	var elements = 'h1|h2|h3|h4|h5|h6|h7|a|abbr|acronym|address|area|article|aside|audio|b|base|bdo|big|blockquote|body|br|button|canvas|caption|cite|code|col|colgroup|command|datagrid|datalist|datatemplate|dd|del|details|dfn|dialog|div|dl|dt|em|embed|event|fieldset|figure|footer|form|frame|frameset|h1|head|header|hr|html|i|iframe|img|input|ins|kbd|label|legend|li|link|m|map|meta|meter|nav|nest|noframes|noscript|object|ol|optgroup|option|output|p|param|pre|progress|q|rule|samp|script|section|select|small|source|span|strong|style|sub|sup|table|tbody|td|textarea|tfoot|th|thead|time|title|tr|tt|ul|var|video';
	var deprecatedElements = Env.selectByXPath(dom,"//xhtml:*[not(contains('|"+elements+"|',concat('|',name(),'|')))]");
	assertEmpty("网页中使用了未知或不推荐的 html标签:\n",deprecatedElements)
}
function checkUnknowAttr(dom){
	var attributes = 'abbr|accept|accept-charset|accesskey|action|align|alink|alt|archive|axis|background|bgcolor|border|cellpadding|cellspacing|char|charoff|charset|checked|cite|class|classid|clear|code|codebase|codetype|color|cols|colspan|compact|content|coords|data|datetime|declare|defer|dir|dir|disabled|enctype|face|for|frame|frameborder|headers|height|href|hreflang|hspace|http-equiv|id|ismap|label|lang|language|link|longdesc|marginheight|marginwidth|maxlength|media|method|multiple|name|nohref|noresize|noshade|nowrap|object|onblur|onchange|onclick|ondblclick|onfocus|onkeydown|onkeypress|onkeyup|onload|onmousedown|onmousemove|onmouseout|onmouseover|onmouseup|onreset|onselect|onsubmit|onunload|profile|prompt|readonly|rel|rev|rows|rowspan|rules|scheme|scope|scrolling|selected|shape|size|span|src|standby|start|style|summary|tabindex|target|text|title|type|usemap|valign|value|valuetype|version|vlink|vspace|width';
	var deprecatedAttributes = Env.selectByXPath(dom,"//xhtml:*/@*[not(contains(name(),':')) and not(contains('|"+attributes+"|',concat('|',name(),'|')))]");
	assertEmpty("网页中使用了未知或不推荐的 属性:\n",deprecatedAttributes)

}
function checkXHTML(path,dom){
	checkEmptyImg(dom);
	checkUnknowAttr(dom);
	checkUnknowTag(dom);
	return dom;
}
function normalizeXML(text,path){
	return Packages.org.xidea.lite.impl.ParseUtil.normalize(text,path);
}

function filterXHTMLDom(path,dom){
	return dom;
}
exports.normalizeXML = normalizeXML;
exports.checkXHTML = checkXHTML;
exports.filterXHTMLDom = filterXHTMLDom;


