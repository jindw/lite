/**<?php
//
// PHP 版本的JSI 代理程序（开发期间使用）
// 功能：
// 1.即时编译
// 2.类库支持
// 3.JSIDoc系列工具支持
//
$encoding = "UTF-8";
$exportService = "http://litecompiler.appspot.com/scripts/export.action";
?>
<?php
echo("/");
ob_clean();
if(array_key_exists('path',$_GET)){
   $path = $_GET['path'];
}else if(array_key_exists('PATH_INFO',$_SERVER)){
   $path = $_SERVER['PATH_INFO'] ;
   $path = substr($path, 1);
}else{
   $path = null;
}
if($path == 'export.action'){
    //转发到指定jsa服务器
    if($exportService){
	    global $exportService;  
		$postdata = http_build_query(
		    $_POST
		);
		$opts = array('http' =>
		    array(
		        'method'  => 'POST',
		        'header'  => 'Content-type: application/x-www-form-urlencoded',
		        'content' => $postdata
		    )
		);
		$context  = stream_context_create($opts);
		echo file_get_contents($exportService, false, $context);
    }else{
        header("HTTP/1.0 404 Not Found");
    }
    return;
}
function isValidFile($dir,$path){
    if(preg_match('/\\\\|\\//',$path) || $path == "lazy-trigger.js"
         || filesize(realpath("$dir/$path"))>200){
        return true;///[\/\\]/
    }
    return false;

}
function printEntry($path){
    global $encoding;
    if(!findEntry(".",$path,"findFromXML")){
	    $classpath = array(
		    './',
	        "../WEB-INF/classes"
	        //,"../../../JSI2/web/scripts/"//这里可以定义其他类路径
	    );
        foreach ($classpath as $dir){
	        if(file_exists(realpath("$dir/$path")) && isValidFile($dir,$path)){
	            header("Content-Type:".findMimiType($path).";charset=$encoding");
	            readfile(realpath("$dir/$path"));
	            return;
	        }
	    }
	    if(!findEntry("../WEB-INF/lib/",$path,"findFromZip")){
	        header("HTTP/1.0 404 Not Found");
	    }
    }
}
function findEntry($base,$path,$action){
    $base = realpath($base);
    if($base){
        $miss_zip = false;
        $dir = dir($base); 
        while (false !== ($file = $dir->read())) {
            if($action("$base/$file",$path)){
                $dir->close();
                return true;
            }
        }
        $dir->close();
    }
}
function findFromZip($file,$path){
    global $encoding;
    if(preg_match('/.*\.(?:jar|zip)$/i',$file)){
	    if(function_exists("zip_open")){
		    $zip = zip_open($file);
		    if (!is_resource($zip)) {
		    	 die(zipFileErrMsg($zip));
		    }
		    while ($entry = zip_read($zip)) {
		        if (zip_entry_name($entry) == $path && zip_entry_open($zip, $entry, "r")) {
		            $contentType = findMimiType($path);
		            if(preg_match('/^image\//i',$contentType)){
                        header("Content-Type:$contentType;");
                    }else{
		                header("Content-Type:$contentType;charset=$encoding");
		            }
		            echo zip_entry_read($entry, zip_entry_filesize($entry));
		            zip_entry_close($entry);
		            zip_close($zip);
		            return true;
		        }
		    }
		    zip_close($zip);
	    }else{
	        echo "//您的php没有安装zip扩展,无法遍历zip格式类库";
	        return true;
	    }
    }
}
function findFromXML($file,$path){
    global $encoding;
	if(preg_match('/.*\.xml$/i',$file)){
	    $xml = simplexml_load_file($file);
	    $result = $xml->xpath("//entry[@key='$path' or @key='$path#base64']");
	    if($result){
	        $contentType = findMimiType($path);
			while(list( $key, $node) = each($result)) {
			    if(preg_match('/#base64$/i',$node['key'])){
                    header("Content-Type:$contentType;");
			        echo base64_decode($node);
			    }else{
                    header("Content-Type:$contentType;charset=$encoding");
			        echo $node;
			    }
			    
		    }
		    return true;
		}
    }
}
function findMimiType($path){
    switch(strtolower(preg_replace('/.*\./',".",$path))){
    case '.css':
        return "text/css";
    case '.png':
        return "image/png";
    case '.gif':
        return "image/gif";
    case '.jpeg':
    case '.jpg':
        return "image/jpeg";
    default:
        return "text/html";
    }
}
function findPackageList($root) {
    $result = array();
    walkPackageTree($root, null, $result);
    $count = count($result);
    $buf= '';
    for($i=0;$i<$count;$i++){
        $buf=$buf.",".$result[$i];
    }
    return substr($buf,1);

}

function walkPackageTree($base, $prefix, &$result) {
    if ($prefix) {
        $subPrefix = $prefix .'.' . basename($base);
    } else {
	    if ($prefix === null) {
	        $subPrefix = "";
	    } else {
	        $subPrefix = basename($base);
        }
    }
    if ($subPrefix && file_exists($base."/__package__.js")){
        array_push($result,$subPrefix);
    }
    $dir = dir($base);
    while (false !== ($file = $dir->read())) {
        if (is_dir("$base/$file")) {
            if (substr($file,0,1) != ".") {
                walkPackageTree("$base/$file", $subPrefix, $result);
            }
        }
    }
    $dir->close();

}




if($path != null){
    $filePath = preg_replace("/__preload__\.js$/",".js",$path);
    $pos = strrpos($path, '/');
    $fileName = substr($filePath, $pos + 1);
    $packageName = preg_replace("/\//", "." ,substr($path, 0, $pos));
    if($filePath!=$path){
        echo("\$JSI.preload('$packageName','$fileName',function(){eval(this.varText);");
        printEntry($filePath);
        echo("\n})");
    }else{
        printEntry($path);
    }
}else{
    //TODO:require
    if(array_key_exists('externalScript',$_GET)){
        $externalScript = $_GET['externalScript'];
    }else{
        $externalScript = findPackageList(realpath("."));
    }
    header("Content-Type:text/html;charset=$encoding");
    echo("<html><frameset rows='100%'><frame src='index.php/org/xidea/jsidoc/index.html?group.All%20Scripts=$externalScript'></frame></frameset></html>");
}

function zipFileErrMsg($errno) {
  // using constant name as a string to make this function PHP4 compatible
  $zipFileFunctionsErrors = array(
    'ZIPARCHIVE::ER_MULTIDISK' => 'Multi-disk zip archives not supported.',
    'ZIPARCHIVE::ER_RENAME' => 'Renaming temporary file failed.',
    'ZIPARCHIVE::ER_CLOSE' => 'Closing zip archive failed',
    'ZIPARCHIVE::ER_SEEK' => 'Seek error',
    'ZIPARCHIVE::ER_READ' => 'Read error',
    'ZIPARCHIVE::ER_WRITE' => 'Write error',
    'ZIPARCHIVE::ER_CRC' => 'CRC error',
    'ZIPARCHIVE::ER_ZIPCLOSED' => 'Containing zip archive was closed',
    'ZIPARCHIVE::ER_NOENT' => 'No such file.',
    'ZIPARCHIVE::ER_EXISTS' => 'File already exists',
    'ZIPARCHIVE::ER_OPEN' => 'Can\'t open file',
    'ZIPARCHIVE::ER_TMPOPEN' => 'Failure to create temporary file.',
    'ZIPARCHIVE::ER_ZLIB' => 'Zlib error',
    'ZIPARCHIVE::ER_MEMORY' => 'Memory allocation failure',
    'ZIPARCHIVE::ER_CHANGED' => 'Entry has been changed',
    'ZIPARCHIVE::ER_COMPNOTSUPP' => 'Compression method not supported.',
    'ZIPARCHIVE::ER_EOF' => 'Premature EOF',
    'ZIPARCHIVE::ER_INVAL' => 'Invalid argument',
    'ZIPARCHIVE::ER_NOZIP' => 'Not a zip archive',
    'ZIPARCHIVE::ER_INTERNAL' => 'Internal error',
    'ZIPARCHIVE::ER_INCONS' => 'Zip archive inconsistent',
    'ZIPARCHIVE::ER_REMOVE' => 'Can\'t remove file',
    'ZIPARCHIVE::ER_DELETED' => 'Entry has been deleted',
  );
  $errmsg = 'unknown';
  foreach ($zipFileFunctionsErrors as $constName => $errorMessage) {
    if (defined($constName) and constant($constName) === $errno) {
      return 'Zip File Function error: '.$errorMessage;
    }
  }
  return 'Zip File Function error: unknown';
}


return;
?>/**/