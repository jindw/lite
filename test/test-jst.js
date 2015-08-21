var parseLite = require('lite').parseLite;
console.log(
	parseLite("<xml attr='${b}'><c:def name='xx(w)'>x${w}xxxx</c:def>${xx(123)}<c:date date='${a.b}'>${ab}</c:date></xml>",
			0,['a','b2'])+"")