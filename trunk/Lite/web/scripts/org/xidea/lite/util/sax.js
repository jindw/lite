
function parseXML(xml,begin,end,text,cdata,doctype,ins,comm) {
	var start = 0;
	var len = xml.length;
	while(start<len){
		var i = start;
		var c = xml.charAt(i++)
		if(c === '<'){
			c = xml.charAt(i++)
			if(c === '!'){
				c = xml.charAt(i++);
				if(c == '-'){//ignore comment start check
					var end = xml.indexOf('-->',i)+3;
					if(end>3){
						comm && comm(xml,start,start=end);
					}else{
						$log.error("找不到注释结束符",getPosition(xml,start))
						text(xml,start,start+=2);
					}
				}else if(c == '['){//ignore cdata start check
					end = xml.indexOf(']]>',i)+3;
					if(end>3){
						cdata(xml,start,start=end);
					}else{
						$log.error("找不到CDATA结束符",getPosition(xml,start))
						text(xml,start,start+=2);
					}
				}else{
					end = parseDoctype(doctype,xml,start);
					if(end<0){
						$log.error("找不到DOCTYPE结束符",getPosition(xml,start))
						text(xml,start,start+=2);
					}
				}
			}else if(c === '?'){
				end = xml.indexOf('?>',i)+2;
				if(end>2){
					ins(xml,start,start=end);
				}else{
					$log.error("找不到XML 处理器结束符",getPosition(xml,start))
					text(xml,start,start+=2);
				}
			}else if(c === '/'){
				end = xml.indexOf('>',i)+1;
				if(end>1){
					end(xml,start,end,xml.substring(start+2,end-1));
					start=end;
				}else{
					$log.error("找不到变迁结束符",getPosition(xml,start))
					text(xml,start,start+=2);
				}
			}else{
				start = parseElement(begin,xml,i-2);
				continue;
			}
		}else{
			end = xml.indexOf('<',i);
			if(end<0){
				end = len;
			}
			text(xml,start,start = end)
		}
	}
}
function parseElement(begin,text,start){
	var len = text.length;
	var i = start+1;
	var token = [];
	var buf = [];
	var ps = []
	while(i<len){
		var c = text.charAt(i++);
		switch(c){
		case '<':
			i--;
		case '>':
			begin(text,start,token,ps)//text,start,name,attrMap
			return i;
		case '"':
		case '\'':
			i = text.indexOf(c,i);
			if(i>0){
				start = i+1;
			}else{
				$log.error("元素解析异常",getPositon(text,start));
				return text.length;
			}
		case '\r':
		case '\n':
		case '\t':
		case ' ':
		case '=':
			if(buf.length){
				token.push(buf.join());
				buf.length = 0;
				ps.push(i);
			}
			if(c == '='){
				token.push('=')
			}
			break;
		default:
			buf.push(c);
		}
	}
	return text.length;
}
function parseDoctype(doctype,text,start){
	var exp = /<!DOCTYPE\s+(\w+)(?:\s+PUBLIC\s+("[^"]+"|'[^']+'))(?:\s+("[^"]+"|'[^']+'))\s*>/i
	var match = text.match(exp);
	if(match){
		var a = match[0];
		var end = start + a.length;
		if(a == text.substring(start,end)){
			doctype(text,start,match[1],match[2],match[3]);
			return end;
		}
	}
	var len = text.length;
	var dep = 0;
	var i = start;
	while(i<len){
		var c = text.charAt(i++);
		switch(c){
		case '<':
			dep++;break;
		case '>':
			dep--;
			if(dep == 0){
				doctype(text,start,text.substring(start,i));
				return i;
			}
			break;
		case '"':
		case '\'':
			i = text.indexOf(c,i);
			if(i>0){
				start = i+1;
			}else{
				$log.error("DTD 申明异常",getPositon(text,start));
				return text.length;
			}
		}
	}
	return text.length;
}
function getPosition(text,end){
	var line = 0;
	var col;
	while(end--){
		if(line === 0){
			col++;
		}
		var c = text.charAt(end);
		if(c === '\n'){
			if(text.charAt(end-1) === '\r'){
				end--;
			}
			line++;
		}else if(c === '\n'){
			line++;
		}
	}
	return [line,col]
}