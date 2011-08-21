<?php
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
		flush();
		$log =  lite_exec("svn up");
		echo '<p>'.htmlspecialchars($log).'</p>';
		flush();
	}
	if(array_key_exists('java',$_GET)){
		$wait = 3000;
    	echo "<p>正在执行文档编译...</p>";
		flush();
		$log = lite_exec("java -Dfile.encoding=UTF-8 -jar ../../../workspace/Lite2/build/dest/Lite.jar -includes /doc/guide/*.xhtml -root ../ -output ../ -translators php");
		echo '<p>'.htmlspecialchars($log).'</p>';
		flush();
	}
	echo "<p>文档$wait毫秒后跳转到文档首页!</p>";
	echo "<p><a href='index.php/guide/index.xhtml'>立即跳转</a>!</p>";
    echo "\n\n<script>setTimeout(function(){
    	document.location = ('index.php/guide/index.xhtml')
    },$wait);</script>";