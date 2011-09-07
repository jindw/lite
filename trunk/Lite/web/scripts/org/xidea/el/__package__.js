this.addScript('expression-token.js',
				[
					//函数
					"getTokenParam","hasTokenParam","getTokenParamIndex",'getTokenLength','findTokenType','findTokenText'
					,"getELType","addELQute"
					
					,"BIT_ARGS","BIT_PRIORITY","BIT_PRIORITY_SUB"
					,"OP_ADD","OP_AND","OP_BIT_AND","OP_BIT_NOT","OP_BIT_OR","OP_BIT_XOR","OP_DIV","OP_EQ","OP_EQ_STRICT","OP_GET","OP_GT","OP_GTEQ","OP_IN","OP_INVOKE","OP_JOIN","OP_LSH","OP_LT","OP_LTEQ","OP_MOD","OP_MUL","OP_NE","OP_NEG","OP_NE_STRICT","OP_NOT","OP_OR","OP_POS","OP_PUT","OP_QUESTION","OP_QUESTION_SELECT","OP_RSH","OP_SUB","OP_URSH"
					,"TYPE_ANY","TYPE_ARRAY","TYPE_BOOLEAN","TYPE_MAP","TYPE_NULL","TYPE_NUMBER","TYPE_STRING","TYPE_TOKEN_MAP"
					,"VALUE_CONSTANTS","VALUE_LIST","VALUE_MAP","VALUE_VAR"
					//,"BIT_*"
					//,"VALUE_*"
					//,"OP_*"
					//,"TYPE_*"
					]
				,"org.xidea.jsi:$log"
				,["evaluate",'getPriority']);
				
				
				
this.addScript('expression.js',['evaluate','Expression']
                ,['getTokenParam']
                ,"ExpressionTokenizer");

this.addScript('json-tokenizer.js','JSONTokenizer');

this.addScript('expression-tokenizer.js',['getPriority','ExpressionTokenizer']
                ,'JSONTokenizer'
                ,["hasTokenParam",'org.xidea.jsi:$log']);

this.addScript('el-util.js','findELEnd'
                ,0
                ,'org.xidea.jsi:$log');

this.addScript('el-translator.js',['stringifyJSEL']
                ,0
                ,[
                	"org.xidea.lite.util:stringifyJSON",
                	'org.xidea.jsi:$log']);
this.addDependence("*","expression-token.js",true);


