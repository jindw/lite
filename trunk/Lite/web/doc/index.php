<?php
require_once("../WEB-INF/classes/lite/LiteEngine.php");
$engine = new LiteEngine();
//初始化Lite Logo 的点阵数据 
$data = array();
$path = @$_SERVER['PATH_INFO'] ;
if($path){
	$path = '/doc'.$path;
}else{
    echo "<script>document.location='index.php/guide/index.xhtml'</script>";
    exit();
}
if(realpath("..".$path)){
	if(strpos($path,".xhtml")>0){
		header("Content-type: text/html;charset=UTF-8");
		$engine->render($path,$data);
	}else{
		if(strpos($path,".css")>0){
			header("Content-type: text/css;charset=UTF-8");
		}
		readfile("..".$path);
	}
}else{
    echo '<h3>找不到文件:'.$path.'</h3>';
}
?>