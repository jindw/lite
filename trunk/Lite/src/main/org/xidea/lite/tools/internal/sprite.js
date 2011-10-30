var Env = require('./env');
var ImageSprite = function(){
	return new org.xidea.lite.tools.ImageSprite(Env.root);
}
var SPRITE_KEY = "org.xidea.lite.tools.ImageSprite"
var spriteConfig = null;
/**
 * sprite base /module/static/_/
 * sprite file /module/static/css/a.css
 * 
 */
function spritePath(path,base){
	if(path.substring(0,base.length) == base){//需要放在相同的模块下
		path = path.substring(base.length).replace(/\/?[^\/]+$/,'').replace(/\//g,'.');
		return path+'.png';
	}else{
		return null;
	}
}
/**
 * image[xyrb 200].png
 */
function spriteImage(path,base){
	var file = path.replace(/.*\//,'');
	var dir = base+file.replace(/\.png$/,'.').replace(/\./g,'/');
	var files = Env.dir(dir);
	var images = [];
	var imageMap = {};
	var spriteImpl = new ImageSprite(Env.root);
	var alpha = false;
	Env.addRelation(dir);
	for(var i=0;i<files.length;i++){
		var file = files[i];
		if(/\.(gif|png|jpe?g)$/.test(file)){
			//$log.info(path,base,dir,file)
			var filePath = dir+file;
			var resource = spriteImpl.getImage(filePath);
			Env.addRelation(filePath);
			if(resource.alpha){
				alpha = true;
			}
			images.push(imageMap[file] = {
				name : file,
				info : file.replace(/^.*?(\[.*\])?\.\w+$/,'$1'),
				width:resource.width,
				height:resource.height,
				alpha:resource.alpha,
				//repeatX : false,
				//repeatY : false,
				//x:0,y:0,
				resource:resource
			});
		}
	}
	var ras = initRepeatAndSize(images);
	var repeat = ras[0];
	var maxWidth = ras[1];
	var maxHeight = ras[2];
	var size = initOffset(images,repeat,maxWidth,maxHeight);
	var width = size[0];
	var height = size[1];
	var imgbuf = spriteImpl.createImage(width,height);
	
	for(var i=0;i<images.length;i++){
		var image = images[i];
		var resource = image.resource;
		if(image.repeat){
			if(image.repeat == 'x'){
				var x = 0;
				do{
					imgbuf.drawImage(resource,Math.min(x,maxWidth-image.width),image.y);
					x += image.width;
				}while(x<maxWidth)
			}else{
				var y = 0;
				do{
					imgbuf.drawImage(resource,image.x,Math.min(y,maxHeight-image.height));
					y += image.height;
				}while(y<maxHeight)
			}
		}else{
			imgbuf.drawImage(resource,image.x,image.y);
		}
	}
	
	Env.set(path, SPRITE_KEY,{
		alpha:alpha,
		width:maxWidth,
		height:maxHeight,
		imageMap:imageMap
	})
	return spriteImpl.compress(imgbuf);
}
function getOffset(image){
	return image.info.replace(/[^\d]/g,'') || 1;
}
function initOffset(images,repeat,maxWidth,maxHeight){
	//repeatY  ->x
	if(repeat == 'y'){
		var offsetX = 0;
		for(var i=0;i<images.length;i++){
			var image = images[i];
			var bottom = image.info.indexOf('b')>=0
			//todo
			image.x = offsetX;
			image.y = bottom ? maxHeight - image.height : 0;
			offsetX += image.width;
			if(i<images.length-1){
				offsetX += getOffset(image);
			}
		}
		return [offsetX,maxHeight]
	}else{
		//repeatX or no Repeat
		//-y
		var offsetY = 0;
		for(var i=0;i<images.length;i++){
			var image = images[i];
			var right = image.info.indexOf('r')>=0
			//todo
			image.y = offsetY;
			image.x = right ? maxWidth - image.width : 0;
			offsetY+=image.height;
			if(i<images.length-1){
				offsetY += getOffset(image);
			}
		}
		return [maxWidth,offsetY]
	}
}
function initRepeatAndSize(images){
	var repeatX = [];
	var repeatY = [];
	var repeat = []
	var maxWidth = 0;
	var maxHeight = 0;
	for(var i=0;i<images.length;i++){
		var image = images[i];
		var resource=image.resource;
		var info = image.info;
		console.error(image.name,info)
		if(info.indexOf('x')>=0){
			repeatX.push(image)
			if(!/x/.test(resource.repeat)){
				repeat.push(image.width)
			}
			image.repeat = 'x'
			image.repeatX = true;
		}
		if(info.indexOf('y')>=0){
			repeatY.push(image)
			if(!/y/.test(resource.repeat)){
				repeat.push(image.height)
			}
			image.repeat = 'y'
			image.repeatY = true;
		}
		maxWidth = Math.max(resource.width,maxWidth);
		maxHeight = Math.max(resource.height,maxHeight);
	}
	if(repeatX.length && repeatY.length){
		//error
		throw new Error('repeat-x and repeat-y can not exits in same dir')
	}
	if(repeatX.length){
		maxWidth = repeatSize(maxWidth,repeat);
		repeat = 'x';
	}else if(repeatY.length){
		maxHeight = repeatSize(maxHeight,repeat);
		repeat = 'y';
	}else{
		repeat = '';
	}
	return [repeat,maxWidth,maxHeight]
}
//求可安全平铺的最小公倍数。
function repeatSize(size,list){
	if(list.length){
		list.sort();
		max = list.pop();
		var base = parseInt(size/max)*max;
		if(base < size){
			base+=max;
		}
		if(list.length){
			var last = list.length-1;
			out:while(true){
				var i = last;
				while(i--){
					if(base % list[i]){
						base += max;
						continue out;
					}
				}
				break;
			}
		}
		return base;
	}
	return size;
}
function spriteCSS(path,base,dest,source){
	return source.replace(/\bbackground\s*\:\s*url\(([^)]+)\)(.*?)(?=[;\}]|\/\*})/ig,
		function(a,url,postfix){
			url = url.replace(/['"]/g,'');
			if(path.indexOf(dest) == ''){//不能sprite 目标地址下的文件
				return a;
			}
			var destFile = dest + spritePath(url,base);
			if(destFile){
				Env.getContentHash(destFile);
				var spriteInfo = Env.get(destFile,SPRITE_KEY);
				if(spriteInfo!=null){
					var filename = url.replace(/.*\/([^\/]+)$/,'$1');
					var fi = spriteInfo.imageMap[filename];
					if(fi){
						var pos = getCSSPosition(fi,postfix);
						var repeat = getCSSRepeat(fi,postfix);
						var ext = repeat+ ' '+ pos ;
						var css = 'url('+destFile+') '+ext;
						if(fi.alpha){
							//如果有Alpha 透明
							css += ';_background: none '+ext
								+'_filter: progid:DXImageTransform.Microsoft.AlphaImageLoader(src="url(' 
								+ destFile
								+ ')")';
						}
						return 'background:'+css;
					}else{
						console.info(url,filename);
					}
				}
			}
			return a;
		});
}
function getCSSPosition(fi,postfix){
	var x = fi.x?-fi.x+'px':0;
	var y = fi.y?-fi.y+'px':0;
	var info = fi.info;
	var info2 = info;
	if(/[r]/.test(info)){
		x = 'right';//'100%'
		if(info2 == info2.replace(/(?:\bright\b)|100%/,'')){
			console.error('css code error, right align images must declared as right position');
		}
		if(/[b]/.test(info)){
		   y = 'bottom';//'100%'
		   if(info2 == info2.replace(/(?:\bbottom\b)|100%/,'')){
		   		console.error('css code error, bottom align images must declared as bottom position');
		   }
		}
	}else if(/[b]/.test(info)){
		y = 'bottom';//'100%'
		if(info2 == info2.replace(/(?:\bbottom\b)|100%/,'')){
		   	console.error('css code error, bottom align images must declared as bottom position');
		}
	}
	if(info2 != info2.replace(/\d+%?|\b(?:left|right|center|top|bottom)\b/,'')){
			console.error('css code error, position must same as filename flag[xyrb]');
	}
	return x + ' '+ y;
}
function getCSSRepeat(fi,postfix){
	if(fi.repeat){
		return repeat = fi.repeatX? 'repeat-x':'repeat-y';
	}else{
		return 'no-repeat';
	}
}
function setupSprite(base,dest){
	if(!spriteConfig){
		Env.addBytesFilter('**.png',function(path,data){
			if(path.indexOf(dest) == 0 && path.indexOf('/',dest.length+1)<0){
				return spriteImage(path,spriteConfig[0]);
			}else{
				return data;
			}
		});
		Env.addTextFilter('**.css',function(path,text){
			return spriteCSS(path,spriteConfig[0],spriteConfig[1],text)
		})
	}
	spriteConfig = [base,dest];
}
exports.spriteCSS = spriteCSS;
exports.spriteImage = spriteImage;
exports.spritePath = spritePath;
exports.setupSprite = setupSprite;
//Env.addTextFilter('/static/_/*.png',imgageSprite);
//Env.addTextFilter('**.css',cssSprite);