<?php
$_SERVER['REQUEST_URI'] = '/example/local-test.php';
require_once("../WEB-INF/classes/lite/TemplateEngine.php");
$engine = new TemplateEngine();
# 通过上下文数据方式传递模板参数：
$context = array(
	"int1"=>1,
	"text1"=>'1'
);
$i = 1000;
$time =  time() ;
while($i--){
    ob_start();
    $engine->render("/example/test.xhtml",$context);
    ob_end_clean();
}
echo "\r\n\n\n\n\n".(time()-$time);

//# 直接通过全局变量传递模板参数：
//$int1 = 1;
//$text1 = '1';
//$engine->render("/example/test.xhtml");
?>