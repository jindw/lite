<?php
require('Expression.php');
require('Template.php');
class TestBean {
    var $rate=100;
    function test($arg1){
       return $arg1 * $this->rate;
    }
}

//print("object.test(123);#123*3=369")
$el = json_decode('[[-1,"object"],[48,"test"],[-3],[0,123],[1,null],[81]]');
$data = array("object"=>new TestBean());
echo (evaluate($el,array("object"=>new TestBean())));


echo ("v1 = 1;");
$el= json_decode('[[-1,"v1"]]');
//echo (evaluate($el,json_decode('{"v1":1}')));

?>