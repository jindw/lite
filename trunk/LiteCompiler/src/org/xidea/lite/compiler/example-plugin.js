//添加Common Template 语法支持
$import("org.xidea.lite.parser.impl.ELParser");
context.addTextParser(ELParser.IF);
context.addTextParser(ELParser.FOR);
context.addTextParser(ELParser.ELSE);
context.addTextParser(ELParser.END);
context.addTextParser(ELParser.VAR);
context.addTextParser(ELParser.CLIENT);

/*
//一个简单测试，将当前文档的标记名称全部打印出来
var testParser = {
	parse:function(context,chain,pagerNode){
		context.append(pagerNode.nodeName);
		var cs = pagerNode.childNodes;
		for(var i=0;i<cs.length;i++){
			context.parse(cs.item(i));
		}
	}
}
context.addNodeParser(testParser );
*/