function getByKey(map,key){
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

function setByKey(map,key,value){
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