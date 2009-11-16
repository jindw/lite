<?php
/**
 * PHP实现类Javascript方法
 * $Id: fn.php 26193 2009-11-02 12:53:47Z  $
 */

if(!defined('PHP_INT_MAX')){
	define('PHP_INT_MAX',2147483647);
}


/**
 * 全局对象（Math,JSON）
 */
$Math   = array("E"=>M_E,"PI"=>M_PI,"LN2"=>M_LN2,"LN10"=>M_LN10,"LOG2E"=>M_LOG2E,"LOG10E"=>M_LOG10E,"SQRT1_2"=>M_SQRT1_2,"SQRT2"=>M_SQRT2);
$JSON   = new stdClass();
$Date   = new stdClass();
$String = new stdClass();
$Array  = new stdClass();
$Object = new stdClass();


/**
 * isFinite Evaluates an argument to determine whether it is a finite number
 * @liteGloblas
 */
function lite__isFinite($op) {
	if (!is_numeric($op)) {
		//in EcmasScript unnumeric string is infinite
		return false;
	}
	return is_finite($op);
}

/**
 * isNaN Evaluates an argument to determine if it is not a number
 * @liteGlobals
 */
function lite__isNaN($op) {
	return is_nan($op);
}

/**
 * parseInt Parses a string argument and returns an integer of the specified radix or base
 * @param int $radix
 * @liteGlobals
 * @return number
 */
function lite__parseInt($op, $radix=10) {
	return intval($op, $radix);
}

/**
 * parseFloat Parses a string argument and returns a floating point number
 * @liteGlobals
 * @return number
 */
function lite__parseFloat($op) {
	return floatval($op);
}

/**
 * use raurlencode 'cause it's is more comply with the RFC1738
 * converts blank to %20 as ECMA-262 does, on the contrary urlencode convert blank to '+'
 * @liteGlobals
 * @return string
 */
function lite__encodeURIComponent($item){
	return rawurlencode($item);
}

/**
 * @liteGlobals
 * @return string
 */
function lite__decodeURIComponent($item){
	return rawurldecode($item);
}

/**
 * @liteGlobals
 * @return string
 */
function lite__encodeURI($uri){
	$url_info = parse_url($uri);
	if (count($url_info) > 1) {
		$query = $url_info['query'];
	} else {
		// means URIComponent
		$query = $url_info['path'];
	}

	$query_segs = explode("&", $query);
	foreach ($query_segs as $index => $seg) {
		$item = explode("=", $seg);
		$query_segs[$index] = join("=", array_map('rawurlencode', $item));
	}

	return str_replace($query, join("&", $query_segs), $uri);
}

/**
 * @liteGlobals
 * @return string
 */
function lite__decodeURI($uri){
	return rawurldecode($uri);
}

/**
 * 获取属性懂得函数(当有特殊属性的时候(length),需要调用这个函数)
 * @param array|object $base
 * @param string $key
 * @return mixed
 */
function lite_op_get_property($obj, $key) {
	//switch 有陷阱
	if($key === 0){
		return $obj[0];
	}else if($key == 'length'){
		if(is_string($obj)){
			//TODO: add muti-charsets supported
			return mb_strlen($obj);
		}else{
			return count($obj);
		}
	}else{
		return $obj[$key];
	}
}

/**
 * binary op add
 * @param mixed $lop
 * @param mixed $v2
 * @return mixed
 */
function lite_op_add($lop, $rop) {
	switch(gettype($lop)){
	case "integer":
	case "double":
	case "boolean":
	case "NULL":
	//case 'date':
		switch(gettype($rop)){
			case "integer":
			case "double":
			case "boolean":
			case "NULL":
			//case 'date':
				return $lop + $rop;
		}
	}
	return $lop . $rop;
}

/**
 * binary op and
 * in ECMA-262 "0" is true but in php "0" is false
 * and empty array in ECMA-262 is true but in php on the contrary
 * @param mixed $lop
 * @param mixed $rop
 * @return mixed
 */
