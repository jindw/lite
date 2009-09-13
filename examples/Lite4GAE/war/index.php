<?php
/**
 * 如果是调试服务器上使用，相关文件系统已内置在服务器中。
 * 不需要拷贝模板引擎，也可以在网站目录上覆盖系统默认实现。
 */
require_once("WEB-INF/classes/lite/TemplateEngine.php");
$engine = new TemplateEngine();

//初始化模拟数据（当能你也可以通过其他方式模拟，比如直接编写php关联数组）
$data = json_decode(file_get_contents("index.json"),true);
//渲染模板
$engine->render("/index.xhtml",$data);
      
?>