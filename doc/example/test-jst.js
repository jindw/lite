var parseLite = require('lite').parseLite;
var fn = parseLite("<xml attr='${b}${a}'>" +
		"<c:def name='xx(w)'>{{${w}}}</c:def>" +
		"${xx(123)}/${a.b}/${a}" +
		"<c:date date='${a.b}'>${ab}</c:date>" +
		"<c:date>${a.b}</c:date>" +
		"<div c:for='${a2:[1,2,3]  }'>${123}${456}</div></xml>",
			0,['a','b2']);
console.log(fn+"")
console.log(fn({a:{b:new Date}}))
