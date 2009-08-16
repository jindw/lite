package org.jside.webserver;

import android.util.Log;


public class LogFactory implements org.apache.commons.logging.Log{

	private String tag;
	

	public static org.apache.commons.logging.Log getLog(Class<? extends Object> type) {
		return new LogFactory(type.getName());
	}
	public LogFactory(String type) {
		tag = type;
	}




	@Override
	public void debug(Object msg) {
		Log.d(tag, String.valueOf(msg));
	}

	@Override
	public void debug(Object msg, Throwable e) {
		Log.d(tag, String.valueOf(msg),e);
		
	}

	@Override
	public void error(Object msg) {
		Log.e(tag, String.valueOf(msg));
		
	}

	@Override
	public void error(Object msg, Throwable e) {
		Log.e(tag, String.valueOf(msg),e);
		
	}

	@Override
	public void fatal(Object msg) {
		Log.println(Log.ASSERT,tag, String.valueOf(msg));
		
	}

	@Override
	public void fatal(Object msg, Throwable e) {
		Log.println(Log.ASSERT,tag, String.valueOf(msg)+Log.getStackTraceString(e));
	}

	@Override
	public void info(Object msg) {
		Log.i(tag, String.valueOf(msg));
		
	}

	@Override
	public void info(Object msg, Throwable e) {
		Log.i(tag, String.valueOf(msg),e);
	}

	@Override
	public boolean isDebugEnabled() {
		return Log.isLoggable(tag, Log.DEBUG);
	}

	@Override
	public boolean isErrorEnabled() {
		return Log.isLoggable(tag, Log.ERROR);
	}

	@Override
	public boolean isFatalEnabled() {
		return Log.isLoggable(tag, Log.ASSERT);
	}

	@Override
	public boolean isInfoEnabled() {
		return Log.isLoggable(tag, Log.INFO);
	}

	@Override
	public boolean isTraceEnabled() {
		return Log.isLoggable(tag, Log.VERBOSE);
	}

	@Override
	public boolean isWarnEnabled() {
		return Log.isLoggable(tag, Log.WARN);
	}

	@Override
	public void trace(Object msg) {
		Log.v(tag, String.valueOf(msg));
	}

	@Override
	public void trace(Object msg, Throwable e) {
		Log.v(tag, String.valueOf(msg),e);
		
	}

	@Override
	public void warn(Object msg) {
		Log.w(tag, String.valueOf(msg));
		
	}

	@Override
	public void warn(Object msg, Throwable e) {
		Log.w(tag, String.valueOf(msg),e);
		
	}

}
