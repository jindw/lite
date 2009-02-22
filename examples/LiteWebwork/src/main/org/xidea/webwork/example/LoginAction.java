/**
 * 
 */
package org.xidea.webwork.example;

import com.opensymphony.xwork.Action;
import com.opensymphony.xwork.ActionSupport;

/**
 * @xwork.package name="user" namespace="/user" extends="default"
 */
public class LoginAction extends ActionSupport {

	private static final long serialVersionUID = -7706253116934971948L;

	private String username = "test";
	private String password = "test";

	/**
	 * @xwork.action
	 * @xwork.interceptor-ref name = "spring-stack"
	 * @xwork.result name = "success" value= "login.xhtml"
	 */
	public String execute() {
		if (username.equals(password)) {
			return Action.SUCCESS;
		} else {
			this.addActionError("登陸失敗（測試用戶名與密碼相同！！）");
			this.addActionMessage("登陸失敗（測試用戶名與密碼相同！！）");
			return Action.ERROR;
		}
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
