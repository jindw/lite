package org.jside.server;

import org.jside.webserver.MutiThreadWebServer;


public class FileServer extends MutiThreadWebServer{

	public FileServer(){
		super("file:///");
	}
}
