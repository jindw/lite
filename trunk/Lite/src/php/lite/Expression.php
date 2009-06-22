<?php
# 值标示定义
define('LITE_VALUE_CONSTANTS', 0);#c;
define('LITE_VALUE_VAR', -1);#n;
define('LITE_VALUE_LAZY', -2);
define('LITE_VALUE_NEW_LIST', -3);#[;
define('LITE_VALUE_NEW_MAP', -4);#{;
	
# 操作符标示定义
#9
define('LITE_OP_GET_PROP', 17);#0 | 16 | 1;
define('LITE_OP_STATIC_GET_PROP', 48);#32 | 16 | 0;
define('LITE_OP_INVOKE_METHOD', 81);#64 | 16 | 1;
#8
define('LITE_OP_NOT', 14);#0 | 14 | 0;
define('LITE_OP_POS', 46);#32 | 14 | 0;
define('LITE_OP_NEG', 78);#64 | 14 | 0;
#7
define('LITE_OP_MUL', 13);#0 | 12 | 1;
define('LITE_OP_DIV', 45);#32 | 12 | 1;
define('LITE_OP_MOD', 77);#64 | 12 | 1;
#6
define('LITE_OP_ADD', 11);#0 | 10 | 1;
#5
define('LITE_OP_SUB', 41);#32 | 8 | 1;
#4
define('LITE_OP_LT', 7);#0 | 6 | 1;
define('LITE_OP_GT', 39);#32 | 6 | 1;
define('LITE_OP_LTEQ', 71);#64 | 6 | 1;
define('LITE_OP_GTEQ', 103);#96 | 6 | 1;
define('LITE_OP_EQ', 135);#128 | 6 | 1;
define('LITE_OP_NOTEQ', 167);#160 | 6 | 1;
#3
define('LITE_OP_AND', 5);#0 | 4 | 1;
define('LITE_OP_OR', 37);#32 | 4 | 1;
#2
define('LITE_OP_QUESTION', 3);#0 | 2 | 1;
define('LITE_OP_QUESTION_SELECT', 35);#32 | 2 | 1;
#1
define('LITE_OP_PARAM_JOIN', 1);#0 | 0 | 1;
define('LITE_OP_MAP_PUSH', 33);#32 | 0 | 1;

$LITE_QUESTION_NEXT = new stdClass();
class Expression{
	var $tokens;
	function stringify(&$o){
		return json_encode($o);
	}
	function parse(&$t){
		return json_decode($t);
	}
	function Expression(&$tokens){
		$this->tokens = &$tokens;
	}
	function &evaluate(&$context) {
		return lite_evaluate($context,$this->tokens);
	}
}

function lite_evaluate(&$context,$tokens) {
	if(count($tokens)==1){
	    $stack = lite_value($context,$tokens[0]);
	}else{
		$stack = array();
		_lite_evaluate($stack, $context, $tokens);
		$stack=$stack[0];
		if ($stack instanceof _LitePropertyValue){
			$stack = &$stack->get();
		}
	}
	return $stack;
}
function _lite_evaluate(&$stack, &$context, &$tokens) {
	foreach($tokens as &$item) {
		if(is_array($item)) {
			$type = &$item[0];
			if($type > 0) {
				if($type & 1) {
					$result = lite_compute($item, array_pop($stack), array_pop($stack));
				}else{
					$arg2 = null;
					$result = lite_compute($item, $arg2, array_pop($stack));
				}
				
				if($result instanceof _LiteLazyToken) {
					_lite_evaluate($stack, $context, $result->children);
				} else {
					$stack[] = $result;
				}
			} else {
				$stack[] = lite_value($context, $item);
			}
		}
	}
}
function lite_value(&$context, &$item) {
	switch($item[0]){
		case LITE_VALUE_VAR:
			$value = $item[1];
			if (array_key_exists($value, $context)) {
				return $context[$value];
			} else {
				switch ($value) {
				case "JSON":
					return "Expression";
				case "encodeURIComponent": 
					return "urlencode";
				case "decodeURIComponent": 
					return "urldecode";
				}
			}
			return null;//您如果需要仅用默认函数，那么 return null吧
		case LITE_VALUE_CONSTANTS:
			return $item[1];
		case LITE_VALUE_NEW_LIST:
			return array();
		case LITE_VALUE_NEW_MAP:
			return new stdClass();
		case LITE_VALUE_LAZY:
			return new _LiteLazyToken($item[1]);
	}
}

