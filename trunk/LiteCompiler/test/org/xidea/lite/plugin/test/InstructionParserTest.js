function TestInstructionParser(){
}
TestInstructionParser.prototype.parse = function(context,text,start){
     context.append(text.substring(start));
     return text.length;
}
TestInstructionParser.prototype.findStart = function(context,text,start){
     return 1*text.charAt(start);
}
context.addInstructionParser(new TestInstructionParser());
$import("org.xidea.lite.parser.impl.HTMLParser");
$import("org.xidea.lite.parser.impl.ELParser");

context.addNodeParser(new HTMLParser());
context.addInstructionParser(ELParser.IF);
context.addInstructionParser(ELParser.FOR);
context.addInstructionParser(ELParser.ELSE);
context.addInstructionParser(ELParser.END);
context.addInstructionParser(ELParser.VAR);
//print(Node.ELEMENT_NODE)