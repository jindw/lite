var inc = 0;
function walk(d){
	inc ++;
	if(d.nodeType == 1){
		//print(d.nodeName);
	}
	if(d.firstChild){
		walk(d.firstChild);
	}
	if(d.nextSibling){
		walk(d.nextSibling);
	}
	
}
function testWalk(doc){
	var i0 = inc;
	var d = +new Date;
	walk(doc);
	return (new Date - d)+'/'+(inc - i0)
}

function compare(d1,d2){
	doCompare(d1,d2);
	print(d2+'')
}
function doCompare(d1,d2){
	assertEquals(d1,d2,'nodeType');
	assertEquals(d1,d2,'nodeName');
	assertEquals(d1,d2,'value');
	assertEquals(d1,d2,'nodeValue');
	assertEquals(d1,d2,'name');
	assertEquals(d1,d2,'data');
	var type = d1.nodeType;
	
	if(type == 1){//el
		var tagName = d1.tagName;
		var attrs1 = d1.attributes;
		var attrs2 = d2.attributes;
		var len = attrs1.length;
		assertEquals(attrs1,attrs2,'length',tagName);
		while(len --){
			var attr1 = attrs1.item(len);
			var attr2 = d2.getAttributeNodeNS(attr1.namespaceURI,attr1.localName);
			var attr1 = d1.getAttributeNodeNS(attr1.namespaceURI,attr1.localName);

			var attr12 = d1.getAttributeNode(attr1.name);
			var attr22 = d2.getAttributeNode(attr1.name);
			if(attr12 != attr1 || attr2 != attr22){
				error(tagName,'getAttributeNodeNS and getAttribute not same',[attr22.namespaceURI,attr22.name,attr12 == attr1,attr22 , attr2])
			}
			if(attr1 == null || attr2 == null){
				error(tagName,"attribute can not be null",attr1,':', attr2) ;
			}else{
				doCompare(attr1,attr2);
			}
			
		}
		
	}else if(type == 2){//attr
		return;
	}

	if(d1.firstChild){
		if(d2.firstChild){
			doCompare(d1.firstChild,d2.firstChild);
		}else{
			error('d2 not exists',d2,d2.parentNode)
		}
	}
	if(d1.nextSibling){
		if(d2.nextSibling){
			doCompare(d1.nextSibling,d2.nextSibling);
		}else{
			error('d2 not exists',d2,d2.parentNode)
		}
	}
}
function assertEquals(d1,d2,key,msg){
	var v1 = d1[key];
	var v2 = d2[key];
	if(v1 && v2 && typeof v1 == 'string'){
		v1 = v1.replace(/\s+/g,' ')
		v2 = v2.replace(/\s+/g,' ')
	}
	if(v1 != v2){
		error(msg,':',key ,' is not equals!!! for\n',v1,'\nand\n',v2 );
	}
}
function error(){
	java.lang.System.err.println([].join.call(arguments,' '));
}