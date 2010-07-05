this.addScript('expression-token.js',
				[
					//函数
					"optimizeEL","getTokenParam","hasTokenParam","getTokenParamIndex",'getTokenLength','findTokenType','findTokenText'
					,"BIT_*"
					,"VALUE_*"
					,"OP_*"
					]
				,0
				,"evaluate");


this.addScript('js-el-translator.js','ELTranslator'
                ,0
                ,['getTokenParam','getPriority',"findTokenText","org.xidea.lite.impl:stringifyJSON",'ExpressionTokenizer','org.xidea.jsi:$log']);


this.addScript('expression.js',['evaluate','Expression']
                ,['getTokenParam']);

this.addScript('json-tokenizer.js','JSONTokenizer');

this.addScript('expression-tokenizer.js',['getPriority','ExpressionTokenizer']
                ,'JSONTokenizer'
                ,["optimizeEL","hasTokenParam",'org.xidea.jsi:$log']);

this.addScript('el-util.js','findELEnd'
                ,0
                ,'org.xidea.jsi:$log');


this.addDependence("*","expression-token.js",true);
