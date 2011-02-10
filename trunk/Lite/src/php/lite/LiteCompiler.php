<?php
/**
 * Lite 使用实例代码：
 * <?php
 * require_once("../WEB-INF/classes/LiteEngine.php");
 * # 通过上下文数据方式传递模板参数：
 * LiteEngine::render("/example/test.xhtml",array("int1"=>1,"text1"=>'1'));
 * 
 * ## 直接通过全局变量传递模板参数：
 * #$int1 = 1;
 * #$text1 = '1';
 * #LiteEngine::render("/example/test.xhtml");
 *
 * @see http://lite.googlecode.com 
 * ?>
 */

class LiteCompiler{
	public LiteBrowserCompile($engine){
		$this->engine = $engine;
		$this->root = $engine->root;
		$this->code = $engine->code;
	}
	
	function resources($path){
		$litefile = $this->code.$path;
		if(file_exists($litefile)){
			$lite = json_decode(file_get_contents($litefile),true);
			if(is_array($lite[0])){
				return $lite[0];
			}
		}
	}
	function lastModified($root,$paths){
    	$i=count($paths);
    	$time = 0;
		while($i--){
			$time = max($time,filemtime($root.$paths[$i]));
		}
		return $time;
	}
	function compile($path){
	    $litefile = $this->code.$path;
    	$phpfile = $litefile.'.php';
    	$phptime = filemtime($phpfile);
    	$litetime = filemtime($litefile);
    	$paths = resources($path);
    	$sourcetime = lastModified($this->root,$paths);
    	if($phptime>$sourcetime){
    		require_once('php_compile');
    	}
	}
	/**
	 * 写入文件缓存
	 * @param $file 缓存文件路径
	 * @param $word 缓存文件内容
	 */
	function writeCache($file, &$paths, &$liteCode) {
		$toFile = fopen($file, 'w+');
		$lockState = flock($toFile,LOCK_EX);
		$word = '['.json_encode($paths).','.$liteCode.']';
		fwrite($toFile, $word);
		fclose($toFile);
	}
}

?>