<?php
require_once("../WEB-INF/classes/org/xidea/lite/TemplateEngine.php");
$engine = new TemplateEngine(realpath("../"));

$int1 = 1;
$text1 = '1';
$engine->render("/example/test.xhtml");
?>