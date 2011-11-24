<?php
header("Content-Type:text/html;charset=utf-8");
function showDir($dir){
$d = dir($dir);
echo "<h3>Path: " . $d->path . "</h3><div>\n";
while (false !== ($path = $d->read())) {
	$ext = strstr($path, '.');
	if($ext == '.json'){
   		echo "<p><h4><a href='./index.php/doc/$dir/$path'>$path</a> </h4>";
   		echo "<ul>";
   		showMessage($dir.$path);
   		echo "</ul>";
   		echo "</p>\n";
   	}
}
echo "</div>\n";
$d->close();
}
function showMessage($path){
	$data = @file_get_contents($path);
	$json = json_decode($data,true);
	if(!$json){
		return;
	}
	$save = false;
	foreach($json['messages'] as &$msg){
		$id = @$msg['id'];
		if(!$id){
			echo '<pre>'.json_encode($msg).'</pre>';
			$save = true;
			$id = $msg['id'] = uniqid();
			echo 'uniqid'.$msg['id'];
		}
		
		echo "<pre><a href='?path=$path&delete=$id'>X</a>".
			'<strong title="'.$msg['email'].'">'.htmlspecialchars($msg['username']).'</strong>'.
			htmlspecialchars($msg['content']).
			"\n".date('Y-m-d',$msg['postTime']/1000).
			"\n[".$msg['ip'].
			"\n][".$msg['email'].
			"]</pre>";
	}
	if($save){
		$data = json_encode($json);
		file_put_contents($path,$data);
	}
}
$delete = @$_GET['delete'];
$path = @$_GET['path'];
if($delete && $path){
	$data = @file_get_contents($path);
	$json = json_decode($data,true);
	if(!$json){
		return;
	}
	$newmsgs = array();
	foreach($json['messages'] as $msg){
		$id = @$msg['id'];
		if($id != $delete && $msg['email']){
			array_push($newmsgs,$msg);
		}
	}
	$json['messages'] = $newmsgs;
	$data = json_encode($json);
	file_put_contents($path,$data);
}
showDir('./guide/');
