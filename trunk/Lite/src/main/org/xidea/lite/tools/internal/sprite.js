var Env = require('./env');
/**
 * sprite base /module/static/_/
 * sprite file /module/static/css/a.css
 * 
 */
function spritePath(path,base){
	if(path.substring(0,base.length) == base){//需要放在相同的模块下
		path = path.substring(base.length).replace(/\/[^\/]+$/,'').replace(/\//g,'.');
		return base+path+'.png';
	}else{
		return null;
	}
}
var SpriteImpl = null
function spriteImage(path,base){
	var file = path.replace(/.*\//,'');
	var dir = base+file.replace(/\.png$/,'').replace(/\./g,'/');
	var files = Env.dir(dir);
	var images = [];
	for(var i=0;i<files.length;i++){
		var file = files[i];
		if(/\.(gif|png|jpe?g)$/.test(file)){
			images.push({
				name : file,
				//repeatX : false,
				//repeatY : false,
				//x:0,y:0,
				info:SpriteImpl.getImage(Env.root,dir+file)
			});
		}
	}
	var repeatX = initRepeat(images,'x');
	var repeatY = initRepeat(images,'y');
	if(repeatX && repeatY){
		//error
		throw new Error('repeat-x and repeat-y can not exits in same dir')
	}
	var width = initOffset(images,'x',!repeatY);
	var height = initOffset(images,'y',repeatY);
	var imgbuf = SpriteImpl.createImage(width,height);
	for(var i=0;i<images.length;i++){
		var img = images[i].info;
		imgbuf.drawImage(img.info,img.x,img.y);
	}
	
	
	Env.setAttribute(path, {
		alpha:true,
		imageType:'png',
		fileMap:{'a.png':{top:40,left:0}}
	})
	return imgbuf.toByteArray();
}
function spriteCSS(path,base,source){
	return source.replace(/\bbackground\s*\:\s*url\(([^)]+)\)\s*(?=[;\}]|\/\*})/ig,
		function(a,url){
			var dest = spritePath(url,base);
			if(dest){
				Env.getFiltedContent(dest);
				var spriteInfo = Env.getAttribute(path,"org.xidea.lite.tools.ImageSprite");
				var filename = url.replace(/.*\/([^\/]+)$/,'$1');
				var fi = spriteInfo.fileMap[filename];
				var pos = fi.left + ' '+fi.top;
				var repeat = '';
				var ext = repeat + pos + ';';
				var css = 'url('+dest+') '+ext;
				if(spriteInfo.alpha){
					//如果有Alpha 透明
					css += ' _background: none '+ext
						+'_filter: progid:DXImageTransform.Microsoft.AlphaImageLoader(src="url(' 
						+ dest
						+ ')")';
				}
				return 'background:'+css;
			}
			return a;
		});
}
exports.spriteCSS = spriteCSS;
exports.spriteImage = spriteImage;
exports.spritePath = spritePath;
exports.setup = function(base,dest){
	Env.addByteFilter(dest+'_/*.png',function(path){
		return spriteImage(path,base);
	});
	Env.addTextFilter('**.css',function(path,text){
		return spriteCSS(path,base,source)
	})
}
//Env.addTextFilter('/static/_/*.png',imgageSprite);
//Env.addTextFilter('**.css',cssSprite);