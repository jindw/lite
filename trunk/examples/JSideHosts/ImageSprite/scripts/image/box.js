function Box(data,width){
	var rows = [];
	var cols = [];
	var start = 0;
	var len = data.length;
	var height = len/width;
	for(var j=0;j<width;j++){
		cols.push([])
	}
	for(var i = 0;i<height;i++){
		var row = [];
		rows.push(row);
		for(var j=0;j<width;j++){
        	var gray = data[start++] * .3 + data[start++] * .59 + data[start++] * .11;
        	start++;
        	row.push(gray);
        	cols[j].push(gray)
		}
	}
	this.rows = rows;
	this.cols = cols;
	//alert([rows.length,cols.length])
	var pos = this.minimize(rows,cols,0,0,rows.length-1,cols.length-1)
	this.left = pos[0];
	this.top = pos[1];
	this.right = pos[2];
	this.bottom = pos[3];
	this.width = this.right-this.left+1;
	this.height=this.top-this.bottom+1;
}


Box.prototype.minimize = function(left,top,right,bottom){
	return minimize(rows,cols,left,top,right,bottom);
}

function findDeltaStart(lines,startLine,endLine,lineBegin,lineEnd,delta,count){
	var inc = startLine<endLine?1:-1;
	if(startLine>0 && startLine<lines.length-1){
		startLine-=inc;
	}
	while(startLine!=endLine){
		if(!lineEqual(lines,startLine,startLine+=inc,lineBegin,lineEnd,delta,count)){
			//t++;//todo:...
			return startLine;
		}
	}
}
function minimize(rows,cols,left,top,right,bottom,delta,deltacount){
//	var lastRowIndex = rows.length-1;
//	var lastColIndex = cols.length -1;
	delta = delta|| 9;
	deltacount = deltacount||0;
	//top
	var t = findDeltaStart(rows,top,bottom,left,right,delta,deltacount);
	//bottom
	var b = findDeltaStart(rows,bottom,top,left,right,delta,deltacount);
	//left
	var l = findDeltaStart(cols,left,right,top,bottom,delta,deltacount);
	//right
	var r = findDeltaStart(cols,right,left,top,bottom,delta,deltacount);
	return [l,t,r,b]
}
function trim(lines,startLine,endLine,lineBegin,lineEnd,delta,count){
	var inc = startLine<endLine?1:-1;
	if(startLine>0 && startLine<lines.length-1){
		startLine-=inc;
	}
	while(startLine!=endLine){
		if(lineEqual(lines,startLine,startLine+=inc,lineBegin,lineEnd,delta,count)
			&& lineEqual(lines,startLine,startLine+inc,lineBegin,lineEnd,delta,count)){
			//t++;//todo:...
			return startLine-=inc;
		}
	}
}
Box.prototype.trimLeftRight = function(){
	var startLine = this.left;
	var endLine = this.right;
	var lineBegin = this.top;
	var lineEnd = this.bottom;
	
	var delta = 9,deltacount = 0;
	this.innerLeft = trim(lines,startLine,endLine,lineBegin,lineEnd,delta,deltacount);
	this.innerRight = trim(lines,endLine,startLine,lineBegin,lineEnd,delta,deltacount);
}
Box.prototype.findButtonText = function(){
	if(!this.innerRight){
		this.trimLeftRight();
	}
	var pos = minimize(this.rows,this.cols,this.innerLeft,this.innerTop,this.right,this.bottom)
	this.textLeft = pos[0];
	this.textTop = pos[1];
	this.textRight = pos[2];
	this.textBottom = pos[3];
	this.textWidth = this.textRight-this.textLeft+1;
	this.textHeight=this.textTop-this.textBottom+1;
}
Box.prototype.findTextInfo = function(){
	//取1/2-3/4 处为文字区域,查询位置
	var begin = parseInt(this.left + (this.right-this.left)*.5);
	var end = parseInt(this.left + (this.right-this.left)*.75);
	var b = this.bottom;
	var counts = [];
	var tt;
	var tb;
	for(var i=this.top;i<=b;i++){
		var c = count(this.rows[i],begin,end,64);
		counts.push(c);
		if(c>2){
			if(!tt){
				tt = i;
			}
			else{
				tb = i;
			}
		}
	}
	this.textTop =tt;
	this.textBottom = tb;
}
/**
 * 搜索图变数
 */
function count(line,begin,end,delta){
	var color = line[begin];
	var count = 0;
	while(begin++<end){
		var color2 = line[begin];
		if(Math.abs(color2- color)>delta){
			count++;
		}
		color = color2;
	}
	return count;
}

function isRepeat(line,begin,end,delta,max){
	var count = 0;
	max = max || 0;
	for(var i=begin+1;i<end;i++){
		if(Math.abs(line[i] - line[i-1])>delta){
			if(++count>max){
				return false;
			}
		}
	}
	return true;
}

function lineEqual(lines,currentIdex,nextIndex,begin,end,delta,max){
	var count = 0;
	var line1 = lines[currentIdex];
	var line2 = lines[nextIndex];
	max = max|| 0;
	for(var i=begin;i<end;i++){
		if(Math.abs(line1[i] - line2[i])>delta){
			if(++count>max){
				return false;
			}
		}
	}
	return true;
}
exports.minimize = minimize;
exports.trim = trim;

