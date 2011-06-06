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
 * 
 */

class LiteService{
	function LiteService($root,$litecode){
		$this->root = $root;
		$this->litecode = $litecode;
	}
	public function execute(){
		$lite_action = @$_REQUEST['LITE-ACTON'] ;
		if($lite_action == 'compile'){
			$lite_path = $_REQUEST['LITE-PATH'] ;
			$this->compile($lite_path);
		}else{
			$pathinfo = @$_SERVER['PATH_INFO'];
			echo $pathinfo;
			if(strncmp($pathinfo,'/scripts/',9) == 0){
				readfile('.'.$pathinfo);
			}
		}
	}
	function compile($path){
		if($this->getFileModified($path)){
			$start_script = $_REQUEST['LITE_SERVICE_URL'].'/scripts/compile.js';
			echo "<script src='$start_script'></script>";
			ob_flush();flush();
			
			$litefile = $this->litecode.'/'.strtr($path,'/','^');
    		$phpfile = $litefile.'.php';
			if(file_exists($phpfile)){unlink($phpfile);}
			while(file_exists($phpfile)){
				sleep(100);
			}
		}
	}
	public function getFileModified($path){
		$litefile = $this->litecode.'/'.strtr($path,'/','^');
    	$phpfile = $litefile.'.php';
		if(!file_exists($litefile)){
			return $litefile;
		}
		if(!file_exists($phpfile)){
			return $phpfile;
		}
		$phptime = filemtime($phpfile);
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
		return $litefile;
		
	}
}

?>