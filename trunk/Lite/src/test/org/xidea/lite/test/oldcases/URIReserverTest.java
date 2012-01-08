package org.xidea.lite.test.oldcases;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import org.junit.Test;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONEncoder;

public class URIReserverTest {
	@Test
	public void testClassLoader() throws Exception{
		cl();
		new Thread(){
			public void run(){
				cl();
				new Thread(){
					public void run(){
						cl();
					}
				}.start();
			}
		}.start();
	}
	private void cl() {
		System.out.println(Thread.currentThread().getContextClassLoader());
	}
	@Test
	public void testURIBean(){
		test(URI.create("lite:/").resolve("/"));
		test(URI.create("lite:///").resolve("/"));
		test(URI.create("lite:///1/2/").resolve("/a"));
		test(URI.create("lite:///1/2/").resolve("a"));
		test(URI.create("lite:///1/2/").resolve("//a"));
	}
	private void test(URI uri){
		System.out.println(uri);
		System.out.println(JSONEncoder.encode(uri));
	}
	@Test
	public void testURI() throws Exception{
		URI uri = new URI("classpath:com/myapp/config.xml");
		System.out.println((uri));
		System.out.println(ReflectUtil.map(uri));
		URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory(){
			public URLStreamHandler createURLStreamHandler(String protocol) {
				if(protocol.equals("classpath")){
				return new URLStreamHandler(){
					@Override
					protected URLConnection openConnection(URL u)
							throws IOException {
						return this.getClass().getResource("/"+u.getPath()).openConnection();
					}
					
				};
				}else{
					return null;
				}
			}
		});
		URL url1 = uri.toURL();
		URL url2 = new URL("classpath:com/myapp/config.xml");
		System.out.println(ReflectUtil.map(url1));
		System.out.println(ReflectUtil.map(url2));
		System.out.println(url1.equals(url2));
		
	}

}
