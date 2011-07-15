

function doFilterXHTML(factory){
	$data = this.getRawData($path);
	$text = this.toString($data);
	$xml = normalizeXML($text);
	$dom = parseXML($xml,$path);
	
	//on*,style,href,src,action
	//script/style
	var xpath="//*[local-name()='script' || local-name()='style']//@*[starts-with(local-name(),'on') or local-name()='style' or local-name()='href' or local-name()='src' or local-name()='action'  ]";
	var nodes = selectNodeByXPath($dom,xpath);
	for(var i=0,len = nodes.length;i<len;i++){
		var item = nodes.item(i);
		if(item.nodeType == 1){
			//script|style
		}else{
			var attrName = item.localName;
			switch(attrName){
			case 'href':
				break;
			case 'src':
			}
		}
	}
	
	return $dom;
}