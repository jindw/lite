
var CORE_URI = "http://www.xidea.org/lite/core"
var CORE_INFO = "__i";
var defaultEntryMap = {"&nbsp;": "&#160;","&copy;": "&#169;",'&':'&amp;','<':'&lt;'};
var defaultNSMap = {
	"xmlns:f": CORE_URI,
	"xmlns:c": CORE_URI,
	"xmlns:h": "http://www.xidea.org/lite/html-ext",
	"xmlns": "http://www.w3.org/1999/xhtml"
}
/**
 * row,col|nsattr1|nsattr2|@path
 */
function getLiteTagInfo(el){
	try{
		return el.getAttributeNS(CORE_URI, CORE_INFO);
	}catch(e){
		return el.getAttribute("c:"+CORE_INFO);
	}
}
function normalizeTag(source,tag,uri,pos){
	var i = 0;
	source = source.replace(/(\b[a-zA-Z_][\w_\-\.]*(?:\:[\w_][\w_\-\.]*)?)(?:\s*=\s*('[^']*'|\"[^\"]*\"|\$\{[^}]+\}|[^\s>]+))?/g,function(a,n,v){
		if(i==0){
			i++;
			tag.name = n;
			tag.attrMap = {};
			tag.nsMap = {};
			return n;
		}else{
			if(!v){
				v = '"'+n+'"'
			}else{
				v = v.replace(/&\w+;|&#\d+;|&#x[\da-fA-F]+;|[&<]/g,function(a){
					if(a in defaultEntryMap){
    					return defaultEntryMap[a];
    				}else{
    					return a;
    				}
				});
				var c = v.charAt();
				if(c != '"' && c != '\''){
					v= '"'+v.replace(/"/g,'&#34;')+'"';
				}
			}
			tag.attrMap[n]=v;
			if(/xmlns(?:\:.*)?/.test(n)){
				if(n == 'xmlns:c'){
					//老名称空间容错
					if(/^['"]http\:\/\/www.xidea.org\/ns\/(?:template|lite)\/core\/?['"]$/.test(v)){
						v = '"http://www.xidea.org/lite/core"';
					}
				}
				tag.nsMap[n] = v;
			}
			return n+'='+v
		}
	});
	for(var n in (tag.parentTag && tag.parentTag.nsMap || defaultNSMap)){
		tag.nsMap[n] = 1;
	}
	for(var n in tag.attrMap){
		if(/^xmlns\:/.test(n)){
			tag.nsMap[n] = 1;
		}
	}
	for(var n in tag.attrMap){
		if(n == 'xmlns'){
			pos += '|xmlns';
		}else if(/^(?:xml|xmlns)\:$/.test(n)){
			var n2 = n.replace(/^(.+)\:.+$/,"xmlns:$1");
			if( n2 !=n){
				pos += '|'+n;
				if(!(n2 in tag.nsMap)){
					console.error("missed namespace:"+n2,source);
				}
			}
		}
	}
	pos+='|';
	var i = source.length-1;
	if(source.charAt(i-1) == '/'){
		i--;
	}
	var begin = source.substring(0,i);
	var end = source.substring(i);
	if(!tag.parentTag){
		pos+='@'+uri;
		for(var n in defaultNSMap){
			if(!(n in tag.attrMap)){
				begin = begin+' ' +n+'="'+defaultNSMap[n]+'"'
			}
		}
	}
	return begin+' c:__i="'+pos+'"'+end;
}

/*
 * /(<\?\w+[\s\S]+?\?>|
 * <!(?:[^>\[\-]+\[[\s\S]+\]>|[^>\[\-]+>)
 * |<!\[CDATA\[[\s\S]+?\]\]>|<!--[\s\S]+?-->)|
 * 
 * 
 * <([a-zA-Z_][\w_\-\.]*(?:\:[\w_][\w_\-\.]+)?)
 * (?:\s+[\w_](?:'[^']*'|\"[^\"]*\"|\$\{[^}]+\}|[^>'"$]+|\$)*>|\s*\/?>)
 * |
 * (<\/[\w_][\w_\-\.]*(?:\:[\w_][\w_\-\.]+)?>)
 * |
 * &\w+;|&#\d+;|&#x[\da-fA-F]+;|[&<]/g,
 */
//<\?\w+[\s\S]+?\?>
var INS = "<\\?\\w+[\\s\\S]+?\\?>"
//<!--[\s\S]+?-->
var COMM = "<!--[\\s\\S]+?-->"
//<!\[CDATA\[[\s\S]+?\]\]>
//<!(?:[^>\[\-]+\[[\s\S]+\]>
var CDATA = "<!\\[CDATA\\[[\\s\\S]+?\\]\\]>";
var DTD = "<!(?:[^>\\[]+\\[[\\s\\S]+?\\]>|[^>\\[]+>)"
var TAG = "[a-zA-Z_][\\w_\\-\\.]*(?:\\:[\\w_][\\w_\\-\\.]+)?"
var ATTR = "(?:\\s+[\\w_](?:'[^']*'|\\\"[^\\\"]*\\\"|\$\{[^}]+\}|[^>'\"\/$]+|\\$)*|\\s*)"
var pattern = "("+INS+'|'+COMM+'|'+CDATA+'|'+DTD+')'+
			"|(<"+'(?:script|SCRIPT)\\b'+ATTR+'\\s*>)[\\s\\S]*?<\/(?:script|SCRIPT)\\s*>'+
			"|<("+TAG+')\\b'+ATTR+'\\/?>'+
			"|"+'<\\/('+TAG+')\s*>'+
			'|&\\w+;|&#\\d+;|&#x[\\da-fA-F]+;|[&<]';
			
pattern = new RegExp(pattern,'g');
function normalizeLiteXML(text,uri){
	var lines = text.split(/\r\n?|\n/);
	var text2 = lines.join('\n');
	var lineIndex = 0;
	var lineBase = 0;
	var rootCount = 0;
	var tag = null;
	var leaf = {
		'meta':1,'link':1,'img':1,'br':1,'hr':1,'input':1};
	
	function getPositionAttr(offset){
		while(lineBase+ lines[lineIndex].length<=offset){
			lineBase+= lines[lineIndex].length+1;
			lineIndex++;
		}
		offset -= lineBase;
		var pos = lineIndex+','+offset
		return pos;
	}
	//一个比较全面的容错。
	text2 = text2.replace(pattern,
    	function(a,notTag,scriptTag,startTag,endTag,offset){
    		if(notTag){
    			if(a.charAt(2) == 'd'){
    				a = a.replace(/^<!doctype\b/,'<!DOCTYPE');
//    			}else{
//    				/<![endif]-->/.test(a);
//    				if(/^<!--\s*[/.test(a)){
//    					return a;
//    				}
    			}
    			return a;
    		}else if(scriptTag){
    			if(tag == null){
    				rootCount++;
    			}
    			tag = {parentTag:tag};
    			var content = a.substring(scriptTag.length);
    			scriptTag = normalizeTag(scriptTag,tag,uri,getPositionAttr(offset));
    			var end = content.lastIndexOf('<');
    			var script = content.substring(0,end);
    			if(/[<>&]/.test(script) && script.indexOf('<![CDATA[')<0){
    				script ="/*<![CDATA[*/"+script+"/*]]>*/"
    			}
    			tag = tag.parentTag;
    			return scriptTag + script+content.substring(end);
    		}else if(startTag){
    			if(tag == null){
    				rootCount++;
    			}
    			tag = {parentTag:tag};
    			var isClosed = false;
    			if(/\/>$/.test(a)){
    				isClosed = true;
    			}else{
    				if(startTag in leaf){//leaf
    					if(text.indexOf('</'+startTag+'>',offset)<0){
    						isClosed = true;
    						a = a.replace(/>$/,'/>');
    					}else{
    						delete leaf[startTag];
    					}
    				}
    			}
    			a = normalizeTag(a,tag,uri,getPositionAttr(offset));
    			if(isClosed){
    				tag = tag.parentTag;
    			}
    			return a;
    		}else if(endTag){
    			if(endTag in leaf){
    				return '';
    			}
    			if(tag == null){
    				console.error("未开始标签:"+a,text)
    			}else{
    				tag = tag.parentTag
    			}
    			return a;
    		}else if(a in defaultEntryMap){
    			return defaultEntryMap[a];
    		}
    		return a;
    	});
	//if(!text2.match(/\sxmlns\:c\b/)){
    //	text2 = text2.replace(/<[\w\-\.\:]+/,"$& xmlns:c='http://www.xidea.org/lite/core'");
	//}
	if(rootCount>1){
		text2 = text2.replace(/<[\w][\s\S]+/,"<c:block xmlns:c='http://www.xidea.org/lite/core'>$&</c:block>")
	}
    return text2;
}
if(typeof require == 'function'){
exports.getLiteTagInfo=getLiteTagInfo;
exports.normalizeLiteXML=normalizeLiteXML;
}