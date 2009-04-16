<?php
require_once("../WEB-INF/template.php");
$engine = new TemplateEngine();
//# 通过上下文数据方式传递模板参数：
//$engine->render("/example/test.xhtml",array("int1"=>1,"text1"=>'1'));

# 直接通过全局变量传递模板参数：
$int1 = 1;
$text1 = '1';
$engine->render("/example/test.xhtml");
?>