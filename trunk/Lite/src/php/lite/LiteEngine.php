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
	public $litecode;	
	/**
	 * 上线后建议关闭
	 * 可以制定true,false,客户端 ip 匹配正则表达式
	 * A：10.0.0.0-10.255.255.255
	 * B：172.16.0.0-172.31.255.255
	 * C：192.168.0.0-192.168.255.255 
	 */
	public $debug = '127\.0\.0\.1|10\..+|172\.(?:1[6789]|2.|30|31)\..+|192\.168\..+';
	/**
	 * 当前执行的模板示例
	 */
	private static $instance;
	
	/**
	 * 上线后建议置空
	 * 设置是编译器实现，有则自动编译，必须在$debug 为true时，才能生效
	 */
	public $compiler = "LiteService";
	function LiteEngine($root=null,$litecode=null){
		if(!$root){
			$pos = strrpos(__FILE__,'WEB-INF');
			$root = $pos ? substr(__FILE__,0,$pos) : $_SERVER['DOCUMENT_ROOT'];
		}
		$this->root = strtr(realpath($root),'\','/').'/';
		$this->litecode = $litecode?$litecode:$this->root.'WEB-INF/litecode/';
	}

	function render($path,$context=array()){
		$old = LiteEngine::instance;
		if($old == null){
			if(is_string($this->debug)){
				$this->debug = !!preg_match("/^(?:$this->debug)$/",$_SERVER["REMOTE_ADDR"]);
			}
			if(!$this->debug){
				$this->compiler = null;
			}
		}
	    LiteEngine::instance = $this;
		$fn = $this->load($path);
	    $fn($context);
	    LiteEngine::instance = $old;
		
	}
	static function load($path){
		$engine = LiteEngine::instance;
		$fn = 'lite_template'.str_replace(array('.','/','-','!','%'),'_',$path);
	    if($engine->compiler){
	    	$compiler = $engine->compiler;
	    	$compiler = new $compiler($engine->root,$engine->litecode);
	    	$compiler->compile($path);
	    }
	    require_once($engine->litecode.strtr($path,'/','^').'.php');
	    return $fn;
	}
	static function op($type,$arg1,$arg2=null){
		switch($type){
		case OP_ADD://+
			//严格的规则是:任意一方为: "number","boolean","NULL",date 时,采用number 加法
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
			if($arg2 == 'length' && is_array($arg2) && !array_key_exists('length',$arg1)){
				return count($arg1);
			}
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