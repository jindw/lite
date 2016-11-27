var parseLite = require('lite').parseLite;
var fn = parseLite("<xml attr='${b}'>" +
		"<c:def name='xx(w)'>{{${w}}}</c:def>" +
		"${xx(123)}" +
		"<c:date date='${a.b}'>${ab}</c:date>" +
		"<c:date>${a.b}</c:date>" +
		"<div>${123}${456}</div></xml>",
			0,['a','b2']);
console.log(fn+"")
console.log(fn({a:new Date}))