function lite_op_and($lop, $rop){
	/*
	if($lop === "0"
		|| is_array($lop)
		|| !empty($lop)) {
			if ($rop === "0"
				|| is_array($rop)
				|| !empty($rop)) {
					return $rop;
				}
		}
	return $lop;
	*/
	return $lop === false || ($lop == null && $lop == 0) ? $lop : $rop;
}

/*
 * binary op or
 * @param mixed $lop
 * @param mixed $rop
 * @return boolean
 */
function lite_op_or($lop, $rop){
	/*
	if ($lop === "0"
		|| is_array($lop)
		|| !empty($lop)) {
			return $lop;
		}
		return $rop;
	*/
	return $lop === false || ($lop == null && $lop == 0) ? $rop : $lop;
}


/**
 * max Returns the largest of zero or more numbers
 * @liteMethod Math
 * @return number
 */
function lite_member_max($obj){
	$ops = func_get_args();
	$ops = array_slice($ops, 1);
	return max($ops);
}

/**
 * min Returns the smallest of zero or more numbers.
 * @liteMethod Math
 * @return number
 */
function lite_member_min($obj){
	$ops = func_get_args();
	$ops = array_slice($ops, 1);
	return min($ops);
}

/**
 * random Returns a pseudo-random number in the range [0,1] — that is, 
 * between 0 (inclusive) and 1 (inclusive). 
 * @liteMethod Math
 * @return number
 */
function lite_member_random($obj) {
	return rand(0, 1);
}

/**
 * floor Returns the largest integer less than or equal to a number
 * @liteMethod Math
 * @return number
 */
function lite_member_floor($obj, $op) {
	return floor($op);
}

/**
 * round Returns the value of a number rounded to the nearest integer
 * @liteMethod Math
 * @return number
 */
function lite_member_round($obj, $op) {
	return round($op);
}

/**
 * json encode
 * @param string from_enc the source character set
 * @return string
 */
function lite_member_stringify($obj, $arg, $from_enc=NULL){
	global $JSON;
	if($obj !== $JSON){
		//TODO: throw TypeError
		return "";
	}
	if($from_enc == NULL){
		$from_enc =  mb_internal_encoding();
	}
	$from_enc = strtolower($from_enc);

	if ($from_enc != "utf8" && $from_enc != "utf-8") {
		$arg = lite_json__iconv($arg,$from_enc,'utf-8//TRANSLIT');
	}
	return json_encode($arg);
}	
/**
 * json decode
 */
function lite_member_parse($obj,$arg,$from_enc=NULL){
	global $JSON;
	if($obj !== $JSON){
		return "";
	}
	if($from_enc == NULL){
		$from_enc = mb_internal_encoding();
	}
	$from_enc = strtolower($from_enc);
	if ($from_enc != "utf8" && $from_enc != "utf-8") {
		$arg = iconv($from_enc, 'UTF-8//TRANSLIT', $arg);
		$arg = json_decode($arg,true);
		return lite_json__iconv($arg,'UTF-8//TRANSLIT',$from_enc);
	}else{
		return json_decode($arg,true);
	}
	
}
/**
 * TODO:改写为非递归
 */
function lite_json__iconv($json,$from_enc,$to_enc) {
	if(is_string($json)){
		return iconv($from_enc, $to_enc, $json);
	}else if(is_array($json)){
		$result = array();
		foreach ($json as $key => $value) {
			$key = iconv($from_enc, $to_enc, $key);
			//lite_json__iconv($key,$from_enc,$to_enc);
			$result[$key] = lite_json__iconv($value,$from_enc,$to_enc);
		}
		return $result;
	}
	return $json;
}


/**
 * 所有对象都应该支持的成员方法
 */
