<?php
/**
 * Lite 使用实例代码：
 * <?php
 * require_once("../WEB-INF/classes/TemplateEngine.php");
 * $engine = new TemplateEngine();
 *
 * # 通过上下文数据方式传递模板参数：
 * $engine->render("/example/test.xhtml",array("int1"=>1,"text1"=>'1'));
 * 
 * ## 直接通过全局变量传递模板参数：
 * #$int1 = 1;
 * #$text1 = '1';
 * #$engine->render("/example/test.xhtml");
 *
 * @see http://lite.googlecode.com 
 * ?>
 */
require_once('Template.php');

class TemplateEngine{
	var $liteBase;
	/**
	 * 上线后建议关闭
	 */
	var $autocompile = true;
	//var $liteService='http://localhost:8080';
	//var $liteService='http://litecompiler.appspot.com';
	var $liteService='java -jar $this->liteBase';
	var $liteCached;
	function TemplateEngine($liteBase=null,$liteService=null){
		if($liteBase == null){
			//自动探测虚拟目录
			$liteBase = $_SERVER['DOCUMENT_ROOT'];
			$dir = realpath($_SERVER['SCRIPT_FILENAME']);
			while($dir!=($dir2 = dirname($dir))){
				$dir=$dir2;
				if(file_exists($dir.'/WEB-INF')){
					$liteBase = $dir;
					break;
				}
			}
		}
		$liteBase = realpath($liteBase);
		if(!file_exists($liteBase)){
			echo 'liteBase not found:'.$liteBase;
			exit();
		}
		if($liteService == null){
			$liteService = $this->liteService;
		}else{
			$this->liteService = $liteService;
		}
		$this->liteBase = $liteBase;
		$this->liteCached = $liteBase.'/WEB-INF/litecached/';
		if(!file_exists($this->liteCached)){
			if(!file_exists($liteBase.'/WEB-INF')){
				mkdir($liteBase.'/WEB-INF');
			}
			mkdir($this->liteCached);
		}
	}
	function render($path,$context=null){
		$liteCode = &$this->load($path);
		$template = new Template($liteCode);
		if($context == null){
			$context = $GLOBALS;
		}
		$template->render($context);
	}
	function &load($path){
	    $liteFile = $this->liteCached.urlencode($path);
	    if(file_exists($liteFile)){
	    	$lite = json_decode(file_get_contents($liteFile));
	    	if($this->autocompile){
		    	$paths = $lite[0];
		    	$liteTime = filemtime($liteFile);
		    	$fileTime = $liteTime;
		    	$i=count($paths);
				while($i--){
					$fileTime = max($fileTime,filemtime($this->liteBase.$paths[$i]));
				}
				if($fileTime<=$liteTime){
					return $lite[1];
				}
			}else{
				return $lite[1];
			}
	    }
	    $lite = $this->compile($liteFile,$path);
	    return $lite[1];
	}
	function compile($liteFile,$path){
		if(substr_compare('/',$path,0,1)){
			echo "模板地址需要 '/' 开头;\n";
		}else{
			$paths = array($path);
			$sources = array(file_get_contents(realpath($this->liteBase.$path)));
			$decoratorPath = '/WEB-INF/decorators.xml';
			$decoratorXml = file_get_contents(realpath($this->liteBase.$decoratorPath));
			if($decoratorXml){
				array_push($sources,$decoratorXml);
				array_push($paths,$decoratorPath);
			}
			//最多尝试6次
			$test = 6;
			while($test--){
				$code = $this->httpLoad($paths,$sources);
				if(!$code){
					sleep(10);
					continue;
				}
				$result = json_decode($code);
				if(!$result){
					sleep(10);
					continue;
				}
				if(is_array($result)){
				    $this->writeCache($liteFile,$paths,$code);
					return array($paths,$result,$code);
				}else{
		        	$missed = $result->missed;
					$retry = false;
					foreach($missed as $path){
						if(!in_array($path,$paths)){
							$content = file_get_contents(realpath($this->liteBase.$path));
							
							array_push($sources,$content);
							array_push($paths,$path);
							$retry = true;
						}
					}
					if(!$retry){
						return array($paths,array($code));
					}
					sleep(5);
				}
			}
		}
		echo "编译失败...";
		exit();
	}
	function httpLoad($paths,&$sources){
		$postdata = http_build_query(
			array(
			    'source'=>$sources,
			    'path'=>$paths,
			    'compress'=>'true',
			    'base'=>'/'
		    )
		);
		
		//echo $postdata;
		//$postdata = preg_replace('/%5B(?:[0-9]+)%5D=/', '=', $postdata);
		$opts = array('http' =>
		    array(
		        'method'  => 'POST',
		        'header'  => 'Content-type: application/x-www-form-urlencoded',
		        'content' => $postdata
		    )
		);
		$context  = stream_context_create($opts);
		return file_get_contents($this->liteService, false, $context);
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