package org.xidea.webwork;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.webwork.multipart.MultiPartRequest;
import org.xidea.webwork.multipart.MultiPartRequestWrapper;

import com.opensymphony.xwork.ActionContext;
import com.opensymphony.xwork.ActionInvocation;
import com.opensymphony.xwork.ActionProxy;
import com.opensymphony.xwork.ActionProxyFactory;
import com.opensymphony.xwork.XworkException;
import com.opensymphony.xwork.config.ConfigurationManager;

/**
 * <p>
 * 创建webwork上下文  
 * </p>
 * 
 * @author Jindw
 * @version 1.0
 * @spring.bean id="webworkContext" init-method="initialize"
 */
public class WebworkContext {

	private static final Log log = LogFactory.getLog(WebworkContext.class);

	private static final String WEBWORK_VALUESTACK_KEY = WebworkContext.class.getName()+".VS";

	/**
	 * web上下文信息
	 */
	protected Map<Object,Object> defaultContextMap;

	/**
	 * Action 集合 [Action Name,List[namespace(order desc)]]
	 */
	protected Map<String,List<String>> actionMap;

	protected String encoding;

	protected Locale locale;

	protected int maxFileSize = 1024;

	protected String tempFileDir = ".";


	/**
	 * @param request
	 * @param response
	 * @param namespace
	 * @param actionName
	 * @throws Exception
	 *             if an error occurs when executing the filter.
	 */
	public void execute(String namespace, String actionName,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("初始化ActionContext");
		}
		if (encoding != null) {
			try {
				request.setCharacterEncoding(encoding);
			} catch (Exception e) {
			}
		}
		if (locale != null) {
			response.setLocale(locale);
		}

		HashMap<Object,Object> extraContext = new HashMap<Object,Object>(defaultContextMap);
		request = wrapperRequest(request);
		extraContext.put(HttpServletRequest.class, request);
		extraContext.put(HttpServletResponse.class, response);
		extraContext.put(ActionContext.PARAMETERS, request.getParameterMap());
		extraContext.put(ActionContext.SESSION, new SessionMap(request));
		//extraContext.put(WebWorkStatics.SERVLET_CONFIG, null);
		ActionProxy proxy = ActionProxyFactory.getFactory().createActionProxy(
				namespace, actionName, extraContext);

		Object action = proxy.getAction();
		if (action == null) {
			throw new XworkException("Action not found:" + namespace + "/"
					+ actionName);
		}
		try {
			ActionInvocation inv = proxy.getInvocation();
			Locale locale = response.getLocale();
			if (locale != null) {
				inv.getInvocationContext().setLocale(locale);
			}
			request.setAttribute(WEBWORK_VALUESTACK_KEY,
					inv.getStack());
			if (log.isDebugEnabled()) {
				log.debug("开始执行Action");
			}
			proxy.execute();
		} catch (Exception e) {
			log.error("执行Action失败", e);
			throw e;
		}

	}

	/**
	 * 获取Action�?��能的命名空间
	 * 
	 * @param action
	 *            Action�?
	 * @param desiredNamespace
	 *            渴望的命名空�?
	 * @return
	 */
	protected String getNamespace(String action, String desiredNamespace) {
		List<String> list = (List<String>) actionMap.get(action);
		String result = null;
		if (list != null) {
			for (Iterator<String> it = list.iterator(); it.hasNext();) {
				String ns = it.next();
				if (desiredNamespace.startsWith(ns)) {
					if(result == null){
						result = ns;
					}else if(ns.length()>result.length()){
						result = ns;
					}
				}
			}
		}
		return result;
	}

	protected HttpServletRequest wrapperRequest(HttpServletRequest request)
			throws IOException {
		// don't wrap more than once
		if (request instanceof WebworkRequestWrapper) {
			return request;
		}
		if (MultiPartRequest.isMultiPart(request)) {
			request = new MultiPartRequestWrapper(request, tempFileDir,
					maxFileSize);
		}else{
			request = new WebworkRequestWrapper(request);
		}
		return request;
	}

	@SuppressWarnings("unchecked")
	protected void initializeActionMap() {

		Map<String, Object> configs = ConfigurationManager.getConfiguration()
				.getRuntimeConfiguration().getActionConfigs();
		HashMap<String,List<String>> actionMap = new HashMap<String,List<String>>();
		List<String> spaceList = new ArrayList<String>();

		// 排列空间 /ns1/ns11,/ns1,/
		for (Iterator<String> it = configs.keySet().iterator(); it.hasNext();) {
			String currentSpace =  it.next();
			int i = 0;
			for (; i < spaceList.size(); i++) {
				String listSpace = (String) spaceList.get(i);
				if (listSpace.startsWith(currentSpace)) {
					spaceList.add(i, currentSpace);
					break;
				}
			}
			if (i == spaceList.size()) {
				spaceList.add(currentSpace);
			}
			Collections.reverse(spaceList);
		}
		for (Iterator<String> it = spaceList.iterator(); it.hasNext();) {
			String namespace = (String) it.next();
			Map actionConfigs = (Map) configs.get(namespace);
			for (Iterator<Map.Entry<String,Object>> configIterator = actionConfigs.entrySet().iterator(); configIterator
					.hasNext();) {
				Map.Entry<String,Object> entry2 = configIterator.next();
				String actionName = (String) entry2.getKey();
				List<String> namespaceList = (List<String>) actionMap.get(actionName);
				if (namespaceList == null) {
					namespaceList = new ArrayList<String>();
					actionMap.put(actionName, namespaceList);
				}
				namespaceList.add(namespace);
			}
		}
		this.actionMap = actionMap;
	}

	/**
	 * 初始化webwork
	 */
	public void initialize(ServletContext context) {
		this.defaultContextMap = new HashMap<Object, Object>();
		defaultContextMap.put(ActionContext.APPLICATION, new ApplicationMap(
				context));
		initializeActionMap();
	}

	/**
	 * Does nothing.
	 */
	public void destroy() {
	}


	/**
	 * @param encoding
	 *            The encoding to set.
	 * @spring.property ref="utf-8"
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	/**
	 * @param locale
	 *            The locale to set.
	 * @spring.property value="zh_CN"
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setMaxFileSize(int maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	public void setTempFileDir(String tempFileDir) {
		this.tempFileDir = tempFileDir;
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(Class<T> type) {
		return (T) ActionContext.getContext().get(type);
	}

}
