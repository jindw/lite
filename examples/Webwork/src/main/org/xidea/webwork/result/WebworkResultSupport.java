/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package org.xidea.webwork.result;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.opensymphony.xwork.ActionInvocation;
import com.opensymphony.xwork.Result;
import com.opensymphony.xwork.util.TextParseUtil;

/**
 * <!-- START SNIPPET: javadoc -->
 * 
 * A base class for all WebWork action execution results. The "location" param
 * is the default parameter, meaning the most common usage of this result would
 * be: <p/> This class provides two common parameters for any subclass:
 * <ul>
 * <li>location - the location to go to after execution (could be a jsp page or
 * another action). It can be parsed as per the rules definied in the
 * {@link TextParseUtil#translateVariables(java.lang.String, com.opensymphony.xwork.util.OgnlValueStack) translateVariables}
 * method</li>
 * <li>parse - true by default. If set to false, the location param will not be
 * parsed for expressions</li>
 * <li>encode - false by default. If set to false, the location param will not
 * be url encoded. This only have effect when parse is true</li>
 * </ul>
 * 
 * <b>NOTE:</b> The encode param will only have effect when parse is true
 * 
 * <p/>
 * 
 * <!-- END SNIPPET: javadoc -->
 * 
 * 
 * <!-- START SNIPPET: example -->
 * 
 * <p/> In the xwork.xml configuration file, these would be included as: <p/>
 * 
 * <pre>
 *  &lt;result name=&quot;success&quot; type=&quot;redirect&quot;&gt;
 *      &lt;param name=&quot;&lt;b&gt;location&lt;/b&gt;&quot;&gt;foo.jsp&lt;/param&gt;
 *  &lt;/result&gt;
 * </pre>
 * 
 * <p/> or <p/>
 * 
 * <pre>
 *  &lt;result name=&quot;success&quot; type=&quot;redirect&quot; &gt;
 *      &lt;param name=&quot;&lt;b&gt;location&lt;/b&gt;&quot;&gt;foo.jsp?url=${myUrl}&lt;/param&gt;
 *      &lt;param name=&quot;&lt;b&gt;parse&lt;/b&gt;&quot;&gt;true&lt;/param&gt;
 *      &lt;param name=&quot;&lt;b&gt;encode&lt;/b&gt;&quot;&gt;true&lt;/param&gt;
 *  &lt;/result&gt;
 * </pre>
 * 
 * <p/> In the above case, myUrl will be parsed against Ognl Value Stack and
 * then URL encoded. <p/> or when using the default parameter feature <p/>
 * 
 * <pre>
 *  &lt;result name=&quot;success&quot; type=&quot;redirect&quot;&gt;&lt;b&gt;foo.jsp&lt;/b&gt;&lt;/result&gt;
 * </pre>
 * 
 * <p/> You should subclass this class if you're interested in adding more
 * parameters or functionality to your Result. If you do subclass this class you
 * will need to override {@link #doExecute(String, ActionInvocation)}.
 * <p>
 * <p/> Any custom result can be defined in xwork.xml as: <p/>
 * 
 * <pre>
 *  &lt;result-types&gt;
 *      ...
 *      &lt;result-type name=&quot;myresult&quot; class=&quot;com.foo.MyResult&quot; /&gt;
 *  &lt;/result-types&gt;
 * </pre>
 * 
 * <p/> Please see the {@link com.opensymphony.xwork.Result} class for more info
 * on Results in general.
 * 
 * <!-- END SNIPPET: example -->
 * 
 * @author Jason Carreira
 * @author Bill Lynch (docs)
 * @author tm_jee
 * @see com.opensymphony.xwork.Result
 */
public abstract class WebworkResultSupport implements Result {

	private static final Log _log = LogFactory
			.getLog(WebworkResultSupport.class);

	public static final String DEFAULT_PARAM = "location";

	protected String location;
	protected boolean containsParam = false;
	
	protected boolean encode = false;
	protected String encoding = "UTF-8";

	private TextParseUtil.ParsedValueEvaluator urlEncoderEvaluator = new TextParseUtil.ParsedValueEvaluator() {
		public Object evaluate(Object parsedValue) {
			if (parsedValue != null) {
				try {
					return URLEncoder.encode(parsedValue.toString(), encoding );
				} catch (UnsupportedEncodingException e) {
					_log.warn("error while trying to encode [" + parsedValue
							+ "]", e);
				}
			}
			return parsedValue;
		}
	};
	/**
	 * The location to go to after action execution. This could be a JSP page or
	 * another action. The location can contain OGNL expressions which will be
	 * evaulated if the <tt>parse</tt> parameter is set to <tt>true</tt>.
	 * 
	 * @param location
	 *            the location to go to after action execution.
	 * @see #setParse(boolean)
	 */
	public void setLocation(String location) {
		containsParam = location.indexOf("${") >= 0;
		this.location = location;
	}

	/**
	 * Set encode to <tt>true</tt> to indicate that the location should be url
	 * encoded. This is set to <tt>true</tt> by default
	 * 
	 * @param encode
	 *            <tt>true</tt> if the location parameter should be url
	 *            encode, <tt>false</tt> otherwise.
	 */
	public void setEncode(boolean encode) {
		this.encode = encode;
	}

	/**
	 * Implementation of the <tt>execute</tt> method from the <tt>Result</tt>
	 * interface. This will call the abstract method
	 * {@link #doExecute(String, ActionInvocation)} after optionally evaluating
	 * the location as an OGNL evaluation.
	 * 
	 * @param invocation
	 *            the execution state of the action.
	 * @throws Exception
	 *             if an error occurs while executing the result.
	 */
	public abstract void execute(ActionInvocation invocation) throws Exception;
	// doExecute(parseParams(location, invocation), invocation);

	protected String parseParams(String location, ActionInvocation invocation) {
		if (this.containsParam && location != null) {
			return TextParseUtil.translateVariables(location, invocation
					.getStack(), urlEncoderEvaluator);
		} else {
			return location;
		}
	}

}
