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
		case Template.XML_ATTRIBUTE_TYPE:
			encode = true;
			qute = '"';
			break;
		case Template.XML_TEXT_TYPE:
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
		TextParser[] textParsers = context.getTextParsers();
		final int length = text.length();
		int start = 0;
		do {
			TextParser nip = null;
			int p$ = length + 1;
			{
				int pri = 0;
				for (TextParser ip : textParsers) {
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
				context.append(text
						.substring(start, p$ - (escapeCount + 1) / 2), encode,
						qute);
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
			context.append(text.substring(start), encode, qute);
		}
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
