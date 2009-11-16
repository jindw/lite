<?php
/**
 * firekylin PHP engine
 * $Id: TemplateEngine.php 26222 2009-11-03 07:58:19Z  $
 */
if(!extension_loaded('mbstring')){
	require_once("mb.php");
}
require_once("fn.php");



/**
 * Lite2php模板引擎实现
 */
class TemplateEngine{
	/**
	 * 上线后建议关闭
	 * 如果在调试服务器上开发，该配置会被自动打开！！
	 */
	var $autocompile = true;
	/**
	 * 控制自动生成中间代码的编码类型
	 */
	var $encoding = "GBK";
	/**
	 * 如果要指定具体位置，可如下示例（注意，第一个字符一定不能是‘“’，特殊字符也只能中间括起来）。
	 * C:/\"Program Files\"/Java/jre1.5.0_20/bin/java
	 */
	var $cmd = "java";
	var $litebase;
	var $litecached;
	function TemplateEngine($litebase=null){
		$this->autocompile = $this->autocompile || function_exists('lite_php_compile');
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
		$this->litebase = $litebase;
		$this->litecached = $litebase.'/WEB-INF/litecached/';
		if(!file_exists($this->litecached)){
			if(!file_exists($litebase.'/WEB-INF')){
				mkdir($litebase.'/WEB-INF');
			}
			mkdir($this->litecached);
		}
		if(!file_exists($this->litecached)){
			echo '<h2>litecached 目录创建失败，请手动创建并赋予333权限:</h2>'.$this->litecached;
			exit();
		}
	}
	function compile($path){
		//TODO:是否更新检查
		$litecached = $this->litecached;
		
		$litefile = $this->litecached.str_replace('/','^',$path);
		$phpfile = $litefile.'.php';
		
	    if(file_exists($litefile) && file_exists($phpfile)){
	    	$lite = json_decode(file_get_contents($litefile),true);
	    	$paths = $lite[0];
	    	
	    	$liteTime = filemtime($litefile);
	    	$phpTime = filemtime($phpfile);
	    	if($phpTime >= $liteTime){
				$fileTime = $liteTime;
				$i=count($paths);
				while($i--){
					$file = $this->litebase.$paths[$i];
					if(!file_exists($file)){
						//需要编译
						$fileTime = $liteTime+1;
					}else{
						$fileTime = max($fileTime,filemtime($file));
					}
				}
				if(array_key_exists("LITE_CLEAN",$_COOKIE)){
					$ct = $_COOKIE["LITE_CLEAN"];
					if($ct>(time()-10)*1000){
						setcookie("LITE_CLEAN", 0, 0);
						$fileTime =  $liteTime+1;
					}
				}
				
				if($fileTime<=$liteTime){
					return ;
				}
	    	}
	    }
	    
		if(function_exists('lite_php_compile')){
			//Quercus 編譯
			lite_php_compile($path,$litecached);
		}else{
			$root = realpath("$litecached/../../");
			$cp = realpath($root.'/WEB-INF/classes');
			$lib = realpath($root.'/WEB-INF/lib');
			if($lib){
		        $lib_file = dir($lib); 
		        while (false !== ($file = $lib_file->read())){
				    if(preg_match('/.*\.(?:jar|zip)$/i',$file)){
				    	$cp.=PATH_SEPARATOR.$lib.'/'.$file;
				    }
				}
			}
			$args = ' -Dfile.encoding='.$this->encoding.
					' -cp '.escapeshellarg($cp).' com.baidu.firekylin.FireKylinCompiler'.
					' -resultType php -path '.escapeshellarg($path).
					' -root '.escapeshellarg($root).
					' -encoding '.$this->encoding
					;
			$cmd = $this->cmd.$args;
			$result = $this->exec($cmd);
			
			if($result == ''){
				$result = '编译失败：';
				if(!preg_match('/[\/\\\\]lib\/FireKylin\b[^\/\\\\]*\.(?:jar|zip)/i',$cp)){
					//远程编译尚未实现。。。。
					$result .= "请将火麒麟类库放置在如下路径中： WEB-INF/lib/FireKylin.jar";
				}else{
					//TODO:具体原因？应该给出当前具体的JDK版本....
					$result .= "未找到java程序或者版本低于1.5,您可以直接运行上述命令获得更详细的错误信息。";
					$result .= $this->exec("$this->cmd -version");
				}
			}else if(0 === strpos($result,"java.lang.UnsupportedClassVersionError")){
				$result = '<p style="color:red">Java版本太老，请使用JRE1.5+</p><p>您当前java版本是：'.$this->exec($this->cmd.' -version').'</p>'.$result ;
			}
			echo('<pre style="border:solid 1px yellow;background:#ddd">');
			echo "<h3>Command:</h3><br>";
			echo($cmd);
			echo "<hr><br><h3>Log/Error:</h3><br>";
			echo $result;
			echo('<hr></pre>');
		}
	}
	public function exec($cmd){
		$descriptorspec = array(array("pipe", "r"), array("pipe", "w"), array("pipe", "w") );
		$process = proc_open($cmd, $descriptorspec, $pipes);
		if (is_resource($process)) {
		    $content= stream_get_contents($pipes[2]).stream_get_contents($pipes[1]);
		    proc_close($process);
		}
		return $content;
	}
	public function render($path,$context){
		//$fn = 'lite_template'.preg_replace("/[^\w]/",'_',$path);
		$fn = 'lite_template'.str_replace(array('.','/','-'),'_',$path);
		$file = $this->litecached.str_replace('/','^',$path).".php";
		if($this->autocompile && !function_exists($fn)){
			$this->compile($path);
		}
		require_once($file);
		$encoding = mb_internal_encoding();
		mb_internal_encoding($this->encoding);
		$error_level =  error_reporting();
		//error_reporting($error_level & ~E_NOTICE);
		call_user_func($fn,$context);
		error_reporting($error_level);
		mb_internal_encoding($encoding);
		
	}
}?>