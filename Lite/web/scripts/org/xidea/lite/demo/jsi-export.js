if(this.ActiveXObject ){
    if(location.protocol == "file:" || !this.XMLHttpRequest ){
        var xmlHttpRequstActiveIds = [
            //"Msxml2.XMLHTTP.6.0,"  //都IE7了，罢了罢了
            //"Msxml2.XMLHTTP.5.0,"  //office 的
            //"Msxml2.XMLHTTP.4.0,"
            //"MSXML2.XMLHTTP.3.0,"  //应该等价于MSXML2.XMLHTTP
            "MSXML2.XMLHTTP",
            "Microsoft.XMLHTTP"//IE5的，最早的XHR实现
            ];
        /**
         * 统一的 XMLHttpRequest 构造器（对于ie，做一个有返回值的构造器（这时new操作返回该返回值），返回他支持的AxtiveX控件）
         * 关于 XMLHttpRequest对象的详细信息请参考
         * <ul>
         *   <li><a href="http://www.w3.org/TR/XMLHttpRequest/">W3C XMLHttpRequest</a></li>
         *   <li><a href="http://www.ikown.com/manual/xmlhttp/index.htm">中文参考</a></li>
         *   <li><a href="http://msdn2.microsoft.com/en-us/library/ms762757(VS.85).aspx">MSXML</a></li>
         * </ul>
         * @id XMLHttpRequest 
         * @constructor
         */
        this.XMLHttpRequest = function(){
            while(true){
                try{
                     return new ActiveXObject(xmlHttpRequstActiveIds[0]);
                }catch (e){
                    if(!xmlHttpRequstActiveIds.shift()){
                        throw e;//not suport
                    }
                }
            }
        };
    }
}


if("org.xidea.jsi.boot:$log"){
    function $log(){
        var i = 0;
        var temp = [];
        if(this == $log){
            var bindLevel = arguments[i++];
            temp.push(arguments[i++],":\n\n");
        }
        while(i<arguments.length){
            var msg = arguments[i++]
            if(msg instanceof Object){
                temp.push(msg,"{");
                for(var n in msg){
                    temp.push(n,":",msg[n],";");
                }
                temp.push("}\n");
            }else{
                temp.push(msg,"\n");
            }
        }
        if(bindLevel >= 0){
            temp.push("\n\n继续弹出 ",temp[0]," 日志?");
            if(!confirm(temp.join(''))){
                consoleLevel = bindLevel+1;
            }
        }else{
            alert(temp.join(''));
        }
    }
    /**
     * 设置日志级别
     * 默认级别为debug
     * @protected
     */
    $log.setLevel = function(level){
        if(logLevelNameMap[level]){
            consoleLevel = level;
        }else{
            var i = logLevelNameMap.length;
            level = level.toLowerCase();
            while(i--){
                if(logLevelNameMap[i] == level){
                    consoleLevel = i;
                    return;
                }
            }
            $log("unknow logLevel:"+level);
        }
    };
    /*
     * @param bindLevel 绑定函数的输出级别，只有该级别大于等于输出级别时，才可输出日志
     */
    function buildLevelLog(bindLevel,bindName){
        var global = this;
        return function(){
            if(bindLevel>=consoleLevel){
                var msg = [bindLevel,bindName];
                msg.push.apply(msg,arguments);
                $log.apply($log,msg);
            }
            if(":debug"){
                if((typeof global.console == 'object') && (typeof console.log == 'function')){
                    var msg = [bindLevel,bindName];
                    msg.push.apply(msg,arguments);
                    console.log(msg.join(';'))
                    
                }
            }
        }
    }
    var logLevelNameMap = "trace,debug,info,warn,error,fatal".split(',');
    var consoleLevel = 1;
    /* 
     * 允许输出的级别最小 
     * @hack 先当作一个零时变量用了
     */
    var logLevelIndex = logLevelNameMap.length;
    //日志初始化 推迟到后面，方便var 压缩
    while(logLevelIndex--){
        var logName = logLevelNameMap[logLevelIndex];
        $log[logName] = buildLevelLog(logLevelIndex,logName);
    };
}