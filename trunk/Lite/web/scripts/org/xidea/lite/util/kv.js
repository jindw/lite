function getByKey(map,key){
	if((map = map[2]) && typeof key == 'string'){
		return key in map ? map[key]:null;
	}
	var keys = map[0];
	var values = map[1];
	var i = keys.length;
	while(i--){
		if(key == keys[i]){
			return values[i];
		}
	}
}
function removeByKey(map,key){
	if(map[2] && typeof key == 'string'){
		delete map[2][key] ;
	}else{
		var keys = map[0];
		var values = map[1];
		var i = keys.length;
		while(i--){
			if(key == keys[i]){
				keys.splice(i,1);
				values.splice(i,1);
			}
		}
	}
}

function setByKey(map,key,value){
	if(map[2] && typeof key == 'string'){
		map[2][key] = value;
	}else{
		var keys = map[0];
		var values = map[1];
		var i = keys.length;
		while(i--){
			if(key == keys[i]){
				values[i] = value;
				return;
			}
		}
		keys.push(key);
		values.push(value);
	}
}