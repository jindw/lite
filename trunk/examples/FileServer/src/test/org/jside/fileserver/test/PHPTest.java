package org.jside.fileserver.test;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.jside.webserver.CGIEnvironment;
import org.jside.webserver.CGIRunner;
import org.jside.webserver.MutiThreadWebServer;
import org.jside.webserver.RequestContext;
import org.jside.webserver.RequestUtil;

public class PHPTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final URI base = new File("../Lite2/web").toURI();
		MutiThreadWebServer mtws = new MutiThreadWebServer(base){
			public void processRequest(RequestContext context) throws Exception {
				String uri = context.getRequestURI();
				String rp = CGIEnvironment.toRealPath(base, uri);
				if(rp.endsWith(".php") ){
					Map<String, String> envp= new CGIEnvironment(context).toMap(null);
					CGIRunner cr = new CGIRunner(context, "php-cgi.exe", 
							envp,
							new File(new File(base),rp).getParentFile()
					, null);
					cr.run();
				}else{
					RequestUtil.printResource();
				}
			}
		};
		mtws.start();
	}

}
