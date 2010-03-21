function XMLHttpRequest(){
}
XMLHttpRequest.prototype={
	open : function(method, url, asyn) {
        this.url = url;
	    if(!XMLHttpRequest[url]){
    		try{
        		url = url.replace(/^\w+:(\/)+(?:\?.*=)/,'$1');
        		var buf = new java.io.StringWriter();
        		var ins = buf.getClass().getResourceAsStream(url);
        		var ins = new java.io.InputStreamReader(ins,"utf-8");
        		var c;
        		while((c=ins.read())>=0){
        			buf.append(c);
        		}
        		this.responseText = ''+buf;
    		}catch(e){
    		    print(e);
    		}
	    }
	},
	getResponseHeader:function(key){
	    try{
	        return XMLHttpRequest[this.url][
	        '#getResponseHeader'][key]
	    }catch(e){
	        print([e,this.url,key])
	    }
	},
	send : function(data) {
	    var data = XMLHttpRequest[this.url];
	    for(var n in data){
	        this[n] = data[n];
	    }
	},
	status : 200
}

alert = print;
confirm = print;
function DOMParser(){
    return new Packages.org.xidea.lite.js.test.JSCompileTest.DOMParser()
}

function XPathEvaluator(){
    return new Packages.org.xidea.lite.js.test.JSCompileTest.XPathEvaluator()
}

//XMLHttpRequest["menu.xml"] = {
//    responseText = 
//}