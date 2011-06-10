package org.xidea.lite;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

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
	private static Pattern ENTRY = Pattern.compile("^&(?:#x[a-f\\d]+|#\\d+|[a-z]\\w*);$",Pattern.CASE_INSENSITIVE);//&#37329;&#x5927;&#X4e3a;
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
			
			outer:for (int i = 0, len = text.length(); i < len; i++) {
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
					int begin = i;
					int last = Math.min(len-1, i+10);//MAX_CODE_POINT:0x10ffff=>1114111//8
					while(begin++<last){
						c = text.charAt(begin);
						if(c == ';'){
							String e = text.substring(i,begin+1);
							if(ENTRY.matcher(e).find()){
								out.write('&');
								continue outer;
							}else{
								System.out.println(e);
								break;
							}
						}
					}
					out.write("&#38;");
					break;
				default:
					out.write(c);
				}
			}
		} catch (IOException e) {
			log.error(e);
		}
	}

}
