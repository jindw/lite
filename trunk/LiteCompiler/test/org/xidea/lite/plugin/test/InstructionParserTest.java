package org.xidea.lite.plugin.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.Parser;
import org.xidea.lite.parser.impl.ParseContextImpl;
import org.xidea.lite.plugin.PluginLoader;

public class InstructionParserTest {

	private PluginLoader pf;
	private ParseContextImpl context;

	@Before
	public void setUp() throws Exception {
		pf = new PluginLoader();
		context = new ParseContextImpl(new URL("http://x/"));
	}

	@Test
	public void testCreateInstructionParser() throws IOException {
		pf.load(new InputStreamReader(this
				.getClass().getResourceAsStream("InstructionParserTest.js"),"utf-8"));
		InstructionParser ip = pf.getInstructionParserList().get(0);
		//Parser<Node> np = (Parser<Node>) pf.getNodeParserList().get(0);
		Assert.assertEquals("查找3的位置", 2, ip.findStart(context, "12345", 1));
		Assert.assertEquals("字符结束位置", 5, ip.parse(context, "12345", 1));
		Assert.assertEquals("字符1后内容", "2345", context.toResultTree().get(0));
	}

}
