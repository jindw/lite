<?php

define('EL_TYPE',0);// [0,'el']
define('IF_TYPE',1);// [1,[...],'test']
define('BREAK_TYPE',2);// [2,depth]
define('XML_ATTRIBUTE_TYPE',3);// [3,'value','name']
define('XML_TEXT_TYPE',4);// [4,'el']
define('FOR_TYPE',5);// [5,[...],'var','items','status']#status
define('ELSE_TYPE',6);// [6,[...],'test']#test opt?
define('ADD_ONS_TYPE',7);// [7,[...],'var']
define('VAR_TYPE',8);// [8,'value','name']
define('CAPTRUE_TYPE',9);// [9,[...],'var']

define('FOR_KEY', "for");
define('IF_KEY', "if");

class Template{
    function Template($items){
        $this->items = $items;
    }
    function render($context,$out){
        renderList($context, $this->items, $out);
    }
}
    
function renderList($context, $children, $out){
    foreach($children as $item){
        try{
            if(is_string($item)){
                $out.write($item);
            }else{
                switch($item[0]){
                	case EL_TYPE:
                    	return processExpression($context, $item, $out, 0);
	                case XML_TEXT_TYPE:
	                    return processExpression($context, $item, $out, 1);
	                case VAR_TYPE:
	                    return processVar($context, $item);
	                case CAPTRUE_TYPE:
	                    return processCaptrue($context, $item);
	                case IF_TYPE:
	                    return processIf($context, $item, $out);
	                case ELSE_TYPE:
	                    return processElse($context, $item, $out);
	                case FOR_TYPE:
	                    return processFor($context, $item, $out);
	                case XML_ATTRIBUTE_TYPE:
	                    return processAttribute($context, $item, $out);
	            }
	        }
        }catch(Exception $e){
            echo "Exception:"; 
            echo $item[0];
            echo $e;
        }
    }
}
function printXMLAttribute($text, $out){
    $out.write(htmlspecialchars($text));
}

function printXMLText($text, $out){
    $out.write(htmlspecialchars($text));
}

function toBoolean($test){
    return !!$test;
}

function processExpression($context, $data, $out, $encodeXML){
    $value = evaluate($data[1],$context);
    if($encodeXML && isset($value)){
        printXMLText("$value", out);
    }else{
        out.write("$value");
    }
}
function processIf($context, $data, $out){
    try{
        if(toBoolean(evaluate($data[2],$context))){
        	$test = 1;
            renderList($context, $data[1], $out);
        }else{
            $test = 0;
        }
        $context[IF_KEY]=$test;
    }catch(Exception $e){
        $context[IF_KEY]=$test;
        throw $e;
    }
}
function processElse($context, $data, $out){
    if(!toBoolean($context[IF_KEY])){
        try{
            if(isnull($data[2]) || toBoolean(evaluate($data[2],$context))){
                $test = 1;
                renderList($context, $data[1], $out);
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
function processFor($context, $data, $out){
    $children = $data[1];
    $varName = $data[2];
    $statusName = $data[4];
    $items = evaluate($data[3],$context);
    $length = count($items);
    $preiousStatus = array_key_exists(FOR_KEY,$context) && $context[FOR_KEY];
    try{
        $forStatus = ForStatus($length);
        $context[FOR_KEY]=$forStatus;
        if(isset($statusName)){
            $context[$statusName]=$forStatus;
        }
        foreach($items as $item){
            $forStatus->index += 1;
            $context[$varName]=$item;
            renderList($context, $data, $out);
        }
        $context[FOR_KEY]=$preiousStatus;
        $context[IF_KEY]= $length > 0;
    }catch(Exception $e){
        $context[FOR_KEY]=$preiousStatus;
        $context[IF_KEY]= $length > 0;
        throw $e;
    }
}

function processVar($context, $data){
    $context[$data[2]]= evaluate($data[1],$context);
} 

function processCaptrue($context, $data){
    $buf = StringWriter();
    renderList($context, $data[1], $buf);
    $context[$data[2]]= "$buf";
}

function processAttribute($context, $data, $out){
    $result = evaluate($data[1],$context);
    if(isnull($data[2])){
        printXMLAttribute("$result",  $out, 1);
    }elseif(isset($result)){
        out.write(" ");
        out.write($data[2]);
        out.write("=\"");
        printXMLAttribute("$result",$out, 0);
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