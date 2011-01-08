
function liteFormat(json,showName){
    return doLiteFormat(json,"",showName);
}
function doLiteFormat(json,prefix,showName){
    var buf = [];
    buf.push(prefix,"[\n");
    for(var i=0;i<json.length;i++){
        buf.push(prefix+"\t");
        var item = json[i];
        if(typeof item == 'string'){
            buf.push(stringifyJSON(item));
        }else{
            var j = 0;
            var type = item[j++];
            buf.push("[",showName?TIN[type]:type,",");
            switch (type) {
    		case CAPTRUE_TYPE:
    		case IF_TYPE:
    		case ELSE_TYPE:
    		case FOR_TYPE:
    		    var child = item[j++];
    		    buf.push("\n",doLiteFormat(child,prefix+"\t\t",showName),",");
    			break;
    		}
    		while(j<item.length){
    		    var item2 = item[j++];
    		    if(item2 instanceof Array){
    		        buf.push(doFormatEL(item2,showName));
    		    }else{
    		        buf.push(stringifyJSON(item2));
    		    }
    		    if(j<item.length){
    		        buf.push(",");
    		    }
    		}
		    buf.push("]");
        }
		if(i+1<json.length){
		    buf.push(",\n");
		}else{
		    buf.push("\n");
		}
    }
    buf.push(prefix,"]");
    return buf.join("");
    
}

function doFormatEL(json,showName){
    if(showName){
        return stringifyLabel(json);
    }else{
        return stringifyJSON(json);
    }
}
function stringifyLabel(json){
	if(json instanceof Array){
		var buf = ["["]
	    for(var i =0;i<json.length;i++){
	        if(i){
	            buf.push(",",stringifyLabel(json[i]));
	        }else{
	        	var type = json[0];
	        	var type2 = EIN[type];
	        	if(type2){
	        		buf.push(type2);
	        	}else{
	        		buf.push(stringifyLabel(type));
	        	}
	        }
	    }
	    buf.push("]");
	    return  buf.join("");
	}
	return stringifyJSON(json);
}
