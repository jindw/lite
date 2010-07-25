<?php

define('LITE_BIT_ARGS',192);
define('LITE_VALUE_CONSTANTS',-1);
define('LITE_VALUE_VAR',-2);
define('LITE_VALUE_NEW_LIST',-3);
define('LITE_VALUE_NEW_MAP',-4);
define('LITE_OP_GET_PROP',96);
define('LITE_OP_GET_STATIC_PROP',33);
define('LITE_OP_INVOKE_METHOD',98);
define('LITE_OP_INVOKE_METHOD_WITH_STATIC_PARAM',35);
define('LITE_OP_INVOKE_METHOD_WITH_ONE_PARAM',352);
define('LITE_OP_NOT',28);
define('LITE_OP_BIT_NOT',29);
define('LITE_OP_POS',30);
define('LITE_OP_NEG',31);
define('LITE_OP_MUL',88);
define('LITE_OP_DIV',89);
define('LITE_OP_MOD',90);
define('LITE_OP_ADD',84);
define('LITE_OP_SUB',85);
define('LITE_OP_LT',4176);
define('LITE_OP_GT',4177);
define('LITE_OP_LTEQ',4178);
define('LITE_OP_GTEQ',4179);
define('LITE_OP_EQ',80);
define('LITE_OP_NOTEQ',81);
define('LITE_OP_BIT_AND',8268);
define('LITE_OP_BIT_XOR',4172);
define('LITE_OP_BIT_OR',76);
define('LITE_OP_AND',4168);
define('LITE_OP_OR',73);
define('LITE_OP_QUESTION',68);
define('LITE_OP_QUESTION_SELECT',69);
define('LITE_OP_PARAM_JOIN',64);
define('LITE_OP_MAP_PUSH',65);


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
		$this->tokens = $tokens;
	}
	function evaluate(&$context) {
		return lite_evaluate($context,$this->tokens);
	}
}

function lite_evaluate(&$context,&$item) {
	$result = _lite_evaluate($context, $item);
	if ($result instanceof _LitePropertyValue) {
		$result = $result->get();
	}
	return $result;
}
function _lite_evaluate(&$context, &$item) {
    $type = $item[0];
    switch($type){
    case LITE_VALUE_NEW_LIST:
    case LITE_VALUE_NEW_MAP:
        return array();
    case LITE_VALUE_VAR:
        $arg1 = $item[1];
		if (array_key_exists($arg1, $context)) {
			return $context[$arg1];
		} else {
			switch ($arg1) {
			case "JSON":
				return "Expression";
			case "encodeURIComponent": 
				return "urlencode";
			case "decodeURIComponent": 
				return "urldecode";
			}
		}
		return null;
    case LITE_VALUE_CONSTANTS:
        return $item[1];
    ///* and or */
    case LITE_OP_AND:
    	$arg1 = lite_evaluate($context,$item[1]);
        return $arg1?$arg1 : lite_evaluate($context,$item[2]);
    case LITE_OP_OR:
    	$arg1 = lite_evaluate($context,$item[1]);
        return $arg1?lite_evaluate($context,$item[2]):$arg1;
    case LITE_OP_QUESTION://// a?b:c -> a?:bc -- >a?b:c
    	global $LITE_QUESTION_NEXT;
    	$arg1 = lite_evaluate($context,$item[1]);
        if(!arg1){
            return lite_evaluate($context,$item[2]);
        }else{
            return $LITE_QUESTION_NEXT;//use as flag
        }
    case LITE_OP_QUESTION_SELECT:
    	global $LITE_QUESTION_NEXT;
    	$arg1 = lite_evaluate($context,$item[1]);
        if(arg1 == $LITE_QUESTION_NEXT){//use as flag
            return lite_evaluate($context,$item[2]);
        }else{
            return $arg1;
        }
    }
    $arg1=_lite_evaluate($context,$item[1]);
    if((($type & LITE_BIT_ARGS) >> 6) == 1){//if(getArgCount(type) ==2){//
        $arg2=lite_evaluate($context,$item[2]);
    }
	if ($type == LITE_OP_INVOKE_METHOD) {
		if($arg1 instanceof _LitePropertyValue) {
			return $arg1->call($arg2);
		} else {
			return call_user_func_array($arg1, $arg2);
		}
	}
	//echo get_class ( $arg1);
	if ($arg1 instanceof _LitePropertyValue) {
		$arg1 = $arg1->get();
	}
	switch($type) {
		case LITE_OP_GET_STATIC_PROP:
			return new _LitePropertyValue($arg1, $item[3]);
		case LITE_OP_GET_PROP:
			return new _LitePropertyValue($arg1, $arg2);
		case LITE_OP_PARAM_JOIN:
			$arg1[]=$arg2;return $arg1;
		case LITE_OP_MAP_PUSH:
			if(is_array($arg1)){
				$arg1[$item[3]] = $arg2;
			}else{
				$arg1->$item[3] = $arg2;
			}
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

		default:break;
	}
}
		


class _LitePropertyValue {
	var $base;
	var $name;
	function _LitePropertyValue(&$base, &$name) {
		$this->base = $base;
		$this->name = $name;
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