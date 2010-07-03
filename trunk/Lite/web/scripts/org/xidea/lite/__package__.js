this.addScript('expression-token.js',
				[
					//函数
					"getTokenParam","hasTokenParam","getTokenParamIndex",'getTokenLength','findTokenType','findTokenText'
					,"BIT_*"
					,"VALUE_*"
					,"OP_*"
					]);

this.addScript('template.js',["Template","PLUGIN_DEFINE",'VAR_TYPE','XML_ATTRIBUTE_TYPE','ELSE_TYPE','PLUGIN_TYPE','CAPTRUE_TYPE','IF_TYPE','EL_TYPE','BREAK_TYPE','XML_TEXT_TYPE','FOR_TYPE']
                ,0
                ,['Translator','XMLParser','org.xidea.jsi:$log','evaluate']);


this.addScript('variable-finder.js','VarStatus'
                ,0
                ,["evaluate","ELTranslator",'ExpressionTokenizer','org.xidea.jsi:$log']);
this.addScript('js-el-translator.js','ELTranslator'
                ,0
                ,['getTokenParam','getPriority',"findTokenText","stringifyJSON",'ExpressionTokenizer','org.xidea.jsi:$log']);

this.addScript('js-translator.js',["Translator"]
                ,0
                ,["ELTranslator",'VarStatus','org.xidea.jsi:$log']);


this.addScript('expression.js',['evaluate','Expression']
                ,['getTokenParam']);

this.addScript('json-tokenizer.js','JSONTokenizer');

this.addScript('expression-tokenizer.js',['getPriority','ExpressionTokenizer']
                ,'JSONTokenizer'
                ,["hasTokenParam",'org.xidea.jsi:$log']);

this.addScript('find-el-end.js','findELEnd'
                ,0
                ,'org.xidea.jsi:$log');


this.addScript('parse-context.js','ParseContext'
                ,['xml-context.js','ParseChain']
                ,['parseEL','stringifyJSON']);

this.addScript('parse-chain.js','ParseChain');

this.addScript('xml-core-parser.js','parseCoreNode'
                ,0
                ,['org.xidea.jsi:$log','selectNodes']);

this.addScript('xml-default-parser.js','parseXMLNode'
                ,0);

this.addScript('xml-context.js',['loadXML','selectNodes']
                ,0
                ,['org.xidea.jsi:$log','org.xidea.jsidoc.util:XMLHttpRequest']);

this.addScript('xml-parser.js','XMLParser'
                ,'ParseContext'
                ,['parseText','parseCoreNode','parseXMLNode']);
                
this.addScript('text-parser.js',['parseText','parseEL']
                ,0
                ,['ExpressionTokenizer','org.xidea.jsi:$log','findELEnd']);

this.addScript('json.js',["stringifyJSON","parseJSON"]);

this.addDependence("*","expression-token.js",true);
this.addDependence("*","template.js",true);