package org.jside.webserver.test.java6;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class TestJava6Server {
	@Test
	public void testFile() throws Exception {
		URL url = this.getClass().getResource("%e4%b8%ad中文"+("&&")+"alpha.txt");
		System.out.println(url.toURI().getPath());
		System.out.println(url.toURI().getRawPath());
		System.out.println(url.getPath());
		System.out.println(url.getFile());
		URI uri = new File("/金大?%e4%b8%ad为").toURI().toURL().toURI();
		System.out.println(uri.toURL());
		System.out.println(uri.getRawPath());
		System.out.println(uri.getPath());
		System.out.println(uri.toURL().getPath());
		System.out.println(new URL("http://l/a?234").toURI().getPath());
//		
//		System.out.println(new File("//金?大为").toURI().toURL().toURI().getPath());
//		System.out.println(new File("//金大为?^_^").toURI());
//		System.out.println(new File("/金大为?^_^").toURI().getPath());
//		System.out.println(new File("金大为").toURI());
//		System.out.println(new File("金大为/?^_^").toURI().getPath());
	}
	@Test
		public void testJava6() throws Exception {
		HttpServerDemo.main(null);
		Thread.sleep(1000*60);
	}

}

class HttpServerDemo {
	public static void main(String[] args) throws IOException {
		InetSocketAddress addr = new InetSocketAddress(8080);
		HttpServer server = HttpServer.create(addr, 0);

		HttpContext context = server.createContext("/", new MyHandler());
		context.getFilters().add(new Filter(){
			@Override
			public String description() {
				return null;
			}

			@Override
			public void doFilter(HttpExchange arg0, Chain arg1)
					throws IOException {
				arg1.doFilter(arg0);
			}
		});
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
		System.out.println("Server is listening on port 8080");
	}
}

class MyHandler implements HttpHandler {
	public void handle(HttpExchange exchange) throws IOException {
		System.out.println(exchange.getRequestURI());
		System.out.println(exchange.getRequestURI().getScheme());
		String requestMethod = exchange.getRequestMethod();
		if (requestMethod.equalsIgnoreCase("GET")) {
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, 0);

			OutputStream responseBody = exchange.getResponseBody();
			Headers requestHeaders = exchange.getRequestHeaders();
			Set<String> keySet = requestHeaders.keySet();
			Iterator<String> iter = keySet.iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				List values = requestHeaders.get(key);
				String s = key + " = " + values.toString() + "\n";
				responseBody.write(s.getBytes());
			}
			responseBody.close();
		}
	}
}