var LiteEngine = require('lite').LiteEngine;
var engine = new LiteEngine(require('path').join(__dirname,'././'));
require('http').createServer(function (request, response) {
	if(/\.xhtml$/.test(request.url)){
		var model = {};
    	engine.render(request.url,model,request,response);
	}
}).listen(2012);

