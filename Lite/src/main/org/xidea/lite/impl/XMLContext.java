package org.xidea.lite.impl;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.parse.ParseContext;

class XMLContext {

	@SuppressWarnings("unused")
	private static Log log = LogFactory.getLog(XMLContext.class);
	private static Object CONTEXT_KEY = new Object();
	static XMLContext get(ParseContext context){
		XMLContext xc = context.getAttribute(CONTEXT_KEY);
		if(xc == null){
			xc = new XMLContext(context);
			context.setAttribute(CONTEXT_KEY, xc);
		}
		return xc;
	}

	private ArrayList<Boolean> indentStatus = new ArrayList<Boolean>();
	private int depth = 0;
	private boolean reserveSpace;
	private boolean format = false;
	private boolean compress = true;
	private final ParseContext context;


	public XMLContext(ParseContext context) {
		this.context = context;
	}


	public boolean isFormat() {
		return format;
	}

	public void setFormat(boolean format) {
		this.format = format;
	}

	public boolean isCompress() {
		return compress;
	}

	public void setCompress(boolean compress) {
		this.compress = compress;
	}

	public boolean isReserveSpace() {
		return reserveSpace;
	}

	public void setReserveSpace(boolean keepSpace) {
		this.reserveSpace = keepSpace;
	}

	public void beginIndent() {// boolean needClose) {
		int size = indentStatus.size();
		printIndent();
		depth++;
		switch (depth - size) {
		case 1:
			indentStatus.add(null);
		case 0:
			indentStatus.add(null);
			// case -1:
		default:
			indentStatus.set(depth - 1, true);
			indentStatus.set(depth, false);
		}
	}

	public void endIndent() {
		if (Boolean.TRUE.equals(indentStatus.get(depth))) {
			depth--;
			printIndent();
		} else {
			depth--;
		}

	}

	private void printIndent() {
		if (!this.isCompress() && !this.isReserveSpace() && this.isFormat()) {
			int i = context.mark();
			if (i > 0) {
				context.append("\r\n");
			}
			int depth = this.depth;
			if (depth > 0) {
				char[] data = new char[depth];
				while (depth-- > 0) {
					data[depth] = ' ';
				}
				context.append(new String(data));
			}

		}
	}

}
