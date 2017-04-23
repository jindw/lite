var LiteEngine = require('lite');
var fs = require('fs');
var path = require('path');
var workDir = path.join(__dirname,'.tmp');
var assert = require('assert')
function resetDir(workDir){
	try{
		fs.mkdirSync(workDir);
	}catch(e){
	}
	try{
		var dirs = fs.readdirSync(workDir);
		for(var i=0;i<dirs.length;i++){
			var d = dirs[i];
			fs.unlinkSync(path.join(workDir,d))
		}
	}catch(e){}
}

function buildResp(buf){
	buf =buf|| [];
	return{
		write:function(){
			buf.push.apply(buf,arguments)
		},
		end:function(){
			buf.push.apply(buf,arguments)
		}
	}
}
var i = 0;
function runTemplate(source,model){
	resetDir(workDir);
	var buf = [];
	var resp = buildResp(buf)
	var engine = new LiteEngine(workDir,{litecache:workDir,released:false});
	var p  = '/test'+i++ +'.xhtml';
	fs.writeFileSync(path.join(workDir,p),source);
	return engine.render(p,model||{},resp).then(function(len){
		return [buf.join(''),engine.templateMap[p]];
	});
}
describe('yield test',function(){
	it('one yiled on one vars',function(done){
		runTemplate("<p>${a.a}/${a.b}<div c:if='${a.b}'>123</div></p>",
			{a:{a:1,b:2}}).then(function(args){
			var result = args[0];
			var template = args[1];
			var tplFn = template.fn+'';
			
			assert.deepEqual(result,"<p>1/2<div>123</div></p>");
			var m = /\byield\b/g;
			var count = 0
			//console.log(tplFn)
			while(m.exec(tplFn)){
				count++;
			}
			if(count!=1){
				assert.fail('require one and only one yield: but'+count+' received')
			}
			done();
		})
	})
	it('else block yiled check',function(done){
		runTemplate("<div><p c:if='${a.a}'>123</p><p c:elif='${b.a}'>123</p><p>${b.a}</p><p>${b.b}</p></div>",
			{a:{a:1,b:2},b:{a:1,b:2}}).then(function(args){
			var result = args[0];
			var template = args[1];
			var tplFn = template.fn+'';
			
			assert.deepEqual(result,"<div><p>123</p><p>1</p><p>2</p></div>");
			var m = /\byield\b/g;
			var count = 0
			while(m.exec(tplFn)){
				count++;
			}
			if(count!=3){//yield a yield b yielda 
				assert.fail('else if is nest in else, so the yield b hit twices: but'+count+' received')
			}
			done();
		})
	})
	it('for else block yiled check',function(done){
		runTemplate("<div><p c:for='${x:a.list}'>${x}</p><p c:elif='${b.a}'>123</p><p>${b.a}</p><p>${b.b}</p></div>",
			{a:{list:[1,2,3],a:1,b:2},b:{a:1,b:2}}).then(function(args){
			var result = args[0];
			var template = args[1];
			var tplFn = template.fn+'';
			
			assert.deepEqual(result,"<div><p>1</p><p>2</p><p>3</p><p>1</p><p>2</p></div>");
			var m = /\byield\b/g;
			var count = 0
			while(m.exec(tplFn)){
				count++;
			}
			if(count!=3){//yield a yield b yielda 
				assert.fail('else if is nest in else, so the yield b hit twices: but'+count+' received')
			}
			done();
		})
	})
	it('vars without yiled check',function(done){
		runTemplate("<div><c:var name='b' value='${ {a:1,b:2} }'><p c:for='${x:a.list}'>${x}</p><p c:elif='${b.a}'>123</p><p>${b.a}</p><p>${b.b}</p></div>",
			{a:{list:[1,2,3],a:1,b:2},b:{a:1,b:2}}).then(function(args){
			var result = args[0];
			var template = args[1];
			var tplFn = template.fn+'';
			
			assert.deepEqual(result,"<div><p>1</p><p>2</p><p>3</p><p>1</p><p>2</p></div>");
			var m = /\byield\b/g;
			var count = 0
			while(m.exec(tplFn)){
				count++;
			}
			if(count!=1){//yield a yield b yielda 
				assert.fail('else if is nest in else, so the yield b hit twices: but'+count+' received')
			}
			done();
		})
	})
});

