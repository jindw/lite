<?php
require_once("../WEB-INF/classes/org/xidea/lite/TemplateEngine.php");
$base = realpath("../");
$liteCode = getLite(array("/example/layout.xhtml","/example/test.xhtml"));
$template = new Template(json_decode($liteCode));
echo $template->render(array());
?>