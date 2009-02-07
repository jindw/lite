package org.xidea.el.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.junit.Test;

@SuppressWarnings({"unused","unchecked"})
public class ClasspathTest {

	static String getText(String file) {
		try {
			InputStreamReader in = new InputStreamReader(ClasspathTest.class
					.getResourceAsStream(file), "utf8");
			char[] buf = new char[1024];
			StringWriter out = new StringWriter();
			int len;
			while ((len = in.read(buf)) >= 0) {
				out.write(buf, 0, len);
			}
			return out.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	@Test
	public void testURL() throws Exception{
		String cp = "classpath:///org/xidea/el/test/test.json";
		URL parent = new URL("http://www.x.y");
		URL url = new URL(parent,cp,new URLStreamHandler(){
			@Override
			protected URLConnection openConnection(URL u) throws IOException {
				System.out.println(u.getFile());
				return this.getClass().getClassLoader().getResource(u.getFile().substring(1)).openConnection();
			}
			
		});
		System.out.println(url);
		InputStream in = url.openStream();
	}
}
