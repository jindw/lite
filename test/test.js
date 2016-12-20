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


var LiteEngine = require('../lite-engine').LiteEngine;
var path = require('path');
var root = path.resolve(__dirname);
var litecache = path.join(root,'.litecache');
var engine = new LiteEngine(root,{litecache:litecache,released:false});
var buf = [];
var response = {
	write:function(){
		buf.push.apply(buf,arguments)
	},
	getHeader:function(){},
	setHeader:function(){},
	end:function(){
		buf.push.apply(buf,arguments)
		console.log(buf.join(''))
	}
}
engine.render('/test.xhtml',{},response)


var parseLite =  require('../index').parseLite;

var root =path.dirname( path.resolve(__dirname));
console.log(root)
//var xml = fs.
var example1 = parseLite('/example/extends-page.xhtml',{root:root})
var html = example1({})
console.log(html)