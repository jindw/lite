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
 * 装载指定路径下文本
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
function loadChainText(path){
	return resourceManager.loadChainContent(path);
}
/**
 * 保存指定文本到指定路径下文件
 * @public
 * @param path 文件路径（相对网站根目录）
 * @param content 文件文本内容
 * @param encoding 文件编码
 */
function saveText(path,content,encoding){
	resourceManager.saveText(path,content,encoding);
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

function getNodePosition(node){
	return resourceManager.getNodePosition(node);
}
