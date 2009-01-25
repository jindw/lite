this.addScript('template.js',"Template"
               ,"_evaluate"
               ,["parser.js","text-parser.js","xml-parser.js"]);

this.addScript('expression.js',["Expression","_evaluate"]
              ,0
              ,"expression-token.js");
this.addScript('parser.js',"*"
               ,0
               ,["buildNativeJS"]);
this.addScript('text-parser.js',["TextParser"]
               ,["Parser","parser.js"]
               ,["parseNativeEL","ExpressionTokenizer",]);
this.addScript('xml-parser.js',"XMLParser"
               ,["TextParser","parser.js"]); 
this.addScript("expression-token.js","*");
this.addScript("json-tokenizer.js","JSONTokenizer");
this.addScript("expression-tokenizer.js","ExpressionTokenizer"
               ,"JSONTokenizer"
               ,"expression-token.js");

this.addScript("native-parser.js",["parseNativeEL","buildNativeJS"]
               ,0
               ,"parser.js");

//this.addDependence('template.js',"xml-parser.js");