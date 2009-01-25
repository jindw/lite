package org.xidea.el.parser;

import java.util.AbstractList;

/**
 * @author jindw
 */
public class Tokens extends AbstractList<Object> {
	private ExpressionToken[] data;

	public Tokens(ExpressionToken[] reversedToken) {
		this.data = reversedToken;
	}

	public Object get(int index) {
		return new Token(data[data.length - index - 1]);
	}

	public ExpressionToken[] getData() {
		return this.data;
	}

	public int size() {
		return data.length;
	}

}

class Token extends AbstractList<Object> {
	private ExpressionToken token;

	public Token(ExpressionToken token) {
		this.token = token;
	}

	@Override
	public Object get(int index) {
		int type = token.getType();
		if (index == 0) {
			return type;
		} else {
			Object param = token.getParam();
			if (type == ExpressionToken.VALUE_LAZY) {
				return new Tokens((ExpressionToken[]) param);
			}
			return param;
		}
	}

	@Override
	public int size() {
		int type = token.getType();
		if (type >= 3) {// op
			switch (type) {
			case ExpressionToken.OP_STATIC_GET_PROP:
			case ExpressionToken.OP_MAP_PUSH:
				return 2;
			}

		} else {
			switch (type) {
			case ExpressionToken.VALUE_NEW_LIST:
			case ExpressionToken.VALUE_NEW_MAP:
				return 1;
			default:
				return 2;
			}
		}
		return 1;
	}
}