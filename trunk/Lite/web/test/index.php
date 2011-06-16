<?php
$path = @$_GET['path'];
if($path){
	require_once("../WEB-INF/classes/lite/LiteEngine.php");
	// 通过上下文数据方式传递模板参数：
	$engine = new LiteEngine();
	$context = array("int1"=>1,"text1"=>'1');
	$engine->render($path,$context);
}else{
	$dir = realpath('./');
	$dir = dir($dir);
	while (false !== ($group = $dir->read())) {
		$subdir = realpath("./$group");
		if(substr($group,0,1) != '.' && is_dir($subdir)){
			$subdir = dir($subdir);
			while (false !== ($path = $subdir->read())) {
				if(substr($path, -6) == '.xhtml'){
					$path = "/test/$group/$path";
					echo "<a href='index.php?path=${path}'>${path}</a><hr>";
				}
			}
			$subdir->close();
		}
	}
	$dir->close();
}