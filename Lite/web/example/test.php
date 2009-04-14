<?php

require_once("../WEB-INF/classes/org/xidea/lite/TemplateEngine.php");
$liteBase = realpath("../");
//$liteService = "http://localhost:8080"; 
$path = "/example/test.xhtml";
if(array_key_exists('path',$_GET)){
	$path = $_GET['path'];
}
$liteCode = liteLoad($path);
$template = new Template($liteCode);


//ob_start();
$template->render(array("int1"=>1,"text1"=>'1'));
//$text = ob_get_contents();ob_end_clean();echo $text;
?>