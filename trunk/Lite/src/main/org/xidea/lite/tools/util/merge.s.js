function mergeJS(path,sourceLoader){
	sourceLoader = sourceLoader || loadChainText;
	var fileMap = {};
	findRelation(path,sourceLoader,jsRelationFinder,fileMap,false);
	return fileMap;
}
function mergeCSS(path,sourceLoader){
	sourceLoader = sourceLoader || loadChainText;
	var fileMap = {};
	findRelation(path,sourceLoader,jsRelationFinder,fileMap,true);
	return fileMap;
}

function findRelation(path,sourceLoader,relationFinder,fileMap,importFirst){
	var relations = [];
	var source = relationFinder(path,sourceLoader,relations);
	fileMap[path] = source;
	for(var i=0,len = relations.length;i<len;i++){
		var relation = relations[i];
		if(!(relation in fileMap)){
			findRelation(relation,sourceLoader,relationFinder,fileMap,importFirst);
		}
	}
	if(importFirst){
		delete fileMap[path];
		fileMap[path] = source;
	}
}
//css import 的内容先装载
function cssRelationFinder(path,loadText,pbuf){
	var source = loadText(path);
	var dw = /^@import\s+url\(['"](.+?)['"]\)(?:\s*;)?/mg;
	var sbuf = [];
	var fi = 0;var m
	while(m = dw.exec(source)){
		var bi = m.index
		sbuf.push(source.substring(fi,fi = bi));
		pbuf.push(m[1]);
		//pbuf.push(dw[1]);
		fi += m[0].length;
	}
	sbuf.push(source.substring(fi),'\n');
	return sbuf.join('');
}
//js !document.write的脚本无论何时都是后装载（Opera 除外）
function jsRelationFinder(path,loadText,pbuf){
	var source = loadText(path);
	var dw = /^!document\s*.\s*write\s*\(/mg;
	var sbuf = [];
	var fi = 0;var m
	while(m = dw.exec(source)){
		var bi = m.index
		var ei = findEnd(source,bi,')');
		if(!ei){
			continue;
		}
		sbuf.push(source.substring(fi,fi = bi));
		var html = window.eval(source.substring(source.indexOf('(',fi)+1,ei))
		html.replace(/<script\s+src\s*=\s*(?:'([^']+)'|"([^"]+)")/g,function(a,src1,src2){
			pbuf.push(src1||src2);
		})
		fi = ei+1;
	}
	sbuf.push(source.substring(fi),'\n');
	return sbuf.join('');
}
/**
 * !document.write([
 *     "a.js",
 *     "b.js"
 * ].join('\n').replace(/.+/,"<script src='/static/js/$1'></script>");
 */
function findEnd(source,fi,end){
	var i = fi-1;
	while((i = source.indexOf(end,i+1))>0){
		try{
			new Function("return "+source.substring(fi,i+1));
			return i;
		}catch(e){}
	}
	return null;
}