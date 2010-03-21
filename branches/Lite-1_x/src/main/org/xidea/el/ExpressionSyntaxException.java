package org.xidea.el;

public class ExpressionSyntaxException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ExpressionSyntaxException() {
		super();
	}

	public ExpressionSyntaxException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExpressionSyntaxException(String message) {
		super(message);
	}

	public ExpressionSyntaxException(Throwable cause) {
		super(cause);
	}


}
