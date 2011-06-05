this.addScript('php-el-translator.js',['stringifyPHPEL','stringifyPHP','php2jsBoolean','isSimplePHPEL']
                ,0
                ,["org.xidea.el:*",
                	'org.xidea.jsi:$log']);
this.addScript('php-translator.js',["PHPTranslator"]
                ,'org.xidea.lite.impl:*'
                ,['php2jsBoolean','isSimplePHPEL',"stringifyPHP","stringifyPHPEL"
                	,'org.xidea.jsi:$log'
                	,'org.xidea.lite.util:stringifyJSON'
                	]);

                
