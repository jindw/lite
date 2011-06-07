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
		$lite_action = @$_REQUEST['LITE_ACTION'] ;
		$lite_path = $_REQUEST['LITE_PATH'] ;
		if($lite_action == 'compile'){
			$this->compile($lite_path);
		}else if($lite_action == 'save'){
			$lite_code = $_REQUEST['LITE_CODE'] ;
			$lite_php = $_REQUEST['LITE_PHP'] ;
			$litefile = $this->litecode.'/'.strtr($lite_path,'/','^');
    		$phpfile = $litefile.'.php';
			file_put_contents($litefile,$lite_code);
			file_put_contents($phpfile,$lite_php);
			require($phpfile);
			echo '{"PHP_PATH":"'.$phpfile.'"}';
		}else{//prox
			$pathinfo = @$_SERVER['PATH_INFO']?$_SERVER['PATH_INFO']:$_SERVER['ORIG_PATH_INFO'];
			if(strncmp($pathinfo,'/scripts/',9) == 0){
				header("Context-Type:text/javascript;charset=utf-8");
				//echo '!!'.substr($pathinfo,10);
				readfile(dirname(__FILE__).substr($pathinfo,8));
			}else if($pathinfo){
				if(substr($pathinfo,-6) == '.xhtml' || substr($pathinfo,-4) == '.xml'){
					header("Context-Type:text/xml;charset=utf-8");
					readfile($this->root.$pathinfo);
				}else{
					echo "not support";
				}
			}else{
				echo "not support action [{$lite_action}]";
				print_r($_REQUEST);
			}
		}
	}
	function loadJavaScriptClass(){
		$scriptBase = $_REQUEST['LITE_SERVICE_URL'];
		$fns = func_get_args();
		
		$sns = array();
		foreach($fns as $fn){
			array_push($sns,preg_replace('/^.*[\.\:]/','',$fn));
		}
		$checkScript = 'window.'.join($sns,'&& window.');
		$importScript = '$import("'.join($fns,	'",true);$import("').'",true);';
		
		
echo "<script>
if(!($checkScript)){
	document.write(\"<script src='$scriptBase/scripts/boot.js'></\"+\"script>\");
}
</script><script>
if(!($checkScript)){
	$importScript
}
</script>";
	}
	function compile($path){
		if($this->getFileModified($path)){
			$scriptBase = $_REQUEST['LITE_SERVICE_URL'];
			$this->loadJavaScriptClass('org.xidea.lite.web:WebCompiler');
			echo "<script>
				var LITE_WC = window.LITE_WC || new WebCompiler('$scriptBase/');
				LITE_WC.compile('$path');
				LITE_WC.save();
			</script>";
			$litefile = $this->litecode.'/'.strtr($path,'/','^');
    		$phpfile = $litefile.'.php';
			if(file_exists($phpfile)){unlink($phpfile);}//.$phpfile.'<hr>';
			for($i=0;$i<10240 && !file_exists($phpfile);$i++){
				echo ' ';
				flush();
			}
			while(!file_exists($phpfile)){
				sleep(1);
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