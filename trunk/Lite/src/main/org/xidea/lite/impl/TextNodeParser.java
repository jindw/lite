package org.xidea.lite.impl;


import org.xidea.lite.Template;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.parse.TextParser;

public class TextNodeParser implements NodeParser<String> {


	public TextNodeParser() {
	}

	public void parse(String text, ParseContext context, ParseChain chain) {
		final boolean encode;
		final char qute;
		switch (context.getTextType()) {
		case Template.XA_TYPE:
			encode = true;
			qute = '"';
			break;
		case Template.XT_TYPE:
			encode = true;
			qute = 0;
			break;
		default:
			encode = false;
			qute = 0;
		}
		parse(context, text, encode, qute);
	}

	/**
	 * 解析指定文本
	 * 
	 * @public
	 * @abstract
	 * @return <Array> result
	 */
	protected void parse(ParseContext context, final String text,
			final boolean encode, final char qute) {
		//尾部优先
		TextParser[] textParsers = context.getTextParsers();
		final int length = text.length();
		int start = 0;
		do {
			TextParser nip = null;
			int p$ = length + 1;
			{
				int pri = 0;
				for (int i = textParsers.length-1;i>=0;) {
					TextParser  ip = textParsers[i--];
					int p$2 = ip.findStart(text, start, p$);
					int pri2 = ip.getPriority();
					if (p$2 >= start ){
						if(p$2 < p$ || p$2 == p$ && pri2>pri){
							p$ = p$2;
							nip = ip;
							pri = pri2;
						}
					}
					
				}
			}
			if (nip != null) {
				int escapeCount = countEescape(text, p$);
				String t = text.substring(start, p$ - (escapeCount + 1) / 2);
				appendText(context,t,encode,qute);
				if ((escapeCount & 1) == 1) {// escapsed
					start = nextPosition(context, text, p$);
				} else {
					start = p$;
					int mark = context.mark();
					try {
						start = nip.parseText(text, start, context);
					} catch (Exception e) {
					}
					if (start <= p$) {
						context.reset(mark);
						start = nextPosition(context, text, p$);
					}

				}
			} else {
				break;
			}
		} while (start < length);
		if (start < length) {
			appendText(context,text.substring(start),encode,  qute);
		}
	}

	public void appendText(ParseContext context,String text,boolean encode, char quteChar) {
		if(encode){
			text = encodeText(text, quteChar);
		}
		context.append(text);
	}

	private String encodeText(String text, int quteChar) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			int c = text.charAt(i);
			switch (c) {
			case '<':
				out.append("&lt;");
				break;
			case '>':
				out.append("&gt;");
				break;
			case '&':
				out.append("&amp;");
				break;
			case '\'':
			case '"':
				if (quteChar == c) {
					out.append("&#");
					out.append(c);
					out.append(';');
				} else {
					out.append((char)c);
				}
				break;
			default:
				out.append((char)c);
			}
		}
		return out.toString();
	}

	protected int nextPosition(ParseContext context, final String text, int p$) {
		context.append(text.substring(p$, p$ + 1));
		return p$ + 1;
	}

	protected int countEescape(final String text, int p$) {
		if (p$ > 0 && text.charAt(p$ - 1) == '\\') {
			int pre = p$ - 1;
			while (pre-- > 0 && text.charAt(pre) == '\\')
				;
			return p$ - pre - 1;
		}
		return 0;
	}
}
