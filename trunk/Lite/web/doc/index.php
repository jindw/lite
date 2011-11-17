<?php
require_once("../WEB-INF/classes/lite/LiteEngine.php");
$engine = new LiteEngine();
$i18n_path = $engine->root.'/WEB-INF/i18n/zh/';
if(@$_COOKIE["i18n_lang"] == 'en'){
	$i18n_path = $engine->root.'/WEB-INF/i18n/en/';
	if(realpath($i18n_path)){
		$engine->i18ncode = $i18n_path;
	}
}
$engine->debug = true;
$engine->autocompile=false ;
//初始化Lite Logo 的点阵数据 
$path = @$_SERVER['PATH_INFO'] ;
$action = @$_POST['i18n_action'];
if($action == 'save'){
	if(@$i18n_path){
		$i18n_id =  $_POST['i18n_id'];
		$i18n_value =  $_POST['i18n_value'];
		$pos = strpos($i18n_id,'__');
		$file = $i18n_path.substr($i18n_id,0,$pos).'.i18n';
		if(file_exists($file)){
			$i18n_data = json_decode(file_get_contents($file),true);
		}else{
			$i18n_data = array();
		}
		$i18n_data[$i18n_id] = $i18n_value;
		file_put_contents($file,json_encode($i18n_data));
		echo '{"success":true,"path":"'+$file+'"}';
	}else{
	}
	exit();
}

if($path == '/guide/index.xhtml'){
        $ref = @$_SERVER['HTTP_REFERER'];
        if($ref && strlen($ref)>=40 && !substr_compare($ref,'http://www.qconhangzhou.com/Speaker.aspx',0,40,true)){
                header('Location:http://www.xidea.org/lite/');
                exit();
        }
}

if($path){
	$path = '/doc'.$path;
}else{
	header("Content-Type:text/html;charset=utf-8");
	echo "<p><a href='index.php/guide/index.xhtml'>跳转到文档首页</a>!</p>";
    echo "\n\n<script>
    	document.location = ('index.php/guide/index.xhtml')
    	</script>";
    exit();
}
if(strpos($path,".xhtml")>0){
	$path2 = $path;
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
		$ip = $_SERVER["REMOTE_ADDR"];
		$ipinfo = @file_get_contents('ip.json');
		if($ipinfo){
			$ipinfo = json_decode($ipinfo,true);
			if(is_array($ipinfo)){
				if(@$ipinfo[$ip] && @$ipinfo[$ip]+5>time()){
					$ipinfo[$ip] = time();
					@file_put_contents('ip.json',json_encode($ipinfo));
					echo json_encode('{"error":"Forbidden!!!!\n: post too frequently!!"}');
					exit();
				}else{
					$ipinfo[$ip] = time();
				}
			}else{
				$ipinfo =array($ip=>time());
			}
		}else{
			$ipinfo =array($ip=>time());
		}
		$ipinfo && @file_put_contents('ip.json',json_encode($ipinfo));
		
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
			"postTime"=>time()*1000,
			"ip"=>$ip,
			'id'=>uniqid(),
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
	$engine->render($path2,$context);
}else{
	$boot = $path == '/doc/boot.js'?realpath('../WEB-INF/classes/lite/boot.js'):null;
	if(array_key_exists('@',$_GET)){
		if($boot){
			$old_etag = array_key_exists('HTTP_IF_NONE_MATCH',$_SERVER)?@$_SERVER('HTTP_IF_NONE_MATCH'):0;
			$etag = @(filemtime($boot).'-'.filesize($boot));
			
			if($old_etag && $old_etag == $etag ){
				header('HTTP/1.1 304 Not Modfied');  
				//header('X-INFO-OLD:'.$old_etag);
				//header('X-INFO:'.$etag);
				//header("ETag:".$etag);
				//header("Content-Type:text/javascript;charset=utf-8");
				//exit();
			}else{
				//header("ETag:".$etag);
				//header("Content-Type:text/javascript;charset=utf-8");
			}
		}else{
			header("Expires: ".gmdate("D, d M Y H:i:s", time()+315360000)." GMT");
			header("Cache-Control: max-age=315360000");
		}
	}
	if($boot){
		header("Content-Type:text/javascript;charset=utf-8",true);
		header("X-TYPE: bootfile");
		@readfile($boot);
	}else if(realpath("..".$path)){
		if(strpos($path,".css")>0){
			header("Content-type: text/css;charset=UTF-8",true);
		}else if(strpos($path,".js")>0){
			header("Content-type: text/javascript;charset=UTF-8",true);
		}
		readfile("..".$path);
	}else{
	    echo '<h3>找不到文件:'.$path.'</h3>';
	}
}
?>