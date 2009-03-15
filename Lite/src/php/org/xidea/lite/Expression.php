<?php
#value token
define('VALUE_CONSTANTS', -0x00);#c;

define('VALUE_VAR', -0x01);#n;
define('VALUE_LAZY', -0x02);
define('VALUE_NEW_LIST', -0x03);#[;
define('VALUE_NEW_MAP', -0x04);#{;
	
#��ű�� ????? !!
#9
define('OP_GET_PROP', 17);#0 | 16 | 1;
define('OP_STATIC_GET_PROP', 48);#32 | 16 | 0;
define('OP_INVOKE_METHOD', 81);#64 | 16 | 1;
#8
define('OP_NOT', 14);#0 | 14 | 0;
define('OP_POS', 46);#32 | 14 | 0;
define('OP_NEG', 78);#64 | 14 | 0;
#7
define('OP_MUL', 13);#0 | 12 | 1;
define('OP_DIV', 45);#32 | 12 | 1;
define('OP_MOD', 77);#64 | 12 | 1;
#6
define('OP_ADD', 11);#0 | 10 | 1;
#5
define('OP_SUB', 41);#32 | 8 | 1;
#4
define('OP_LT', 7);#0 | 6 | 1;
define('OP_GT', 39);#32 | 6 | 1;
define('OP_LTEQ', 71);#64 | 6 | 1;
define('OP_GTEQ', 103);#96 | 6 | 1;
define('OP_EQ', 135);#128 | 6 | 1;
define('OP_NOTEQ', 167);#160 | 6 | 1;
#3
define('OP_AND', 5);#0 | 4 | 1;
define('OP_OR', 37);#32 | 4 | 1;
#2
define('OP_QUESTION', 3);#0 | 2 | 1;
define('OP_QUESTION_SELECT', 35);#32 | 2 | 1;
#1
define('OP_PARAM_JOIN', 1);#0 | 0 | 1;
define('OP_MAP_PUSH', 33);#32 | 0 | 1;

$globalMap = array('JSON'=>null, 'encodeURIComponent'=> null, 'test'=> 'testMap');

function evaluate($tokens, $context) {
	$stack = array();
	_evaluate($stack, $tokens, $context);
	$stack=$stack[0];
	if ($stack instanceof PropertyValue) 
		$stack = $stack->base[$stack->name];
	return $stack;
}

function _evaluate(&$stack, $tokens, $context) {
	foreach($tokens as $item) {
		if(is_array($item)) {
			$type = $item[0];
			if($type > 0) {
				$arg1 = array_pop($stack);
				$arg2 = $arg1;
				if($type & 1) {
					$arg1 = array_pop($stack);
				}
				$result = compute($item, $arg1, $arg2);
				if($result instanceof LazyToken) {
					_evaluate($stack, $result->children, $context);
				} else {
					$stack[] = $result;
				}
			} else {
				$stack[] = getTokenValue($context, $item);
			}
		}
	}
}

function compute($op, $arg1, $arg2) {
	$type = $op[0];
	if ($type == OP_INVOKE_METHOD) {
		if($arg1 instanceof PropertyValue) {
			$base = $arg1->base;
			$name = $arg1->name;
			if (is_array($arg1) && array_key_exists($name, $base)) {
				return call_user_func_array($base[$name], $arg2);
			} else {
				return call_user_func_array(array($base,$name),$arg2);
			}
		} else {
			return call_user_func($arg1, $arg2);
		}
	}
	if ($arg1 instanceof PropertyValue) {
		$arg1 = $arg1->base[$arg1->name];
	}
	if ($arg2 instanceof PropertyValue) {
		$arg2 = $arg2->base[$arg2->name];
	}

	switch($type) {
		case OP_STATIC_GET_PROP:
			return new PropertyValue($arg1, $op[1]);
		case OP_GET_PROP:
			return new PropertyValue($arg1, $arg2);
		case OP_PARAM_JOIN:
			$arg1[] = $arg2;return $arg1;
		case OP_MAP_PUSH:
			$arg1[$op[1]] = $arg2;return $arg1;
		case OP_NOT:
			return !$arg1;
		case OP_POS:
			return +$arg1;
		case OP_NEG:
			return -$arg1;
		/* +-*%/ */
		case OP_ADD:
			return $arg1+$arg2;
		case OP_SUB:
			return $arg1-$arg2;
		case OP_MUL:
			return $arg1*$arg2;
		case OP_DIV:
			return $arg1/$arg2;
		case OP_MOD:
			return $arg1%$arg2;
		/* boolean */
		case OP_GT:
			return $arg1>$arg2;
		case OP_GTEQ:
			return $arg1>=$arg2;
		case OP_NOTEQ:
			return $arg1!=$arg2;
		case OP_EQ:
			return $arg1==$arg2;
		case OP_LT:
			return $arg1<$arg2;
		case OP_LTEQ:
			return $arg1<=$arg2;

		/* and or */
		case OP_AND:
			return $arg2 && $arg1;
		case OP_OR:
			return $arg1 || $arg2;
		case OP_QUESTION:
			return $arg1 ? $arg2 : new stdClass();
		case OP_MAP_PUSH:
			$arg1[$op[1]] = $arg2;return $arg1;
		default:break;
	}
}

function getTokenValue($context, $item) {
	global $globalMap;
	$type = $item[0];
	switch($type){
		case VALUE_CONSTANTS:
			return $item[1];
		case VALUE_VAR:
			$value = $item[1];
			if ('this'==$value) {
				return $context;
			} else {
				if (array_key_exists($value, $context)) {
					return $context[$value];
				} elseif (array_key_exists($value, $globalMap)) {
					return $globalMap[$value];
				}
				return null;
			}
		case VALUE_NEW_LIST:
			return array();
		case VALUE_NEW_MAP:
			return new stdClass();
		case VALUE_LAZY:
			return LazyToken($item[1]);
	}
}

class LazyToken {
	var $children = 0;
	function LazyToken($children) {
		$this->children = $children;
	}
}

class PropertyValue {
	var $base;
	var $name;
	function PropertyValue($base, $name) {
		$this->base = $base;
		$this->name = $name;
	}
}
?>