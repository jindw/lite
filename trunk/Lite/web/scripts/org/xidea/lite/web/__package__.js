this.addScript('server-compiler.js',["WebCompiler"]
               ,[
                   "org.xidea.lite.impl.php:PHPTranslator",
                   "org.xidea.lite.parse.*",
                   "org.xidea.lite.util.*",
                   "org.xidea.jsi:console"
               ]
               ,"org.xidea.jsidoc.util:XMLHttpRequest");
               
this.addScript("data-view.js","DataView",
				[
					"org.xidea.lite.util:stringifyJSON"
					,"org.xidea.jsidoc.util:XMLHttpRequest"
					,"org.xidea.lite.util:base64Encode"
				])
