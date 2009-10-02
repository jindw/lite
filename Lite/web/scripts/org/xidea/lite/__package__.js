this.addScript('expression-token.js',
				[
					//函数
					"getParamCount",'getTokenLength','findTokenType','toTokenString','findTokenText'
					//BIT CONSTANTS
					,"BIT_PRIORITY_SUB","BIT_PRIORITY","BIT_PARAM"
					//VALUES
					,"VALUE_CONSTANTS","VALUE_VAR","VALUE_NEW_LIST","VALUE_NEW_MAP"
					//9
					,"OP_GET_PROP","OP_GET_STATIC_PROP","OP_INVOKE_METHOD","OP_INVOKE_METHOD_WITH_STATIC_PARAM","OP_INVOKE_METHOD_WITH_ONE_PARAM"
					//8
					,"OP_NOT","OP_POS","OP_NEG"
					//7
					,"OP_MUL","OP_DIV","OP_MOD"
					//6
					,"OP_ADD","OP_SUB"
					//5
					,"OP_LT","OP_GT","OP_LTEQ","OP_GTEQ"
					//4
					,"OP_EQ","OP_NOTEQ"
					//3
					,"OP_AND","OP_OR"
					//2
					,"OP_QUESTION","OP_QUESTION_SELECT"
					//1
					,"OP_PARAM_JOIN"
					,"OP_MAP_PUSH"
					]);

this.addScript('template.js',["Template","PLUGIN_DEFINE",'VAR_TYPE','XML_ATTRIBUTE_TYPE','ELSE_TYPE','ADD_ON_TYPE','CAPTRUE_TYPE','IF_TYPE','EL_TYPE','BREAK_TYPE','XML_TEXT_TYPE','FOR_TYPE']
                ,0
                ,['XMLParser','org.xidea.jsidoc.util:$log','evaluate']);


this.addScript('variable-finder.js','VarStatus'
                ,0
                ,["evaluate","ELTranslator",'ExpressionTokenizer','org.xidea.jsidoc.util:$log']);
this.addScript('js-el-translator.js','ELTranslator'
                ,0
                ,['getPriority',"findTokenText","stringifyJSON",'ExpressionTokenizer','org.xidea.jsidoc.util:$log']);

this.addScript('js-translator.js',["Translator"]
                ,0
                ,["ELTranslator",'VarStatus','org.xidea.jsidoc.util:$log']);


this.addScript('expression.js',['evaluate','Expression']
                ,0);

this.addScript('json-tokenizer.js','JSONTokenizer');

this.addScript('expression-tokenizer.js',['getPriority','ExpressionTokenizer']
                ,'JSONTokenizer'
                ,['org.xidea.jsidoc.util:$log']);

this.addScript('find-el-end.js','findELEnd'
                ,0
                ,'org.xidea.jsidoc.util:$log');


this.addScript('parse-context.js','ParseContext'
                ,['xml-context.js','ParseChain']
                ,['Translator','parseEL','stringifyJSON']);

this.addScript('parse-chain.js','ParseChain');

this.addScript('xml-core-parser.js','parseCoreNode'
                ,0
                ,['org.xidea.jsidoc.util:$log','selectNodes']);

this.addScript('xml-default-parser.js','parseXMLNode'
                ,0);

this.addScript('xml-context.js',['loadXML','selectNodes','parseXMLText']
                ,0
                ,['org.xidea.jsidoc.util:$log','org.xidea.jsidoc.util:XMLHttpRequest']);

this.addScript('xml-parser.js','XMLParser'
                ,'ParseContext'
                ,['parseText','parseCoreNode','parseXMLNode']);
                
this.addScript('text-parser.js',['parseText','parseEL']
                ,0
                ,['ExpressionTokenizer','org.xidea.jsidoc.util:$log','findELEnd']);

this.addScript('json.js',["stringifyJSON","parseJSON"]);

this.addDependence("*","expression-token.js",true);
this.addDependence("*","template.js",true);