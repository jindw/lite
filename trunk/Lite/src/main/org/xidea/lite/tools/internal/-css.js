//导入调试服务器编译上下文
var Env = require("./env");

/**
 * @param path
 * @param 被import的其他CSS绝对路径
 * @return 删除import之后的纯CSS源码
 */
function extractCssRelation(path,relations){
	var source = Env.loadChainText(path);
	var dw = /^@import\s+url\(['"](.+?)['"]\)(?:\s*;)?/mg;
	var sbuf = [];
	var fi = 0;var m
	while(m = dw.exec(source)){
		var bi = m.index
		sbuf.push(source.substring(fi,fi = bi));
		relations.push(m[1]);
		//pbuf.push(dw[1]);
		fi += m[0].length;
	}
	sbuf.push(source.substring(fi),'\n');
	return sbuf.join('');
}
function doCSSMerge(path,fileList,fileMap,relations){
	var source = extractCssRelation(path,relations);
	fileMap[path] = source;
	fileList.push(path);
	for(var i=0,len = relations.length;i<len;i++){
		if(!(relation in fileMap)){
			doCSSMerge(path,fileList,fileMap,relations);
		}
	}
}

/**
 * 合并所有@import 导入的外部CSS源码，并上上下文设置导入的文件依赖
 */
function mergeCSS(path){
	var fileMap = {};
	var fileList = [];
	var relations = [];
	doCSSMerge(path,fileMap,fileList,relations);
	var result = [];
	for(var i = 0;i<fileList.length;i++){
		var n  = fileList[i];
		Env.addRelation(n);
		result.push(fileMap[n])
	}
	return result.join('\n');
}

function compressCSS(path,text){
	return text.replace(/^\s+/g,'');
}

function onSpriteBackground(path){
	//调用后端Java实现，生成Sprite图片
	var info = Packages.org.xidea.lite.util.CSSSprite.sprite(Env.getRoot(),path);
	
	//按自定义规则，生成Sprite文件的位置
	var spritePath = path.replace(/[^\/]+$/,'')+'_/'+info.name;
	
	//标记生成文件地址，数据，和依赖文件
	Env.addVirtualFile(spritePath, info.data, info.relations);
	
	//生成Sprite CSS代码
	return 'background:url('+spritePath+') '+info.top+'px '+info.left+'px'
}

/* 设置CSS资源过滤器 */
Env.addTextFilter("/**.css",mergeCSS);
Env.addTextFilter("/**.css",compressCSS);

/* 暴露CSS 压缩器，以备不时之需 */
exports.compressCSS = compressCSS;



