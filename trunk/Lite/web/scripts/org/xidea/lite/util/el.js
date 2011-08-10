
function findLiteParamMap(value){
	var result = {};
	while(value){
		var match = value.match(/^\s*([\w\$\_]+|'[^']*'|"[^"]*")\s*(?:[\:=]\s*([\s\S]+))\s*$/);
		if(!match){
			throw $log.error("非法参数信息",value);
			return null;
		}
		value =match[2];
		var key = match[1].replace(/^['"]|['"]$/g,'');
		var p = findStatementEnd(value);
		var statment = value.substring(0,p);
		
		result[key] = statment;
		value = value.substring(p+1);
	}
	return result;
}
/**
 * @private
 */
function findStatementEnd(text){
	var end = 0;
	do{
		var end1 = text.indexOf(',',end + 1);
		var end2 = text.indexOf(';',end + 1);
		if(end2>0 && end1>0){
			end = Math.min(end1 , end2);
		}else{
			end = Math.max(end1,end2);
		}
		if(end<=0){
			break;
		}
		var code = text.substring(0,end);
		try{
			new Function(code);
			return end;
		}catch(e){
			end = end+1
		}
	}while(end>=0)
	return text.length;
}