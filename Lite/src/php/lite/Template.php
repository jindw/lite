<?php
require_once('Expression.php');
define('LITE_FOR_KEY', 'for');
define('LITE_IF_KEY', 'if');

class Template{

    function Template($items){
        $this->items = &$items;
    }
    function render($context){
        lite_render_list($context, $this->items);
    }
}
function lite_render_list(&$context, &$children){
    foreach($children as &$item){
        try{
            if(is_string($item)){
                echo $item;
            }else{
                switch($item[0]){
                	case 0://LITE_EL_TYPE:
                    	lite_process_el($context, $item, 0);
                    	break;
	                case 1://LITE_IF_TYPE:
	                    lite_process_if($context, $item);
                    	break;
	                case 3://LITE_XML_ATTRIBUTE_TYPE:
	                    lite_process_attribute($context, $item);
                    	break;
	                case 4://LITE_XML_TEXT_TYPE:
	                    lite_process_el($context, $item, 1);
                    	break;
	                case 5://LITE_FOR_TYPE:
	                    lite_process_for($context, $item);
                    	break;
	                case 6://LITE_ELSE_TYPE:
	                    lite_process_else($context, $item);
                    	break;
	                case 8://LITE_VAR_TYPE:
	                    lite_process_var($context, $item);
                    	break;
	                case 9://LITE_CAPTRUE_TYPE:
	                    lite_process_captrue($context, $item);
                    	break;
	            }
	        }
        }catch(Exception $e){
            echo $e;
        }
    }
}
function lite_print_xml_attribute(&$text){
    lite_print_xml_text($text);
}

function lite_print_xml_text(&$text){
    if(is_bool($text)){
        echo $text?'true':'false';
    }else{
        echo htmlspecialchars($text);
    }
}

function lite_to_bool(&$test){
    return !!$test;
}

function lite_process_el(&$context, &$data, $encodeXML){
	$stack = &$data[1];
    $value = lite_evaluate($context,$stack);
    if($encodeXML && isset($value)){
        lite_print_xml_text($value);
    }else{
        echo $value;
    }
}
function lite_process_if(&$context, &$data){
    try{
        if(lite_to_bool(lite_evaluate($context,$data[2]))){
        	$test = 1;
            lite_render_list($context, $data[1]);
        }else{
            $test = 0;
        }
        $context[LITE_IF_KEY]=$test;
    }catch(Exception $e){
        $context[LITE_IF_KEY]=$test;
        throw $e;
    }
}
function lite_process_else(&$context, &$data){
    if(array_key_exists(LITE_IF_KEY,$context) && !lite_to_bool($context[LITE_IF_KEY])){
        try{
            if(is_null($data[2]) || lite_to_bool(lite_evaluate($context,$data[2]))){
                $test = 1;
                lite_render_list($context, $data[1]);
            }else{
            	$test = 0;
            }
            $context[LITE_IF_KEY] = $test;
        }catch (Exception $e){
            $context[LITE_IF_KEY] = $test;
            throw $e;
        }
    }
}
function lite_process_for(&$context, &$data){
    $children = &$data[1];
    $items = lite_evaluate($context,$data[2]);
    $varName = &$data[3];
    if(array_key_exists(LITE_FOR_KEY,$context)){
    	$preiousStatus =  &$context[LITE_FOR_KEY];
    }
    try{
        $length = 0;
        $forStatus = array('index'=>-1);
        $context[LITE_FOR_KEY]=&$forStatus;
        if(is_numeric($items)){
        	$i=0;
        	$length = $items;
        	$forStatus['lastIndex']=$length-1;
	        while($i<$length){
	            $forStatus['index'] = $i++;
	            $context[$varName] = $i;
	            lite_render_list($context, $data);
	        }
        }else if(is_array($items)){
            $length = count($items);
        	$forStatus['lastIndex'] = $length-1;
	        foreach($items as &$item){
	            $forStatus['index'] += 1;
	            $context[$varName] = $item;
	            lite_render_list($context, $children);
	        }
        }
        $context[LITE_FOR_KEY]=&$preiousStatus;
        $context[LITE_IF_KEY]= $length > 0;
    }catch(Exception $e){
        $context[LITE_FOR_KEY]=$preiousStatus;
        $context[LITE_IF_KEY]= $length > 0;
        throw $e;
    }
}

function lite_process_var(&$context, &$data){
    $context[$data[2]]= lite_evaluate($context,$data[1]);
} 

function lite_process_captrue(&$context, &$data){
    ob_start();
    lite_render_list($context, $data[1]);
    $context[$data[2]]= ob_get_contents();
    ob_end_clean();
}

function lite_process_attribute(&$context, &$data){
    $result = lite_evaluate($context,$data[1]);
    if(is_null($data[2])){
        lite_print_xml_attribute($result);
    }elseif(isset($result)){
        echo ' ';
        echo $data[2];
        echo '="';
        lite_print_xml_attribute($result);
        echo '"';
    }
}
?>