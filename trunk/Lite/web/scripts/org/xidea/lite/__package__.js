this.addScript('template.js',"Template"
               ,"evaluate"
               ,["parser.js","text-parser.js","xml-parser.js"]);

this.addScript('expression.js',["Expression","evaluate"]
              ,0
              ,"expression-token.js");
this.addScript('parser.js',"*"
               ,0
               ,["buildNativeJS"]);
this.addScript('text-parser.js',["TextParser","checkEL"]
               ,["Parser","parser.js"]
               ,["ExpressionTokenizer"]);
this.addScript('xml-parser.js',"XMLParser"
               ,["TextParser","parser.js"]); 
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



//this.addDependence('template.js',"xml-parser.js");