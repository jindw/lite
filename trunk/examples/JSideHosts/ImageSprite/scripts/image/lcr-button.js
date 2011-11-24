var trim = require('./box').trim;
var minimize = require('./box').minimize;

function LCRButton(boxFactory){
	this.factory = boxFactory;
	this.children = [];
}

LCRButton.prototype.start = function(pos){
	
}

LCRButton.prototype.change = function(pos){
	
}


LCRButton.prototype.end = function(pos){
	var selectedRange = this.factory.createMatrix(pos[0],pos[1],pos[2]-pos[0],pos[3]-pos[1]);
	var rows = this.rows = selectedRange[0];
	var cols = this.cols = selectedRange[1];
	var range = this.range =  minimize(rows,cols,0,0,cols.length-1,rows.length-1);
	
	var row1 = [];
	var left = range[0];
	var top = range[1];
	var right = range[2];
	var bottom = range[3];
	var delta = 9,deltacount = 0;
	
	var innerLeft = trim(cols,left,right,top,bottom,delta,deltacount);
	var innerRight = trim(cols,right,left,top,bottom,delta,deltacount);
	
	row1.push([left,top,innerLeft,bottom]);
	row1.push([innerLeft,top,innerRight,bottom]);;
	row1.push([innerRight,top,right,bottom]);
	
	this.children[0] = row1;
	
	var textBox = minimize(rows,cols,innerLeft+1,top+2,innerRight-1,bottom-5,24);
	this.fontSize = textBox[2] - textBox[0];
	//this.fontColor = getTextColor(textBox);
	row1.push(textBox);
	console.log(textBox,"#",[innerLeft,0,innerRight,rows.length-5]);
	return range;
}


LCRButton.prototype.toCSS = function(){
//	return "${name}{background:url(${path}/${name}[0][l].png) no-repeat}\n"+
//			"${name} span{background:url(${path}/${name}[1][r].png) no-repeat\n}"
//			"${name} span span{background:url(${path}/${name}[2].png[x]);"+
//				"margin:0 ${marginRight}px 0 ${marginLeft}px;"
//				"padding:${paddingTop}px ${paddingRight}px ${paddingButton}px ${paddingLeft}px\n}".
//				replace(/\$\{(\w+)\}/g,function(a,n){
//					switch(n){
//					case 'name':
//						'btn'
//					case 'path':
//						'sprite.png'
//					default:
//					return a;
//					}
//			})
}

LCRButton.prototype.toHTML = function(){
//	return "<span class='${name}'><span><span><c:block name='content'/></span></span></span>".
//				replace(/\$\{(\w+)\}/g,function(a,n){
//					switch(n){
//					case 'name':
//						'btn'
//					default:
//					return a;
//					}
//			})
return "";
}
LCRButton.prototype.toExample = function(){
	return "<div s:class='btn'>我的按钮</div>"
}
exports.LCRButton = LCRButton;