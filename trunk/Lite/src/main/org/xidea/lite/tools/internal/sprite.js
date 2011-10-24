var Env = require('./env');
var ImageSprite = function(){
	return new org.xidea.lite.tools.ImageSprite(Env.root);
}
var SPRITE_KEY = "org.xidea.lite.tools.ImageSprite"
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
	var dir = base+file.replace(/\.png$/,'').replace(/\./g,'/');
	var files = Env.dir(dir);
	var images = [];
	var imageMap = {};
	var maxWidth = 0;
	var maxHeight = 0;
	var spriteImpl = new ImageSprite(Env.root);
	var alpha = false;
	for(var i=0;i<files.length;i++){
		var file = files[i];
		if(/\.(gif|png|jpe?g)$/.test(file)){
			var resource = spriteImpl.getImage(dir+file);
			if(resource.alpha){
				alpha = true;
			}
			maxWidth = Math.max(resource.width,maxWidth)
			maxHeight = Math.max(resource.height,maxHeight);
			images.push(imageMap[file] = {
				name : file,
				info : file.replace(/^.*(\[.*\])?\.\w+$/,''),
				width:resource.width,
				height:resource.height,
				//repeatX : false,
				//repeatY : false,
				//x:0,y:0,
				resource:resource
			});
		}
	}
	var repeat = initRepeat(images);
	if(repeat == 'xy'){
		//error
		throw new Error('repeat-x and repeat-y can not exits in same dir')
	}
	var size = initOffset(images,repeat,maxWidth,maxHeight);
	var width = size[0];
	var height = size[1];
	var imgbuf = spriteImpl.createImage(width,height);
	for(var i=0;i<images.length;i++){
		var image = images[i];
		var resource = image.resource;
		if(repeat){
			if(repeat == 'x'){
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
function initOffset(images,repeat,maxWidth,maxHeight){
	//repeatY  ->x
	if(repeat == 'y'){
		var offsetX = 0;
		for(var i=0;i<images.length;i++){
			var image = images[i];
			var bottom = image.info.indexOf('b')
			//todo
			image.x = offsetX;
			image.y = bottom ? maxHeight - image.height : 0;
			offsetX+=image.width;
			if(i<images.length-1){
				offsetX += Math.max(48,image.width)
			}
		}
		return [offsetX,maxHeight]
	}else{
		//repeatX or no Repeat
		//-y
		var offsetY = 0;
		for(var i=0;i<images.length;i++){
			var image = images[i];
			var right = image.info.indexOf('r')>0
			//todo
			image.y = offsetY;
			image.x = right ? maxWidth - image.width : 0;
			offsetY+=image.height;
			if(i<images.length-1){
				offsetY += Math.max(48,image.height)
			}
		}
		return [maxWidth,offsetY]
	}
}
function initRepeat(images){
	var rtv = ['','']
	for(var i=0;i<images.length;i++){
		var info = images[i].info;
		if(info.indexOf('x')>0){
			rtv[0] = 'x'
			info.repeatX = true;
		}else if(info.indexOf('y')>0){
			rtv[1] = 'y'
			info.repeatY = true;
		}
	}
	return rtv.join('')
}
function spriteCSS(path,base,dest,source){
	return source.replace(/\bbackground\s*\:\s*url\(([^)]+)\)\s*(?=[;\}]|\/\*})/ig,
		function(a,url){
			url = url.replace(/['"]/g,'');
			var destFile = dest + spritePath(url,base);
			if(destFile){
				Env.getContentHash(destFile);
				var spriteInfo = Env.get(destFile,SPRITE_KEY);
				if(spriteInfo!=null){
					var filename = url.replace(/.*\/([^\/]+)$/,'$1');
					var fi = spriteInfo.imageMap[filename];
					var pos = -fi.x + 'px '+ -fi.y+'px';
					var repeat = '';
					var ext = repeat + pos + '';
					var css = 'url('+destFile+') '+ext;
					if(spriteInfo.alpha){
						//如果有Alpha 透明
						css += ';_background: none '+ext
							+'_filter: progid:DXImageTransform.Microsoft.AlphaImageLoader(src="url(' 
							+ destFile
							+ ')")';
					}
					$log.error(css)
					return 'background:'+css;
				}
			}
			return a;
		});
}
exports.spriteCSS = spriteCSS;
exports.spriteImage = spriteImage;
exports.spritePath = spritePath;
exports.setup = function(base,dest){
	Env.addBytesFilter(dest+'_/*.png',function(path){
		return spriteImage(path,base);
	});
	Env.addTextFilter('**.css',function(path,text){
		return spriteCSS(path,base,dest,text)
	})
}
//Env.addTextFilter('/static/_/*.png',imgageSprite);
//Env.addTextFilter('**.css',cssSprite);