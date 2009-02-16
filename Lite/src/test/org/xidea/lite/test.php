<?php
require('Expression.php');
class Test {
	function test($v) {
		return $v *3;
	}
}
$el = json_decode('[[-1,"object"],[48,"test"],[-3],[0,123],[1,null],[81]]');
echo (evaluate($el,array("object"=>new Test())));


?>