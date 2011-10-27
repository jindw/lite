var Env = require('./env');


function processURI(path){
	//replace exact
	var path2 = replacePath(path);
	var path3 = addHashData(path2,path)
	return path3;
}
function replacePath(path){
	return path;
}
function addHashData(path,realpath){
	if(path.indexOf('?')>=0){
		return path;//not add hash on dynamic
	}
	if(path.indexOf('#')>=0){
		return path;//not add hash on hash url
	}
	var hash = Env.getContentHash(realpath||path)
	if(hash){
		return path+'?@='+hash
	}else{
		return path;
	}
}
exports.processURI = processURI;