<?php
require('Expression.php');
class TestBean {
    var $rate=100;
    function test($arg1){
       return $arg1 * $this->rate;
    }
}
/* 难道php就没有办法动态调用对象方法吗？ */
function test($v){
    return $v * 3;
}

//print("object.test(123);#123*3=369")
$el = json_decode('[[-1,"object"],[48,"test"],[-3],[0,123],[1,null],[81]]');
$data = array("object"=>new TestBean());


echo (evaluate($el,array("object"=>new TestBean())));


?>