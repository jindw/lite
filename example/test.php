<?php
require_once(dirname(__FILE__)."/../WEB-INF/classes/lite/LiteEngine.php");
$engine = new LiteEngine();
# 通过上下文数据方式传递模板参数：
$context = array(
	"int1"=>1,
	"text1"=>'1'
);
$engine->render("/example/extends-page.xhtml",$context);
?>