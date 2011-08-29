package org.xidea.lite.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


interface DebugSupport {

	/**
	 * refresh model model,url source
	 */
	String LITE_DEBUG = "LITE_DEBUG";
	String LITE_SERVICE = "LITE_SERVICE";
	String DATA_VIEW_JS_PATH = "/scripts/data-view.js";
	String PARAM_LITE_PATH = "LITE_PATH";
	String PARAM_LITE_DATA = "LITE_DATA";
	String PARAM_LITE_ACTION = "LITE_ACTION";
	String PARAM_LITE_CALLBACK = "LITE_CALLBACK";
	String PARAM_LITE_ACTION_LOAD = "load";
	String PARAM_LITE_ACTION_SAVE = "save";
	/**
	 * 
	 * @param path
	 * @param request
	 * @param response
	 * @return complete request
	 * @throws IOException
	 */
	boolean debug(String path, HttpServletRequest request,HttpServletResponse response) throws IOException;
	boolean service(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
