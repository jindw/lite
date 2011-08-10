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
    echo "<a href='index.php/guide/index.xhtml'>跳转到文档首页</a>"
	if(array_key_exists('svn',$_GET)){
    	echo "<p>正在执行svn更新...</p>";
		$log =  lite_exec("svn up");
		echo '<p>'.htmlspecialchars($log).'</p>';
	}
	if(array_key_exists('java',$_GET)){
    	echo "<p>正在执行文档编译...</p>";
		$log = lite_exec("java -jar ../WEB-INF/lib/Lite.jar -includes /doc/guide/*.xhtml -root ../ -output ../ -translators php");
		echo '<p>'.htmlspecialchars($log).'</p>';
	}
	
    echo "\n\n<script>setTimeout(function(){
    	document.location = ('index.php/guide/index.xhtml')
    },3000);</script>";
    exit();
}
if($path == '/doc/boot.js'){
	readfile("../WEB-INF/classes/lite/boot.js");
}else if(strpos($path,".xhtml")>0){
	if(!file_exists("..".$path)){
		$path2 = preg_replace('/[^\/]+$/','notfound.xhtml',$path);
		if(!file_exists('..'.$path2)){
			 echo '<h3>找不到文件:'.$path.'</h3>';
			 exit();
		}
	}
	
	//处理留言需求
	$content = @$_POST["content"];
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
}else if(realpath("..".$path)){
	if(strpos($path,".css")>0){
		header("Content-type: text/css;charset=UTF-8");
	}
	readfile("..".$path);
}else{
    echo '<h3>找不到文件:'.$path.'</h3>';
}
?>