/*function * test(){
	yield 1;
	yield 2;
	yield this;
	yield 3;
}
//*/
//var g = test.apply({a:"aaaa"},[]);
//console.log( g.next());
//console.log( g.next());
//console.log( g.next().value.a);
//console.log( g.next());

var LiteEngine = require('lite');

describe('in process && out process the same',function(){
	it('inprocess',function(){
		var path = require('path');
		var buf = [];
		var response = {
			write:function(){
				buf.push.apply(buf,arguments)
			},
			//setHeader:function(){},
			end:function(){
				buf.push.apply(buf,arguments)
				console.log(buf.join(''))
			}
		}
		
		var root = path.resolve(__dirname);
		var litecache = path.join(root,'.litecache');
		var engine = new LiteEngine(root,{litecache:litecache,released:false});
		engine.render('/test.xhtml',{},response).then(function(rtv){
			console.log(rtv,buf.join('').length)
		})
		
		var parseLite =  require('../../main/js/index').parseLite;
		
		var root =path.dirname( path.resolve(__dirname,'../../'));
		console.log(root)
		//var xml = fs.
		var example1 = parseLite('/doc/example/extends-page.xhtml',{root:root})
		var html = example1({})
		console.log(html)
	})
});

