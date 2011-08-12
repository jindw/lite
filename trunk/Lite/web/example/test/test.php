<?php
require_once(dirname(__FILE__)."/../../WEB-INF/classes/lite/LiteEngine.php");
$engine = new LiteEngine();
$context = array(
	"int1"=>1,
	"site"=>'http://www.xidea.org/'
);
$engine->render("/example/test/index.xhtml",$context);
?>