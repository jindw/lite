<?php
require_once("Expression.php");

define('EL_TYPE',0);            // [0,<el>]
define('IF_TYPE',1);            // [1,[...],<test el>]
define('BREAK_TYPE',2);         // [2,depth]
define('XML_ATTRIBUTE_TYPE',3); // [3,<value el>,'name']
define('XML_TEXT_TYPE',4);      // [4,<el>]
define('FOR_TYPE',5);           // [5,[...],<items el>,'varName']
define('ELSE_TYPE',6);          // [6,[...],<test el>] //<test el> 可为null
define('ADD_ON_TYPE',7);        // [7,[...],<add on el>,'<addon-class>']
define('VAR_TYPE',8);           // [8,<value el>,'name']
define('CAPTRUE_TYPE',9);       // [9,[...],'var']


define('FOR_KEY', "for");
define('IF_KEY', "if");

class Template{
    function Template($items){
        $this->items = $items;
    }
    function render($context){
        renderList($context, $this->items);
    }
}
    
function renderList(&$context, &$children){
    foreach($children as $item){
        try{
            if(is_string($item)){
                echo $item;
            }else{
                switch($item[0]){
                	case EL_TYPE:
                    	processExpression($context, $item, 0);
                    	break;
	                case XML_TEXT_TYPE:
	                    processExpression($context, $item, 1);
                    	break;
	                case VAR_TYPE:
	                    processVar($context, $item);
                    	break;
	                case CAPTRUE_TYPE:
	                    processCaptrue($context, $item);
                    	break;
	                case IF_TYPE:
	                    processIf($context, $item);
                    	break;
	                case ELSE_TYPE:
	                    processElse($context, $item);
                    	break;
	                case FOR_TYPE:
	                    processFor($context, $item);
                    	break;
	                case XML_ATTRIBUTE_TYPE:
	                    processAttribute($context, $item);
                    	break;
	            }
	        }
        }catch(Exception $e){
            echo $e;
        }
    }
}
function printXMLAttribute(&$text){
    echo htmlspecialchars("".$text);
}

function printXMLText(&$text){
    echo htmlspecialchars("".$text);
}

function toBoolean(&$test){
    return !!$test;
}

function processExpression(&$context, &$data, $encodeXML){
	$stack = $data[1];
    $value = evaluate($stack,$context);
    if($encodeXML && isset($value)){
    	$value = "$value";
        printXMLText($value);
    }else{
        echo $value;
    }
}
function processIf(&$context, &$data){
    try{
        if(toBoolean(evaluate($data[2],$context))){
        	$test = 1;
            renderList($context, $data[1]);
        }else{
            $test = 0;
        }
        $context[IF_KEY]=$test;
    }catch(Exception $e){
        $context[IF_KEY]=$test;
        throw $e;
    }
}
function processElse(&$context, &$data){
    if(array_key_exists(IF_KEY,$context) && !toBoolean($context[IF_KEY])){
        try{
            if(is_null($data[2]) || toBoolean(evaluate($data[2],$context))){
                $test = 1;
                renderList($context, $data[1]);
            }else{
            	$test = 0;
            }
            $context[IF_KEY] = $test;
        }catch (Exception $e){
            $context[IF_KEY] = $test;
            throw $e;
        }
    }
}
function processFor(&$context, &$data){
    $children = $data[1];
    $items = evaluate($data[2],$context);
    $varName = $data[3];
    $length = count($items);
    $preiousStatus = array_key_exists(FOR_KEY,$context) && $context[FOR_KEY];
    try{
        $forStatus = new ForStatus($length);
        $context[FOR_KEY]=$forStatus;
        if(is_numeric($items)){
	        for($i=0;$i<$items;$i++){
	            $forStatus->index += 1;
	            $context[$varName]=$i+1;
	            renderList($context, $data);
	        }
        }else if(is_array($items)){
	        foreach($items as $item){
	            $forStatus->index += 1;
	            $context[$varName]=$item;
	            renderList($context, $children);
	        }
        }
        $context[FOR_KEY]=$preiousStatus;
        $context[IF_KEY]= $length > 0;
    }catch(Exception $e){
        $context[FOR_KEY]=$preiousStatus;
        $context[IF_KEY]= $length > 0;
        throw $e;
    }
}

function processVar(&$context, &$data){
    $context[$data[2]]= evaluate($data[1],$context);
} 

function processCaptrue(&$context, &$data){
    ob_start();
    renderList($context, $data[1]);
    $context[$data[2]]= ob_get_contents();
    ob_end_clean();
}

function processAttribute(&$context, &$data){
    $result = evaluate($data[1],$context);
    if(isnull($data[2])){
        printXMLAttribute($result);
    }elseif(isset($result)){
        out.write(" ");
        out.write($data[2]);
        out.write("=\"");
        printXMLAttribute($result);
        out.write('"');
    }
}

class ForStatus{
    var $index = -1;
    var $lastIndex = 0;

    function ForStatus($end){
        $this->lastIndex = $end - 1;
    }
}
?>