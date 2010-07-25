function parse(uri,loader,hander) {
	var xml = loader.load(uri);
	var tStack = [];
	var eStack = [];
	var pos = -1;
	function begin(xml,start,end,tagName,attrMap){
		tStack[++pos] = tokens[0];
		var e = eStack[pos] = doc.createElement(tagName);
		for(var n in attrMap){
			var v = attrMap[n];
			if(v == null){
				v = n;
			}
			e = doc.setAttribute(n,v)
		}
		if(xml.charAt(end-2) == '/'){
			end(xml,start,end);
		}
	}
	function end(xml,start,end,tagName){
		pos--;
	}
	function text(xml,start,end){
		
	}
	function cdata(xml,start,end){
		
	}
	function doctype(xml,start,end,docName,pubid,sysid,content){
		
	}
	function ins(xml,start,end){
		
	}
	function comm(){
		
	}
	function error(msg,pa){
		$log.error(msg,uri+"@[line:"+pa[0]+",col:"+pa[1]+"]")
	}
	parseXML(xml,begin,end,text,cdata,doctype,ins,comm,error);
}

