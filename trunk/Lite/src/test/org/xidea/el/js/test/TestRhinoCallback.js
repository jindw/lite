function TestRhinoCallback(){
}
TestRhinoCallback.prototype = {
    "test1" : function(){
         return base.test1.apply(base,arguments);
    }
}