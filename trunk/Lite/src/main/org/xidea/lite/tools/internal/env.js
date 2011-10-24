/**
 * 装载未经过处理的指定路径下文本
 * @public
 * @param path 文件路径（相对网站根目录）
 * @param encoding 文件编码
 * @return String 文件内容文本
 */
function loadRawText(path,encoding){
	var data = resourceManager.getRawBytes(path);
	if(encoding){
		encoding = resourceManager.getEncoding(path);
	}
	var text = Packages.org.xidea.lite.impl.ParseUtil.loadTextAndClose(
			new java.io.ByteArrayInputStream(data), encoding);
	return text;
}
/**
 * 用于在插件运行过程中，装载指定路径下内容，并经过当前以执行的过滤器处理后的文本。
 * @public
 * @param path 文件路径（相对网站根目录）
 * @return String 文件内容文本
 */
function loadChainText(path){
	return resourceManager.loadChainText(path);
}
/**
 * 采用系统默认编码，保存指定数据到指定路径下的文件中。
 * @public
 * @param path 文件路径（相对网站根目录）
 * @param content 文件文本内容
 */
function saveText(path,content){
	resourceManager.saveText(path,content);
}

/**
 * 添加关联文件
 * @public
 * @param path 文件路径（相对当前处理文件）
 */
function addRelation(path){
	resourceManager.addRelation(path)
}
function addFilter(pattern,impl,type){
	function fn(v){
		var rtv = impl.apply(this,arguments)
		return rtv || v;
	}
	var proxy = resourceManager.createFilterProxy({doFilter:fn,toString:function(){return impl+''}});
	return resourceManager['add'+type+'Filter'](pattern,proxy);
}
function addBytesFilter(pattern,impl){
	addFilter(pattern,impl,'Bytes');
}
function addTextFilter(pattern,impl){
	addFilter(pattern,impl,'Text');
}
function addDocumentFilter(pattern,impl){
	addFilter(pattern,impl,'Document');
}
function selectByXPath(node,xpath){
	return Packages.org.xidea.lite.impl.ParseUtil.selectByXPath(node,xpath);
}
function getNodePosition(el){
	if(el.nodeType == 2){//attr
		el = el.ownerElement;
	}
	var info = el.getAttributeNS("http://www.xidea.org/lite/core", "__i");
	return el.ownerDocument.documentURI + '@'+(info && info.replace(/\|[\s\S]*$/,''));
}
// for commonjs exports
exports.root = resourceManager.root;
exports.loadRawText = loadRawText;
exports.saveText = saveText;
exports.addRelation = addRelation;
exports.loadChainText = loadChainText;
exports.addBytesFilter = addBytesFilter;
exports.addTextFilter = addTextFilter;
exports.addDocumentFilter = addDocumentFilter;
exports.getContentHash = function(path){
	return resourceManager.getContentHash(path);
}
exports.dir = function(path){
	return resourceManager.dir(path);
}

exports.get = function(path,key){
	return resourceManager.get(path,key);
}

exports.set = function(path,key,value){
	return resourceManager.set(path,key,value);
}

exports.selectByXPath = selectByXPath;
exports.getNodePosition = getNodePosition;