function lite_member_toString($obj, $radix=10){
        if (is_array($obj)) {
                return join(",", $obj);
        }
        if (is_object($obj)) {
                return $obj->__toString();
        }
        if (!is_numeric($obj)) {
                return (string)($obj);
        }
        if ($radix<2 || $radix>36) {
                throw new Error("Error: illegal radix {$radix}");
        }
        $number = floatval($obj);
        if ($radix === 10) {
                return $number;
        }

        list($integer, $decimal) = explode(".", $obj);
        $buffer = array(
                'int' => array(),
                'dec' => array(),
        );
/*
 *{{{ when no decimal, use base_conver instead
        while ($flag = intval($integer / $radix)) {
                array_unshift($buffer['int'], ($integer % $radix));
                $integer = $flag;
        }
        array_unshift($buffer['int'], ($integer % $radix));
 *}}}
 */
        $ret = base_convert($integer, 10, $radix);

        if (!is_null($decimal)) {
                $decimal = floatval("0." . $decimal);
                while ($flag = intval($decimal * $radix)) {
                        array_push($buffer['dec'], $flag);
                        $decimal = $decimal * $radix  - $flag;
                }
                $ret .= "." . join("", $buffer['dec']);
        }

        return $ret;
}

/**
 * 给数组和字符串取子系列
 * support the negative slice length
 * @liteMethod array/string
 * @return mixed
 */
function lite_member_slice($obj, $start, $end=0){
	if (is_array($obj)) {
		$len = count($obj);
		$slice_len = (($len + $end) % $len) - $start;
		if ($slice_len < 0) {
			return array_slice($obj, $start + $slice_len, abs($slice_len));
		}else {
			return array_slice($obj, $start, $slice_len);
		}
	}
	$obj = strval($obj);
	// support the end is negative integer 
	$len = mb_strlen($obj);
	$sub_len = (($len + $end) % $len) - $start;
	if ($sub_len < 0) {
		return mb_substr($obj, $start + $sub_len, abs($sub_len));
	} else {
		return mb_substr($obj, $start, $sub_len);
	}
}

/**
 * join
 * @liteMethod string/array
 * @return mixed
 */
function lite_member_join($obj, $separator=""){
	return join($separator, $obj);
}

/**
 * split
 * @liteMethod string/array
 * @return mixed
 */
function lite_member_split($obj, $separator=",", $limit=false){
	if (!$limit) {
		return explode($separator, $obj);
	}
	return explode($separator, $obj, $limit);
}

/**
 * charAt Returns the character at the specified index
 * @liteMethod string
 * @param int $index
 * @return char
 */
function lite_member_charAt($obj, $index) {
	$len = mb_strlen($obj);
	if ($index <0 || $index >= $len) {
		return "";
	}
	return mb_substr($obj, $index, 1);
}

/**
 * charCodeAt Returns a number indicating the unicode code value of the character at the given index
 * @liteMethod string
 * @return number
 */
function lite_member_charCodeAt($obj, $index) {
    $len = mb_strlen($obj);
    if ($index <0 || $index >= $len) {
        //TODO: how to return a  NaN
        return -1;
    }
    $char = mb_substr($obj, $index, 1);
    $char = iconv(mb_internal_encoding(), 'utf32', $char);
    $code = unpack("Lbom/Lcode", $char);
    return $code['code'];
}

/**
 * indexOf Returns the index within the calling String object
 * of the first occurrence of the specified value, or -1 if not found.
 * @liteMethod string
 * @return number
 */
function lite_member_indexOf($obj, $needle, $from_index=0) {
	return false === ($index = mb_strpos($obj, $needle, $from_index))? -1 : $index;
}

/**
 * lastIndexOf Returns the index within the calling String object
 *  of the last occurrence of the specified value, or -1 if not found. 
 *  The calling string is searched backward, starting at fromIndex
 * @liteMethod string
 * @return number
 */
function lite_member_lastIndexOf($obj, $needle, $from_index=0) {
	return false === ($index = mb_strrpos($obj, $needle, $from_index))? -1 : $index;
}

/**
 * match Used to retrieve the matches when 
 * matching a string against a regular expression
 * @liteMethod string
 * @deprecate  ugly implement
 * @param string $regexp pcre regular expression
 * @return array
 */
function lite_member_match($obj, $regexp) {
	preg_match_all($regexp, $obj, $matches);
	return $matches;
}

