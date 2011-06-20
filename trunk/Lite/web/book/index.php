<?php
require_once("../WEB-INF/classes/lite/LiteEngine.php");
$engine = new LiteEngine();
$engine->encoding="UTF-8";
header("Content-type: text/html;charset=UTF-8");
//初始化Lite Logo 的点阵数据 
$data = array();
if(array_key_exists('PATH_INFO',$_SERVER)){
    $path = $_SERVER['PATH_INFO'] ;
    $path = '/book'.$path;
}else{
    echo "<script>document.location='index.php/index.xhtml'</script>";
    exit();
}
if(realpath("../".$path)){
	if(strpos($path,".xhtml")>0){
		$engine->render($path,$data);
	}else{
		readfile("..".$path);
	}
}else{
    echo '<h3>找不到文件:'.$path.'</h3>';
}
?>