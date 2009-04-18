<?php
require_once('Expression.php');
define('LITE_FOR_KEY', 'for');
define('LITE_IF_KEY', 'if');

class Template{

    function Template($items){
        $this->items = &$items;
    }
    function render($context){
        $this->renderList($context, $this->items);
    }
    function renderList(&$context, &$children){
	    foreach($children as &$item){
	        try{
	            if(is_string($item)){
	                echo $item;
	            }else{
	                switch($item[0]){
	                	case 0://LITE_EL_TYPE:
	                    	$this->processExpression($context, $item, 0);
	                    	break;
		                case 1://LITE_IF_TYPE:
		                    $this->processIf($context, $item);
	                    	break;
		                case 3://LITE_XML_ATTRIBUTE_TYPE:
		                    $this->processAttribute($context, $item);
	                    	break;
		                case 4://LITE_XML_TEXT_TYPE:
		                    $this->processExpression($context, $item, 1);
	                    	break;
		                case 5://LITE_FOR_TYPE:
		                    $this->processFor($context, $item);
	                    	break;
		                case 6://LITE_ELSE_TYPE:
		                    $this->processElse($context, $item);
	                    	break;
		                case 8://LITE_VAR_TYPE:
		                    $this->processVar($context, $item);
	                    	break;
		                case 9://LITE_CAPTRUE_TYPE:
		                    $this->processCaptrue($context, $item);
	                    	break;
		            }
		        }
	        }catch(Exception $e){
	            echo $e;
	        }
	    }
	}
	function printXMLAttribute(&$text){
	    echo htmlspecialchars($text);
	}
	
	function printXMLText(&$text){
	    echo htmlspecialchars($text);
	}
	
	function toBoolean(&$test){
	    return !!$test;
	}
	
	function processExpression(&$context, &$data, $encodeXML){
		$stack = &$data[1];
	    $value = Expression::evaluate($context,$stack);
	    if($encodeXML && isset($value)){
	    	$value = "$value";
	        $this->printXMLText($value);
	    }else{
	        echo $value;
	    }
	}
	function processIf(&$context, &$data){
	    try{
	        if($this->toBoolean(Expression::evaluate($context,$data[2]))){
	        	$test = 1;
	            $this->renderList($context, $data[1]);
	        }else{
	            $test = 0;
	        }
	        $context[LITE_IF_KEY]=$test;
	    }catch(Exception $e){
	        $context[LITE_IF_KEY]=$test;
	        throw $e;
	    }
	}
	function processElse(&$context, &$data){
	    if(array_key_exists(LITE_IF_KEY,$context) && !$this->toBoolean($context[LITE_IF_KEY])){
	        try{
	            if(is_null($data[2]) || $this->toBoolean(Expression::evaluate($context,$data[2]))){
	                $test = 1;
	                $this->renderList($context, $data[1]);
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
	function processFor(&$context, &$data){
	    $children = &$data[1];
	    $items = Expression::evaluate($context,$data[2]);
	    $varName = &$data[3];
	    $length = count($items);
	    if(array_key_exists(LITE_FOR_KEY,$context)){
	    	$preiousStatus =  &$context[LITE_FOR_KEY];
	    }
	    try{
	        $forStatus = array('index'=>-1,'lastIndex'=>$length-1);
	        $context[LITE_FOR_KEY]=&$forStatus;
	        if(is_numeric($items)){
		        for($i=0;$i<$items;$i++){
		            $forStatus['index'] += 1;
		            $context[$varName]=$i+1;
		            $this->renderList($context, $data);
		        }
	        }else if(is_array($items)){
		        foreach($items as &$item){
		            $forStatus['index'] += 1;
		            $context[$varName]=$item;
		            $this->renderList($context, $children);
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
	
	function processVar(&$context, &$data){
	    $context[$data[2]]= Expression::evaluate($context,$data[1]);
	} 
	
	function processCaptrue(&$context, &$data){
	    ob_start();
	    $this->renderList($context, $data[1]);
	    $context[$data[2]]= ob_get_contents();
	    ob_end_clean();
	}
	
	function processAttribute(&$context, &$data){
	    $result = Expression::evaluate($context,$data[1]);
	    if(is_null($data[2])){
	        $this->printXMLAttribute($result);
	    }elseif(isset($result)){
	        echo ' ';
	        echo $data[2];
	        echo '="';
	        $this->printXMLAttribute($result);
	        echo '"';
	    }
	}
	    
}
?>