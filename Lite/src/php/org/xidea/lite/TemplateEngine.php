<?php
require_once("Template.php");

class TemplateEngine{
	var $liteBase;
	var $liteService; 
	function TemplateEngine($liteBase=NULL,$liteService="http://litecompiler.appspot.com"){
		if($liteBase == NULL){
			$liteBase = $_SERVER["DOCUMENT_ROOT"];
		}
		$this->liteBase = &$liteBase;
		$this->liteService = $liteService;
		if(!file_exists($liteBase)){
			echo "liteBase not found:$this->liteBase";
			exit();
		}
	}
	function render($path,$context=NULL){
		$liteCode = &$this->load($path);
		$template = new Template($liteCode);
		if($context == NULL){
			$context = $GLOBALS;
		}
		$template->render($context);
	}
	function &load($path){
	    $compileDir = "$this->liteBase/WEB-INF/litecached/";
		if(!file_exists($compileDir)){
			mkdir($compileDir,0700,true);
		}
	    $liteFile = $compileDir.rawurlencode($path);
	    if(file_exists($liteFile)){
	    	$lite = &json_decode(file_get_contents($liteFile));
	    	$paths = $lite[0];
	    	$liteTime = filemtime($liteFile);
	    	$fileTime = $liteTime;
	    	$i=count($paths);
			while($i--){
				$fileTime = max($fileTime,filemtime("$this->liteBase$paths[$i]"));
			}
			if($fileTime<=$liteTime){
				return $lite[1];
			}
	    }
	    $lite = &$this->compile($path);
	    $this->writeCache($liteFile,json_encode($lite));
	    return $lite[1];
	}
	function compile($path){
		$paths = array($path);
		$sources = array(file_get_contents(realpath("$this->liteBase$path")));
		$decoratorPath = "/WEB-INF/decorators.xml";
		$decoratorXml = file_get_contents(realpath("$this->liteBase$decoratorPath"));
		if($decoratorXml){
			array_push($sources,$decoratorXml);
			array_push($paths,$decoratorPath);
		}
		while(true){
			$code = $this->httpLoad($paths,$sources);
			if(!$code){
				continue;
			}
			$result = json_decode($code);
			if(!$result){
				//echo "<hr>".$code."<hr>";
				continue;
			}
			if(!is_array($result)){
				//echo "<hr>".$code."<hr>";
				$missed = $result->missed;
				$retry = false;
				foreach($missed as $path){
					if(!in_array($path,$paths)){
						$content = file_get_contents(realpath("$this->liteBase$path"));
						
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
	function httpLoad($paths,&$sources){
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
		return file_get_contents($this->liteService, false, $context,0,1024*1024);
	}
	/**
	 * 写入文件缓存
	 * @param $file 缓存文件路径
	 * @param $word 缓存文件内容
	 */
	function writeCache ($file, &$word) {
		$toFile = fopen($file, 'w+');
		$lockState = flock($toFile,LOCK_EX);
		fwrite($toFile, $word);
		fclose($toFile);
	}
}

?>