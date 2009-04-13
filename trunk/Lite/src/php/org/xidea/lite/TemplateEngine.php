<?php
require_once("Template.php");
$liteBase = $_SERVER["DOCUMENT_ROOT"];
$liteService = "http://litecompiler.appspot.com"; 

function liteLoad($path){
    global $liteBase;
    $compileDir = "$liteBase/WEB-INF/classes/phplite/";
	if(!file_exists($compileDir)){
		mkdir($compileDir);
	}
    $liteFile = $compileDir.rawurlencode($path);
    if(file_exists($liteFile)){
    	$lite = json_decode(file_get_contents($liteFile));
    	$paths = $lite[0];
    	$liteTime = filemtime($liteFile);
    	$fileTime = $liteTime;
    	$i=count($paths);
		while($i--){
			$fileTime = max($fileTime,filemtime("$liteBase$paths[$i]"));
		}
		if($fileTime<=$liteTime){
			return $lite[1];
		}
    }
    $lite = liteCompile($path);
    liteWriteCache($liteFile,json_encode($lite));
    return $lite[1];
}
function liteCompile($path){
    global $liteBase;
	$paths = array($path);
	$sources = array(file_get_contents(realpath("$liteBase$path")));
	$decoratorPath = "/WEB-INF/decorators.xml";
	$decoratorXml = file_get_contents(realpath("$liteBase$decoratorPath"));
	if($decoratorXml){
	echo $decoratorXml;
		array_push($sources,$decoratorXml);
		array_push($paths,$decoratorPath);
	}
	while(true){
		$result = json_decode(liteHttpLoad($paths,$sources));
		if(array_key_exists("missed",$result)){
			$missed = $result["missed"];
			//print_r($missed);
			$retry = false;
			foreach($missed as $path){
				if(!in_array($path,$paths)){
					$content = file_get_contents(realpath("$liteBase$path"));
					
					array_push($sources,$content);
					array_push($paths,$path);
					$retry = true;
				}
			}
			if(!$retry){
				return array($paths,$result);
			}
		}else{
			return array($paths,$result);
		}
	}
}
function liteHttpLoad($paths,$sources){
    global $liteService;
	$postdata = http_build_query(
		array(
		    "source"=>$sources,
		    "path"=>$paths,
		    "compress"=>"true",
		    "base"=>"/"
	    )
	);
	//$postdata = preg_replace('/%5B(?:[0-9]+)%5D=/', '=', $postdata);
	$opts = array('http' =>
	    array(
	        'method'  => 'POST',
	        'header'  => 'Content-type: application/x-www-form-urlencoded',
	        'content' => $postdata
	    )
	);
	$context  = stream_context_create($opts);
	return file_get_contents($liteService, false, $context);
}
/**
 * 写入文件缓存
 * @param $file 缓存文件路径
 * @param $word 缓存文件内容
 */
function liteWriteCache ($file, $word) {
	$toFile = fopen($file, 'w+');
	$lockState = flock($toFile,LOCK_EX);
	fwrite($toFile, $word);
	fclose($toFile);
}
?>