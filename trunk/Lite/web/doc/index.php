<?php
require_once("../WEB-INF/classes/lite/LiteEngine.php");
$engine = new LiteEngine();
//初始化Lite Logo 的点阵数据 
$path = @$_SERVER['PATH_INFO'] ;



if($path){
	$path = '/doc'.$path;
}else{
	function lite_exec($cmd,$descriptorspec = array(array("pipe", "r"), array("pipe", "w"), array("pipe", "w") )){
		if(!function_exists('proc_open') ){
			trigger_error("Compile Error","ERROR:Missed php function: proc_open");
		}else if(!function_exists('stream_get_contents')){
			trigger_error("Compile Error","ERROR:Missed php function: stream_get_contents");
		}else{
			$process = proc_open($cmd, $descriptorspec, $pipes);
			if (is_resource($process)) {
			    $content= stream_get_contents($pipes[2]).stream_get_contents($pipes[1]);
			    if(function_exists('proc_close')){
			    	proc_close($process);
			    }
			    return $content;
			}else{
				trigger_error("Compile Error","ERROR: command execution failed, can not find the executable program:".$cmd);
			}
		}
	}
	header("Content-Type:text/html;charset=utf-8");
    echo "<a href='index.php/guide/index.xhtml'>跳转到文档首页</a>";
    $wait = 0;
	if(array_key_exists('svn',$_GET)){
		$wait = 3000;
    	echo "<p>正在执行svn更新...</p>";
		$log =  lite_exec("svn up");
		echo '<p>'.htmlspecialchars($log).'</p>';
	}
	if(array_key_exists('java',$_GET)){
		$wait = 3000;
    	echo "<p>正在执行文档编译...</p>";
		$log = lite_exec("java -jar ../WEB-INF/lib/Lite.jar -includes /doc/guide/*.xhtml -root ../ -output ../ -translators php");
		echo '<p>'.htmlspecialchars($log).'</p>';
	}
	echo "<p>文档$wait毫秒后跳转!</p>";
    echo "\n\n<script>setTimeout(function(){
    	document.location = ('index.php/guide/index.xhtml')
    },$wait);</script>";
    exit();
}
if(strpos($path,".xhtml")>0){
	if(!file_exists("..".$path)){
		$path2 = preg_replace('/[^\/]+$/','notfound.xhtml',$path);
		if(!file_exists('..'.$path2)){
			 echo '<h3>找不到文件:'.$path.'</h3>';
			 exit();
		}
	}
	
	//处理留言需求
	$content = @$_POST["content"];
	if(get_magic_quotes_gpc()){
		$content = stripslashes($content);
	}
	$json = "..".preg_replace('/\.xhtml$/','.json',$path);
	if($content){
		$username = $_POST["username"];
		$email = $_POST["email"];
		$data = @file_get_contents($json);
		$data = json_decode($data,true);
		if(!$data){
			$data = array("messages"=>array());
		}
		$item = array(
			"username"=>$username,
			"content"=>$content,
			"postTime"=>mktime()*1000,
			"email"=>$email
		);
		array_push($data['messages'] ,$item);
		$data = json_encode($data);
		file_put_contents($json,$data);
		echo json_encode($item);
		exit();
	}
	
	
	$json = realpath($json);
	$context = null;
	if($json){
		$context = file_get_contents($json);
		$context = json_decode($context,true);
	}
	if(!is_array($context)){
		$context = array();
	}
	$engine->render($path2?$path2:$path,$context);
}else{
	$boot = $path == '/doc/boot.js'?realpath('../WEB-INF/classes/lite/boot.js'):null;
	if(array_key_exists('@',$_GET)){
		if($boot){
			header("ETag:".filemtime($boot).filesize($boot));
		}else{
			header("Expires: ".gmdate("D, d M Y H:i:s", time()+315360000)." GMT");
			header("Cache-Control: max-age=315360000");
		}
	}
	if($boot){
		readfile($boot);
	}else if(realpath("..".$path)){
		if(strpos($path,".css")>0){
			header("Content-type: text/css;charset=UTF-8");
		}
		readfile("..".$path);
	}else{
	    echo '<h3>找不到文件:'.$path.'</h3>';
	}
}
?>