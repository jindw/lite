<?php
require_once("LiteEngine.php");
$root = realpath(__DIR__.'/../').'/';
$engine = new LiteEngine($root);


$path =  '/example/test.xhtml';
$context = array("int1"=>1,"text1"=>'1');
$engine->render($path,$context);
