package org.jside.webserver.action;

import org.jside.webserver.RequestContext;

public interface ActionInvocation {
	public boolean match(String uri);
	public void execute(RequestContext context) throws Exception;
}

