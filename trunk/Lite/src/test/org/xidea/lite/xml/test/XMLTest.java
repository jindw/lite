package org.xidea.lite.xml.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.JSIText;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.impl.ParseUtil;

public class XMLTest {
	JSIRuntime jsr = RuntimeSupport.create();
	@Test
	public void test() throws Exception, IOException{
		String source = ParseUtil.loadTextAndClose(new FileInputStream("D:\\workspace\\FireSite\\web\\index.xhtml"), "utf-8");
		Document d1 = ParseUtil.loadXMLBySource(source, "");
		source = ParseUtil.normalize(source, "");
		jsr.eval("$import('org.xidea.lite.nodejs:DOMParser')");
		Object d2 = jsr.invoke(jsr.eval("new DOMParser()"), "parseFromString", source);

		Object r1 = jsr.invoke(jsr.eval("new DOMParser()"), "test", d1);
		Object r2 = jsr.invoke(jsr.eval("new DOMParser()"), "test", d2);
		System.out.println(r1);
		System.out.println(r2);
		
		r1 = jsr.invoke(jsr.eval("new DOMParser()"), "test", d1);
		r2 = jsr.invoke(jsr.eval("new DOMParser()"), "test", d2);

		System.out.println(r1);
		System.out.println(r2);
	}

}
