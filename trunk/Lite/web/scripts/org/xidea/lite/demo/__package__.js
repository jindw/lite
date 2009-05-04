this.addScript('json.js',["JSON"]);

this.addScript('lite-demo-util.js',["TestCase"]
               ,[
                   "org.xidea.lite.*",
                   "JSON",
                   "liteFormat",
                   "org.xidea.jsidoc.util:$log"
               ]
               ,"org.xidea.jsidoc.util:XMLHttpRequest");

this.addScript('lite-formatter.js',["liteFormat"]
                ,[
                    "org.xidea.lite.*",
                    "JSON"
                ]);
