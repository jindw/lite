function copy(s,d){
	for(var n in s){
		d[n] = s[n];
	}
}
copy(require('./image/box'),exports);
copy(require('./image/fix-box'),exports);
copy(require('./image/init'),exports);
copy(require('./image/lcr-button'),exports);
