package org.jside.server;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import org.jside.webserver.MutiThreadWebServer;
import org.jside.webserver.RequestContext;


public class FileServer extends MutiThreadWebServer{

	public FileServer(){
		super("file:///");
	}

	@Override
	public void processRequest(RequestContext context) throws Exception {
		URL res = new URL(webBase, context.getRequestURI()
				.substring(1));
		if(res.getProtocol().equals("file")){
			File file = getFile(res);
			if(file.isDirectory()){
				
			}else{
				super.processRequest(context);
			}
		}else{
			super.processRequest(context);
		}
	}

	private File getFile(URL res) {
		try {
			return  new File(URLDecoder.decode(res.getFile(),"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
}
