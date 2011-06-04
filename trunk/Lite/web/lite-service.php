<?php
require_once("WEB-INF/classes/lite/LiteService.php");
/**
 * 设置模板目录和模板缓存目录
 */
$service = new LiteService(realpath('./'),realpath('./WEB-INF/litecode'));
/*
 * 设置是否允许外网上传文件
 */
//$service->setAllowExternalUpload(false);

/**
 * 设置JavaScript 库地址
 */
//$service->addJavaScriptSource(realpath('./WEB-INF/lib'));
//$service->addJavaScriptLib(realpath('./WEB-INF/lib'));

$service->execute();
?>