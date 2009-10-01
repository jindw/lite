this.addScript('expression-token.js',
				[
					//函数
					'getTokenLength','findTokenType','toTokenString','findTokenText'
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


this.addScript('parse-context.js','ParseContext'
                ,['xml-context.js','ParseChain']
                ,["PLUGIN_DEFINE",'buildNativeJS','parseEL','stringifyJSON','VAR_TYPE','XML_ATTRIBUTE_TYPE','ELSE_TYPE','ADD_ON_TYPE','CAPTRUE_TYPE','IF_TYPE','EL_TYPE','XML_TEXT_TYPE','FOR_TYPE']);

this.addScript('js-translator.js',["Translator",'buildNativeJS','checkEL']
                ,0
                ,['org.xidea.jsidoc.util:$log','VarStatus','VAR_TYPE','XML_ATTRIBUTE_TYPE','ELSE_TYPE','ADD_ON_TYPE','CAPTRUE_TYPE','IF_TYPE','EL_TYPE','XML_TEXT_TYPE','FOR_TYPE']);

this.addScript('variable-finder.js','VarStatus'
                ,0
                ,["PLUGIN_DEFINE",'ExpressionTokenizer','org.xidea.jsidoc.util:$log','VALUE_VAR','OP_STATIC_GET_PROP','VALUE_LAZY','VAR_TYPE','XML_ATTRIBUTE_TYPE','ELSE_TYPE','ADD_ON_TYPE','CAPTRUE_TYPE','IF_TYPE','XML_TEXT_TYPE','EL_TYPE','FOR_TYPE']);
this.addScript('js-el-translator.js','ELTranslator'
                ,0
                ,["findTokenText","stringifyJSON",'ExpressionTokenizer','org.xidea.jsidoc.util:$log']);

this.addScript('xml-context.js',['loadXML','selectNodes','parseXMLText']
                ,0
                ,['org.xidea.jsidoc.util:$log','org.xidea.jsidoc.util:XMLHttpRequest']);

this.addScript('xml-parser.js','XMLParser'
                ,'ParseContext'
                ,['parseText','parseCoreNode','parseXMLNode']);

this.addScript('expression.js',['evaluate','Expression']
                ,0
                ,['OP_LTEQ','OP_MAP_PUSH','OP_AND','OP_OR','VALUE_VAR','OP_ADD','OP_MUL','OP_GT','OP_GTEQ','OP_DIV','OP_NOTEQ','OP_INVOKE_METHOD','OP_QUESTION','OP_POS','VALUE_LAZY','OP_MOD','OP_EQ','OP_SUB','OP_GET_PROP','OP_NEG','OP_QUESTION_SELECT','OP_PARAM_JOIN','VALUE_CONSTANTS','OP_STATIC_GET_PROP','OP_NOT','OP_LT','VALUE_NEW_MAP','VALUE_NEW_LIST']);

this.addScript('json-tokenizer.js','JSONTokenizer');


this.addScript('parse-chain.js','ParseChain');

this.addScript('xml-core-parser.js','parseCoreNode'
                ,0
                ,["PLUGIN_DEFINE",'org.xidea.jsidoc.util:$log','selectNodes','ELSE_TYPE','EL_TYPE']);

this.addScript('xml-default-parser.js','parseXMLNode'
                ,0
                ,['XML_ATTRIBUTE_TYPE','EL_TYPE','XML_TEXT_TYPE']);

this.addScript('text-parser.js',['parseText','parseEL']
                ,0
                ,['checkEL','ExpressionTokenizer','org.xidea.jsidoc.util:$log','findELEnd','XML_ATTRIBUTE_TYPE','EL_TYPE','XML_TEXT_TYPE']);

this.addScript('expression-tokenizer.js','ExpressionTokenizer'
                ,'JSONTokenizer'
                ,['org.xidea.jsidoc.util:$log','OP_SUB','OP_GET_PROP','OP_NEG','OP_MAP_PUSH','OP_QUESTION_SELECT','getTokenLength','OP_PARAM_JOIN','OP_AND','VALUE_VAR','OP_OR','VALUE_CONSTANTS','OP_ADD','OP_STATIC_GET_PROP','findTokenType','toTokenString','VALUE_NEW_MAP','OP_INVOKE_METHOD','VALUE_NEW_LIST','OP_QUESTION','OP_POS','VALUE_LAZY']);

this.addScript('find-el-end.js','findELEnd'
                ,0
                ,'org.xidea.jsidoc.util:$log');

this.addScript('json.js',["stringifyJSON","parseJSON"]);