/**
 * replace Used to find a match between a regular expression 
 * and a string, and to replace the matched substring with a new substring
 * @deprecate ugly implement
 * @param string  $regexp pcre regular expression or a string
 * @return string
 */
function lite_member_replace($obj, $regexp, $replacement) {
    if (preg_match("/^([\/#%$]).*?\\1(\w+)?$/", $regexp)) {
        return preg_replace($regexp, $replacement, $obj);
    } else {
        return str_replace($regexp, $replacement, $obj);
    }
}

/**
 * toLowerCase Returns the calling string value converted to lower case
 */
function lite_member_toLowerCase($obj) {
	return mb_strtolower($obj);
}

/**
 * toUpperCase Returns the calling string value converted to uppercase
 */
function lite_member_toUpperCase($obj) {
	return mb_strtoupper($obj);
}

/**
 * substr  Returns the characters in a string beginning 
 * at the specified location through the specified number of characters
 * @liteMethod string
 */
function lite_member_substr($obj, $start, $length=false) {
	if ($length) {
		return mb_substr($obj, $start, $length);
	}
	return mb_substr($obj, $start);
}

/**
 * substring Returns a subset of a string between one index and another, 
 * or through the end of the string
 * @liteMethod string
 * @return string
 */
function lite_member_substring($obj, $start, $end=false) {
	$len = mb_strlen($obj);
	if ($end) {
		$length = ($len + $end) % $len - $start;
		return mb_substr($obj, $start, $length);
	}
	return mb_substr($obj, $start);
}

/**
 * array pop Removes the last element from an array and returns that element 
 * @liteMethod array
 * @return mixed 
 */
function lite_member_pop(&$obj) {
	return array_pop($obj);
}

/**
 * array push Mutates an array by appending the given elements 
 * and returning the new length of the array
 * @liteMethod array
 * @return int number of elements had be pushed
 */
function lite_member_push(&$obj, $dummy) {
	$ops = func_get_args();
	$ret = 0;
	for ($i=1,$l=count($ops);$i<$ops;$i++) {
		$ret += array_push($obj, $ops[$i]);
	}
	return $ret;
}

/**
 * array shift Removes the first element from an array and returns that 
 * element. This method changes the length of the array
 */
function lite_member_shift(&$obj) {
	return array_shift($obj);
}

/**
 * array unshift Adds one or more elements to the beginning of 
 * an array and returns the new length of the array
 * @liteMethod array
 * @return int number of elements had be added
 */
function lite_member_unshift(&$obj, $dummy) {
	$ops = func_get_args();
	$ret = 0;
	for ($i=1,$l=count($ops);$i<$ops;$i++) {
		$ret += array_unshift($obj, $ops[$i]);
	}
	return $ret;
}

/**
 * array splice Changes the content of an array, adding new elements while removing old elements
 * @liteMethod array
 * @vaarg
 * @return array the elements had been removed
 */
function lite_member_splice(&$obj, $index=0) {
	$argc = func_num_args();
	if ($arg < 3) {
		return array_splice($obj, $index);
	} else if ($arg == 3) {
		return array_splice($obj, $index, $count);
	} else {
		$argv = func_get_args();
		$argv = array_slice($argv, 3);
		return array_splice($obj, $index, $count, $argv);
	}
}

/**
 * array reverse Reverses an array in place.  
 * The first array element becomes the last and the last becomes the first.
 * @liteMethod array
 * @return return a refer to the reversed array 
 */
function &lite_member_reverse(&$obj) {
	$obj = array_reverse($obj);
	return $obj;
}

/**
 * 实现js函数的toFixed方法
 * @liteMethod number
 * @return string
 */
function lite_member_toFixed($obj, $digits=0){
	$ret = sprintf("%.{$digits}f", $obj);
	return $ret;
}


/*
 * Local variables:
 * tab-width: 4
 * c-basic-offset: 4
 * End:
 * vim600: noet sw=4 ts=4 fdm=marker
 * vim<600: noet sw=4 ts=4
 */
?>
