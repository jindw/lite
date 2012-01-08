var XMLHttpRequest = window.XMLHttpRequest;
if(!XMLHttpRequest && window.ActiveXObject){
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
    var XMLHttpRequest = function(){
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

if(typeof require == 'function'){
exports.XMLHttpRequest=XMLHttpRequest;
}