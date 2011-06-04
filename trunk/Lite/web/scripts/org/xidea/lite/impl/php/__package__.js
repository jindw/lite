this.addScript('php-el-translator.js',['stringifyPHPEL','stringifyPHP']
                ,0
                ,["org.xidea.el:*",
                	'org.xidea.jsi:$log']);
this.addScript('php-translator.js',["PHPTranslator"]
                ,0
                ,["stringifyPHP",'org.xidea.jsi:$log']);

                
