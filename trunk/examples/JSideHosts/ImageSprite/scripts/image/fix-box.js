var trim = require('./box').trim;
var minimize = require('./box').minimize;

function FixBox(boxFactory){
	this.factory = boxFactory;
}

FixBox.prototype.start = function(pos){
//	this.startPosition = pos;
}

FixBox.prototype.change = function(pos){
//	this.currentPosition = pos;
}


FixBox.prototype.end = function(pos){
	var selectedRange = this.factory.createMatrix(pos[0],pos[1],pos[2]-pos[0],pos[3]-pos[1]);
	var rows = this.rows = selectedRange[0];
	var cols = this.cols = selectedRange[1];
	this.range =  minimize(rows,cols,0,0,cols.length-1,rows.length-1);
	return this.range;
}


FixBox.prototype.toCSS = function(){
	return "${name}{background:url(${path}/${name}[0].png)}"+
			"${name} span{background:url(${path}/${name}[1].png)}"
			"${name} span span{background:url(${path}/${name}[2].png);margin:0 2px}"
}

FixBox.prototype.toHTML = function(){
	return "<span class='${name}'><span><span><c:block name='content'/></span></span></span>"
}
exports.FixBox = FixBox;