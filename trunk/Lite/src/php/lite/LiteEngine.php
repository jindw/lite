<?php
require_once('LiteFunction.php');
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
 */

class LiteEngine{
	/**
	 * 模板根目录（建议为网站根目录）
	 */
	public $root;
	/**
	 * 中间代码根目录（默认为：/[网站根目录]/WEB-INF/litecode）
	 */
	public $code;	
	/**
	 * 上线后建议关闭
	 * 可以制定true,false,客户端 ip 匹配正则表达式
	 * A：10.0.0.0-10.255.255.255
	 * B：172.16.0.0-172.31.255.255
	 * C：192.168.0.0-192.168.255.255 
	 */
	public $debug = '127\.0\.0\.1|10\..+|172\.(?:1[6789]|2.|30|31)\..+|192\.168\..+';
	
	/**
	 * 上线后建议置空
	 * 设置是编译器实现，有则自动编译，必须在$debug 为true时，才能生效
	 */
	public $compiler = "LiteBrowserCompile";
	function LiteEngine($root=null,$litecode=null){
		$this->root = realpath($root?$root:$this->_root());
		$this->code = $code?$code:$this->root.'/WEB-INF/litecode/';
		if(is_string($this->debug)){
			$this->debug = !!preg_match("/^(?:$this->debug)$/",$_SERVER["REMOTE_ADDR"]);
		}
		if(!$this->debug){
			$this->compiler = null;
		}
	}
	function _root(){
		//自动探测虚拟目录
		$dir = realpath($_SERVER['SCRIPT_FILENAME']);
		while($dir!=($dir2 = dirname($dir))){
			$dir=$dir2;
			if(file_exists($dir.'/WEB-INF')){
				return $dir;
			}
		}
		return $_SERVER['DOCUMENT_ROOT'];
	}

	function render($path,$context=array()){
		$fn = $this->load($path);
		$fn($this,$context);
		
	}
	function load($path){
	    if($this->compiler){
	    	$compiler = $this->compiler;
	    	require_once($compiler.'.php');
	    	$compiler = new $compiler(this);
	    	$compiler->compile($path);
	    }
	    return 'lite_template'.str_replace(array('.','/','-','!','%'),'_',$path);
	}
	function op($type,$arg1,$arg2=null){
		switch($type){
		case OP_ADD://+
			if(is_string($arg1)||is_string($arg2)){
				return $arg1.$arg2;
			}else{
				return $arg1+$arg2;
			}
		case OP_NOT://!
			return !$arg1;
		case OP_EQ://==
			return $arg1==$arg2;
		case OP_NOTEQ://!=
			return $arg1!=$arg2;
		case OP_GET://.
			return $arg1[$arg2];
		case OP_INVOKE://member only
			if(is_array($arg1)){//member
				$thiz = $arg1[0];
				$key = $arg1[1];
				$type = gettype($thiz);
				if(function_exist("$type_$key")){
					array_unshift($arg2,$thiz);
					return call_user_func_array(array($this,"$type_$key"),$arg2);
				}else if($type == 'object'){
					return call_user_func_array(arg1,$arg2);
				}
			}else{//不会执行
				return call_user_func_array($arg1,$arg2);
				//lite__name
			}
		}
	}
	
}

?>