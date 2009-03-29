this.addScript('jsi-export.js',["XMLHttpRequest","$log"]);

this.addScript('lite-demo-util.js',["TestCase"]
               ,[
                   "org.xidea.lite.*",
                   "org.xidea.jsidoc.util:JSON",
                   "liteFormat"
               ]);

this.addScript('lite-formatter.js',["liteFormat"]
                ,[
                    "org.xidea.lite.*",
                    "org.xidea.jsidoc.util:JSON"
                ]);
