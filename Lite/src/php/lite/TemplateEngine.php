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
	var $litebase;
	/**
	 * 上线后建议关闭
	 */
	var $autocompile = true;
	//var $liteservice='http://localhost:8080';
	var $liteservice='http://litecompiler.appspot.com';
	var $litecached;
	function TemplateEngine($litebase=null,$liteservice=null){
		if($litebase == null){
			//自动探测虚拟目录
			$litebase = $_SERVER['DOCUMENT_ROOT'];
			$dir = realpath($_SERVER['SCRIPT_FILENAME']);
			while($dir!=($dir2 = dirname($dir))){
				$dir=$dir2;
				if(file_exists($dir.'/WEB-INF')){
					$litebase = $dir;
					break;
				}
			}
		}
		$litebase = realpath($litebase);
		if(!file_exists($litebase)){
			echo 'litebase not found:'.$litebase;
			exit();
		}
		if($liteservice == null){
			$liteservice = $this->liteservice;
		}else{
			$this->liteservice = $liteservice;
		}
		$this->litebase = $litebase;
		$this->litecached = $litebase.'/WEB-INF/litecached/';
		if(!file_exists($this->litecached)){
			if(!file_exists($litebase.'/WEB-INF')){
				mkdir($litebase.'/WEB-INF');
			}
			mkdir($this->litecached);
		}
	}
	function render($path,$context=null){
		$liteCode = $this->load($path);
		$template = new Template($liteCode);
		if($context == null){
			$context = $GLOBALS;
		}
		$template->render($context);
	}
	function load($path){
	    $litefile = $this->litecached.urlencode($path);
	    if(file_exists($litefile)){
	    	$lite = json_decode(file_get_contents($litefile),true);
	    	if($this->autocompile){
		    	if($lite!=null){
			    	$paths = $lite[0];
			    	$liteTime = filemtime($litefile);
			    	$fileTime = $liteTime;
			    	$i=count($paths);
					while($i--){
						$fileTime = max($fileTime,filemtime($this->litebase.$paths[$i]));
					}
					if($fileTime<=$liteTime){
						return $lite[1];
					}
				}
			}else{
				return $lite[1];
			}
	    }
	    if(substr_compare('/',$path,0,1)){
			echo "模板地址需要 '/' 开头;\n";
			exit();
		}else{
			$lite = $this->javaCompile($path,$litefile);
			if($lite !=null){
				return $lite[1];
			}
		}
		
	    $lite = $this->httpCompile($path,$litefile);
	    return $lite[1];
	}
	function javaCompile($path,$litefile){
		try{
			$litebase = $this->litebase;
			$cp = realpath("$litebase/WEB-INF/lib/Template.jar");
			if(!$cp){
				$cp = realpath("$litebase/../build/dest/Template.jar");
			}
			//.PATH_SEPARATOR;
			$main = "org.xidea.lite.tools.LiteCompiler";
			$args = array("-cp",$cp,$main,"-path",$path,"-webRoot",$litebase);
			//echo json_encode($args);
			$cmd = "java";
			foreach($args as $arg){
				$cmd="$cmd $arg";
			}
			$time = time();
			exec($cmd);
			sleep(1);
			if(file_exists($litefile) && $time < filemtime($litefile)){
				$lite = json_decode(file_get_contents($litefile));
				return $lite;
			}
		}catch(Exception $e){
			echo $e;
		}
		return null;
	}
	function httpCompile($path,$litefile){
		$paths = array($path);
		$sources = array(file_get_contents(realpath($this->litebase.$path)));
		$decoratorPath = '/WEB-INF/decorators.xml';
		$decoratorXml = file_get_contents(realpath($this->litebase.$decoratorPath));
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
			    $this->writeCache($litefile,$paths,$code);
				return array($paths,$result);
			}else{
	        	$missed = $result->missed;
				$retry = false;
				foreach($missed as $path){
					if(!in_array($path,$paths)){
						$content = file_get_contents(realpath($this->litebase.$path));
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
		echo "编译失败...";
		exit();
	}
	function httpLoad($paths,$sources){
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
		return file_get_contents($this->liteservice, false, $context);
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