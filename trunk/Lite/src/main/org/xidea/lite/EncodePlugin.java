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
public class EncodePlugin implements Plugin {
	private static Log log = LogFactory.getLog(EncodePlugin.class);
	private Expression el;
	
	public void initialize(Template template, Object[] children) {
		this.el = (Expression) ((Object[])children[0])[1];
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
			String text = String.valueOf(value);
			for (int i = 0,len= text.length(); i <len; i++) {
			int c = text.charAt(i);
			switch (c) {
			case '<':
				out.write("&lt;");
				break;
			case '"':// 34
				out.write("&#34;");
				break;
			case '\'':// 39
				out.write("&#39;");
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
