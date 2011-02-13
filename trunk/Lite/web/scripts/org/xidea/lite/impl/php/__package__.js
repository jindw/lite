this.addScript('php-el-translator.js',['stringifyPHPEL','stringifyPHP']
                ,0
                ,["org.xidea.el:*",
                	'org.xidea.jsi:$log']);
this.addScript('php-translator.js',["PHPTranslator"]
                ,["org.xidea.lite.impl:TranslateContext"]
                ,["PHPELTranslator","stringifyPHP",'org.xidea.jsi:$log']);

                
