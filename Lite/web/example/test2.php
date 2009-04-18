<?php
require_once("../WEB-INF/classes/lite/TemplateEngine.php");
$engine = new TemplateEngine(realpath("../"));

//$liteService = "http://localhost:8080"; 
$path = "/example/test.xhtml";
if(array_key_exists('path',$_GET)){
	$path = $_GET['path'];
}
$int1 = 1;
$text1 = '1';
//ob_start();
$engine->render($path);
//$engine->render($path,array("int1"=>1,"text1"=>'1'));
//$text = ob_get_contents();ob_end_clean();echo $text;
/*
class Test{
	var $base;
	function Test(&$base) {
		$this->base = &$base;
	}
	function &get(){
		return $this->base;
	}
}
$data = array(1,2,3);
$obj =  new Test($data);
$data[0] = 5;
$test = &$obj->get();
$data[0] = 6;
echo "<hr>";
echo $test[0];
$test = &$obj->get();
echo $test[0];
*/
?>