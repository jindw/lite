<?php
require_once("Template.php");
$base = $_SERVER["DOCUMENT_ROOT"];
$exportService = "http://litecompiler.appspot.com"; 
function compileLite($paths){
    global $base;
    global $exportService;
	$sources = array();
	$count=count($paths);
	for($i=0;$i<$count;$i++){
		$sources[$i] = file_get_contents(realpath("$base$paths[$i]"));
	}
	$postdata = http_build_query(
		array(
		    "source"=>$sources,
		    "path"=>$paths,
		    "base"=>"/" //file:///".$base
	    )
	);
	$postdata = preg_replace('/%5B(?:[0-9]+)%5D=/', '=', $postdata);
	$opts = array('http' =>
	    array(
	        'method'  => 'POST',
	        'header'  => 'Content-type: application/x-www-form-urlencoded',
	        'content' => $postdata
	    )
	);
	$context  = stream_context_create($opts);
	return file_get_contents($exportService, false, $context);
}
/**
 * 写入文件缓存
 * @param $file 缓存文件路径
 * @param $word 缓存文件内容
 */
function writeCache ($file, $word) {
	$toFile = fopen($file, 'w+');
	$lockState = flock($toFile,LOCK_EX);
	fwrite($toFile, $word);
	fclose($toFile);
}
function getLite($paths){
    global $base;
    $compileDir = "$base/WEB-INF/classes/phplite/";
	if(!file_exists($compileDir)){
		mkdir($compileDir);
	}
    $liteFile = $compileDir.rawurlencode(join(",",$paths));
    if(file_exists($liteFile)){
    	$liteTime = filemtime($liteFile);
    	$fileTime = $liteTime;
    	$i=count($paths);
		while($i--){
			$fileTime = max($fileTime,filemtime("$base$paths[$i]"));
		}
		if($fileTime<=$liteTime){
			return file_get_contents($liteFile);
		}
    }
    $lite = compileLite($paths);
    writeCache($liteFile,$lite);
    return $lite;
}
?>