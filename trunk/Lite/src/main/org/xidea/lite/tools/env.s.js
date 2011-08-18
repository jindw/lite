$import("org.xidea.jsi:$log");

/**
 * 处理脚本中包含其他js脚本
 * @public
 * @param path 需要包含的脚本路径
 */
function include(path){
	return resourceManager.include(path);
}

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
function addTextFilter(pattern,impl){
	impl = resourceManager.createFilterProxy({doFilter:impl});
	return resourceManager.addTextFilter(pattern,impl);
}
function addDocumentFilter(pattern,impl){
	impl = resourceManager.createFilterProxy({doFilter:impl});
	return resourceManager.addDocumentFilter(pattern,impl);
}
function selectByXPath(node,xpath){
	return Packages.org.xidea.lite.impl.ParseUtil.selectByXPath(node,xpath);
}
function getNodePosition(el){
	var info = el.getAttributeNS("http://www.xidea.org/lite/core", "__i");
	return el.ownerDocument.documentURI + '@'+(info && info.replace(/\|[\s\S]*$/,''));
}