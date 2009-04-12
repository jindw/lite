<?php
require_once("../WEB-INF/classes/org/xidea/lite/TemplateEngine.php");
$base = realpath("../");
$path = "/example/test.xhtml";
if(array_key_exists('path',$_GET)){
	$path = $_GET['path'];
}
echo $path;
$liteCode = getLite(array("/example/layout.xhtml",$path));
$template = new Template(json_decode($liteCode));
echo $template->render(array());
?>