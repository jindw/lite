var trim = require('./box').trim;
var minimize = require('./box').minimize;

function $(id){
	return document.getElementById(id);
}
function getPosition(event){
	var main = $('main');
	return [event.clientX+main.scrollLeft,event.clientY+main.scrollTop]
}
var currentInfo;
var ImageSprite = {
	initialize:function(image){
		$('covers').style.width = image.width+'px';
		$('covers').style.height = image.height+'px';
		this.boxFactory = new BoxFactory(image,$('canvas'));
	},
	prepare:function(Type){
		currentInfo = [new Type(this.boxFactory),[],null];
		document.body.className = 'prepare finder-type-'+currentInfo.name;
	},
	startBox:function(event){
		if(currentInfo){
			document.body.className = 'start'
			var pos = getPosition(event);
			var div = document.createElement('div');
			div.className = 'box'
			div.style.left = pos[0] +'px'
			div.style.top = pos[1]+'px'
			$('covers').appendChild(div);
			currentInfo[0].start(pos);
			currentInfo[1]=  pos;
			currentInfo[2] = div;
		}
	},
	changeBox:function(event){
		var div = currentInfo && currentInfo[2];
		if(div ){
			var pos1=currentInfo[1];
			var pos2 = getPosition(event);
			var box = [Math.min(pos2[0],pos1[0]),Math.min(pos2[1],pos1[1]),
				Math.max(pos2[0],pos1[0]),Math.max(pos2[1],pos1[1])];
			div.style.left = box[0];
			div.style.top = box[1];
			div.style.width = (box[2] - box[0]) +'px'
			div.style.height = (box[3] - box[1])+'px'
			currentInfo[0].change(pos2);
		}
	},
	endBox:function(event){
		if(currentInfo){
			document.body.className = ''
			var pos1=currentInfo[1];
			var pos2 = getPosition(event);
			var box = [Math.min(pos2[0],pos1[0]),Math.min(pos2[1],pos1[1]),
				Math.max(pos2[0],pos1[0]),Math.max(pos2[1],pos1[1])];
			var div = currentInfo[2];
			var box = currentInfo[0].end(box);
			
			
			//TODO:
			div.style.left = pos1[0] + box[0] +'px'
			div.style.top =pos1[1]+ box[1]+'px'
			div.style.width = box[2]-box[0] +'px';
			div.style.height = box[3]-box[1] +'px';
			appendChildren(div, currentInfo[0])
			currentInfo = null;
		}
	},
	end:function(type){
		
	}
}
function appendChildren(parentDiv,rangeBox){
	var rows = rangeBox.children;
	console.log(rows)
	if(rows){
		for(var i = 0;i<rows.length;i++){
			var row = rows[i];
			for(var j=0; j<row.length; j++) {
				var box = row[j]
				var div = document.createElement("div");
				var pos1 = rangeBox.range;
				console.log(box)
				div.style.border="1px solid blue";
				div.style.left = box[0] - pos1[0] +'px'
				div.style.top =  box[1] -pos1[1] +'px'
				div.style.width = box[2]-box[0] +'px';
				div.style.height = box[3]-box[1] +'px';
				parentDiv.appendChild(div)
			}
		}
	}
	
}

function BoxFactory(image,canvas){
	this.image = image;
	this.canvas = canvas;
}
BoxFactory.prototype.drawResult = function(box){
	var context = this.canvas.getContext('2d');
	context.fillStyle = "rgb(255,0,0)";
	context.fillRect(0, 0, width, height);
	var sx = begin[0]+box.left;
	var sy = begin[1]+box.top;
	
	width = box.width;
	height = box.height;
	
	context.drawImage(image,sx,sy,width,height,box.left,box.top,width,height)
}
BoxFactory.prototype.createMatrix = function(x,y,width,height){
	this.canvas.width = width;
	this.canvas.height = height;
	var context = this.canvas.getContext('2d');
	context.drawImage(this.image,x,y,width,height,0,0,width,height);
	var data = context.getImageData(0,0,width,height).data
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
	
	return [rows,cols,data];
}
exports.ImageSprite = ImageSprite;