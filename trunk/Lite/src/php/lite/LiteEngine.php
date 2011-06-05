<?php
//require_once('LiteFunction.php');
/**
 * Lite 使用实例代码：
 * <?php
 * require_once("../WEB-INF/classes/LiteEngine.php");
 * # 通过上下文数据方式传递模板参数：
 * #$engine = new LiteEngine();
 * #$context = array("int1"=>1,"text1"=>'1');
 * #$engine->render("/example/test.xhtml",$context);
 * 
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

	function render($path,$context){
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
		$fn = lite_load($path);
	    $fn($context);
	    LiteEngine::instance = $old;
	}
}
/**
 * 装载模板函数
 * @return function name
 */
function lite_load($path,$engine=null){
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
/**
 * javascript == 运算符模拟
 * @return bool
 */
function lite_op__eq($lop, $rop) {
    if($lop === null || $rop === null){
    	return $rop === $lop;
    }
    if(is_string($lop) || is_string($rop)){//0 == 'ttt'=>false
        return strcmp($lop,$rop) == 0;
    }else{
        return $lop == $rop;
    }
}

/**
 * javascript + 运算符模拟
 * @return string|number
 */
function lite_op__add($lop, $rop) {
	if($lop === null || $lop === false || $lop === true || is_numeric($lop)){
		if($rop === null || $rop === false){
			return $lop;
		}else if($rop === true || is_numeric($rop)){
			return $lop + $rop;
		}
	}
	return $lop . $rop;
}
/**
 *
 * 左操作数为确定数值类型(number/boolean/null)的 javascript + 运算符模拟
 * @return string|number
 */
function lite_op__add_nx($lop, $rop) {
	if($rop === null || $rop === false){
		return $lop;
	}else if($rop === true || is_numeric($rop)){
		return $lop + $rop;
	}else{
		return $lop . $rop;
	}
}

/**
 * 获取属性懂得函数(当有特殊属性的时候(length),需要调用这个函数)
 * @param array|object $base
 * @param string $key
 * @return mixed
 */
function lite_op__get($obj, $key) {
	//switch 有陷阱 $key == 0 =>0 =='length'
	if($key === 'length'){
		if(is_string($obj)){
			//TODO: add muti-charsets supported
			return mb_strlen($obj);
		}else if(is_array($obj)){
			return count($obj);
		}
	}
	return @$obj[$key];
}

function lite_op__invoke($lop, $rop) {
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

?>