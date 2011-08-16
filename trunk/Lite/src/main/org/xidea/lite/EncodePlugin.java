package org.xidea.lite;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xidea.el.Expression;
import org.xidea.el.ValueStack;

/**
 * 自定义函数和扩展函数（Invocable接口类）
 * 
 * @author jindw
 */
public class EncodePlugin implements RuntimePlugin {
	private static Log log = LogFactory.getLog(EncodePlugin.class);
	private Expression el;

	public void initialize(Template template, Object[] children) {
		this.el = (Expression) ((Object[]) children[0])[1];
	}

	public void execute(ValueStack context, Writer out) {
		Object value = el.evaluate(context);
		try {
			if (value instanceof Number) {
				if (((Number) value).floatValue() == 0) {
					out.append('0');
					return;
				}
			}
			final String text = String.valueOf(value);
			
			for (int i = 0, len = text.length(); i < len; i++) {
				int c = text.charAt(i);
				switch (c) {
				case '<':
					out.write("&lt;");//60
					break;
				case '"':// 34
					out.write("&#34;");
					break;
				case '\'':// 39
					out.write("&#39;");
					break;
				case '&':// 38
					if(notEntity(text,i,len)){
						out.write("&amp;");
						break;
					}
				default:
					out.write(c);
				}
			}
		} catch (IOException e) {
			log.error(e);
		}
	}
	private boolean notEntity(String text, int i,int len) {
		int status = 0;
		while (++i < len) {
			int c = text.charAt(i);
			switch(status){
			case 0:
				if(c == '#'){
					//&#0x12df;
					//&#12343;
					status = 2;
				}else if(Character.isJavaIdentifierStart(c)&& c !='$'){
					status = 1;
				}else{
					return true;
				}
				break;
			case 1://实体有值&a
				if(!Character.isJavaIdentifierPart(c) && c !='$' || c == '.' || c == '-'){//有改进空间（xml entity支持 '.'）
					return c != ';';
				}
				break;
			case 2://字符引用&#
				if(c == 'x'){
					status = 21;
				}else if(c>='0' && c<='9'){
					status = 20;
				}else{
					return true;
				}
				break;
			case 20://十进制字符实体有值:&#1  &#01
				if(c>='0' && c<='9'){
				}else{
					return c != ';';
				}
				break;
			case 21://十六进制字符引用 &#x
				if(c>='0' && c<='9' || c>='a' && c<= 'f' || c>='A' && c<= 'F'){
					status = 22;
				}else{
					return true;
				}
				break;
			case 22://十六进制字符引用 &#xa
				if(c>='0' && c<='9' || c>='a' && c<= 'f' || c>='A' && c<= 'F'){
				}else{
					return c != ';';
				}
				break;
			default:
				return true;
			}
			
		}
		return true;
	}
}
