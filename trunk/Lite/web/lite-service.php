<?php
require_once("WEB-INF/classes/lite/LiteService.php");
/**
 * 设置模板目录和模板缓存目录
 */
$service = new LiteService(dirname(realpath(__FILE__)),dirname(realpath(__FILE__)).'/WEB-INF/litecode');
/*
 * 设置是否允许外网上传文件
 */
//$service->setAllowExternalUpload(false);

/**
 * 设置JavaScript 库地址
 */
//$service->addJavaScriptSource(realpath('./WEB-INF/lib'));
//$service->addJavaScriptLib(realpath('./WEB-INF/lib'));
//$_REQUEST['LITE_ACTION'] = 'compile';
//$_REQUEST['LITE_PATH'] = '/test/logo.xhtml';
if(!array_key_exists('LITE_SERVICE_URL',$_REQUEST)){
	$_REQUEST['LITE_SERVICE_URL'] = $_SERVER['SCRIPT_NAME'];
}
$service->execute();
?>