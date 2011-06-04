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

class LiteService{
	public LiteService($root,$litecode){
		$this->root = $root;
		$this->litecode = $litecode;
	}
	public function execute(){
		
	}
	function compile($path){
		if(getFileModified($path)){
		}
	    
	}
	public function getFileModified($path){
		$litefile = $this->litecode.'/'.strtr($path,'/','^');
    	$phpfile = $litefile.'.php';
    	$phptime = filemtime($phpfile);
    	
		if(file_exists($litefile)){
			$lite = json_decode(file_get_contents($litefile),true);
			$resources = @$lite[0];
			if(is_array($resources)){
				foreach($resources as $spath){
					$file = $this->root.$spath;
					if(file_exists($file)){
						if(filemtime($file) > $phptime){
							return $file;
						}
					}else{
						return $file;
					}
				}
				return false;
			}
		}
		return $litefile;
	}
}

?>