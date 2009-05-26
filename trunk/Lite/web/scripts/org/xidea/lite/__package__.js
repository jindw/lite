this.addScript('template.js',"Template"
               ,0
               ,["parser.js","evaluate","xml-parser.js"]);

this.addScript('expression.js',["Expression","evaluate"]
              ,0
              ,"expression-token.js");
this.addScript('parser.js',"*"
               ,0
               ,["buildNativeJS"]);
this.addScript('text-parser.js',["TextParserOld","checkEL"]
               ,["Parser","parser.js"]
               ,["ExpressionTokenizer","findELEnd","org.xidea.jsidoc.util:XMLHttpRequest"]); 
this.addScript('xml-parser.js',"XMLParser"
               ,["TextParserOld","parser.js"]
               ,"org.xidea.jsidoc.util:XMLHttpRequest"); 
this.addScript("expression-token.js","*");
this.addScript("json-tokenizer.js","JSONTokenizer");
this.addScript("expression-tokenizer.js","ExpressionTokenizer"
               ,"JSONTokenizer"
               ,"expression-token.js");

this.addScript("native-compiler.js",["buildNativeJS"]
               ,0
               ,["parser.js","findStatus","checkEL"]);
this.addScript("variable-finder.js",["findStatus"]
               ,0
               ,["parser.js","expression-token.js","ExpressionTokenizer"]);

this.addScript("find-el-end.js","findELEnd")

this.addDependence('*',"org.xidea.jsidoc.util:$log");