/**
 * 丑陋的PHP引用：（，引用传递，传入前的变量不能随便修改，都不能随便修改的：（
 * @param $op
 * @param $arg1
 * @param $arg2 
 */
function lite_compute(&$op, &$arg2, &$arg1) {
    global $LITE_QUESTION_NEXT;
	$type = $op[0];
	if ($type == LITE_OP_INVOKE_METHOD) {
		if($arg1 instanceof _LitePropertyValue) {
			return $arg1->call($arg2);
		} else {
			return call_user_func_array($arg1, $arg2);
		}
	}
	//echo get_class ( $arg1);
	if ($arg1 instanceof _LitePropertyValue) {
		$arg1 = &$arg1->get();
	}
	if ($arg2 instanceof _LitePropertyValue) {
		$arg2 = &$arg2->get();
	}
	switch($type) {
		case LITE_OP_STATIC_GET_PROP:
			return new _LitePropertyValue($arg1, $op[1]);
		case LITE_OP_GET_PROP:
			return new _LitePropertyValue($arg1, $arg2);
		case LITE_OP_PARAM_JOIN:
			$arg1[]=$arg2;return $arg1;
		case LITE_OP_MAP_PUSH:
			$arg1->$op[1] = $arg2;
			return $arg1;
		case LITE_OP_NOT:
			return !$arg1;
		case LITE_OP_POS:
			return +$arg1;
		case LITE_OP_NEG:
			return -$arg1;
		/* +-*%/ */
		case LITE_OP_ADD:
			if(is_string($arg1)||is_string($arg2)){
				return $arg1.$arg2;
			}else{
				return $arg1+$arg2;
			}
		case LITE_OP_SUB:
			return $arg1-$arg2;
		case LITE_OP_MUL:
			return $arg1*$arg2;
		case LITE_OP_DIV:
			if($arg2==0){
				return 'NaN';
			}
			return $arg1/$arg2;
		case LITE_OP_MOD:
			return $arg1%$arg2;
		/* boolean */
		case LITE_OP_GT:
			return $arg1>$arg2;
		case LITE_OP_GTEQ:
			return $arg1>=$arg2;
		case LITE_OP_NOTEQ:
			return $arg1!=$arg2;
		case LITE_OP_EQ:
			return $arg1==$arg2;
		case LITE_OP_LT:
			return $arg1<$arg2;
		case LITE_OP_LTEQ:
			return $arg1<=$arg2;

		/* and or */
		case LITE_OP_AND:
			return $arg2 && $arg1;
		case LITE_OP_OR:
			return $arg1 || $arg2;
		case LITE_OP_QUESTION:
			return $arg1 ? $arg2 : $LITE_QUESTION_NEXT;
		case LITE_OP_QUESTION_SELECT:
			return $arg1 == $LITE_QUESTION_NEXT ? $arg2:$arg1;
		case LITE_OP_MAP_PUSH:
			$arg1[$op[1]] = $arg2;return $arg1;
		default:break;
	}
}
		


class _LiteLazyToken {
	var $children;
	function _LiteLazyToken(&$children) {
		$this->children = &$children;
	}
}

class _LitePropertyValue {
	var $base;
	var $name;
	function _LitePropertyValue(&$base, &$name) {
		$this->base = &$base;
		$this->name = &$name;
	}
	function get(){
		$base = &$this->base;
		$name = &$this->name;
		if(is_array($base)) {
			if(array_key_exists($name, $base)){
				return $base[$name];
			}
		}else{
			return $base->$name;
		}
	}
	function call(&$arg){
		$base = &$this->base;
		$name = &$this->name;
		if (is_array($base) && array_key_exists($name, $base)) {
			return call_user_func_array($base[$name], $arg);
		} else {
			return call_user_func_array(array($base,$name),$arg);
		}
	}
}
?>