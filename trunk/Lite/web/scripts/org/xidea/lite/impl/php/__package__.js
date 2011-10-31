this.addScript('php-el-translator.js',['stringifyPHPEL','stringifyPHP','php2jsBoolean','isSimplePHPEL']
                ,0
                ,["org.xidea.el:*",
                	"org.xidea.lite.impl.js:GLOBAL_DEF_MAP",
                	'org.xidea.jsi:console']);
this.addScript('php-translator.js',["PHPTranslator"]
                ,['org.xidea.lite.impl:*',"org.xidea.el:*",
                	"org.xidea.lite.impl.js:GLOBAL_DEF_MAP",
                	"org.xidea.lite.impl.js:GLOBAL_VAR_MAP"
                ]
                ,['php2jsBoolean','isSimplePHPEL',"stringifyPHP","stringifyPHPEL"
                	,'org.xidea.jsi:console'
                	,'org.xidea.lite.util:stringifyJSON'
                	]);

                
