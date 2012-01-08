package org.xidea.lite.test;

import java.net.URI;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xidea.lite.impl.ParseConfigImpl;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.ParseUtil;

public class SimpleTest {
	@Test
	public void test() throws Exception{
		URI root = this.getClass().getResource("/").toURI().resolve("../../");
		Document xml = ParseUtil.loadXML(root.resolve("WEB-INF/lite.xml").toString());
		ParseConfigImpl pc = new ParseConfigImpl(root,root.resolve("WEB-INF/lite.xml"));
		String path = "/doc/guide/el-op.xhtml";
		ParseContextImpl c = new ParseContextImpl(pc, path);
		c.parse(c.createURI(path));;
	}

}
