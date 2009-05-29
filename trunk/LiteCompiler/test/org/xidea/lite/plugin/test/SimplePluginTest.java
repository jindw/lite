package org.xidea.lite.plugin.test;

import java.beans.XMLDecoder;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Template;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.impl.ParseContextImpl;
import org.xidea.lite.plugin.PluginLoader;

public class SimplePluginTest {

	private PluginLoader pf;
	private Map<String, Object> context;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSimplePluginTest() throws Exception {
		runTest("SimplePluginTest");
		
	}

	@SuppressWarnings("unchecked")
	public void runTest(String file) throws Exception {
		XMLDecoder de = new XMLDecoder(this.getClass().getResourceAsStream(
				file + ".xml"));
		String plugin = (String)de.readObject();
		pf = new PluginLoader();
		pf.load(plugin);
		this.context = (Map<String, Object>) de.readObject();
		System.out.println(JSONEncoder.encode(this.context));
		Map<String, String> templateResultMap = (Map<String, String>) de.readObject();
		int i=0;
		for (String key : templateResultMap.keySet()) {
			String value = templateResultMap.get(key);
			test(i++,key, value);
		}
	}

	@SuppressWarnings("unchecked")
	public void test(int index,String text, String result) throws Exception {

		ParseContextImpl parseContext = new ParseContextImpl(this.getClass()
				.getResource("/"),null,null,null);
		for(InstructionParser iparser:pf.getInstructionParserList()){
			parseContext.addInstructionParser(iparser);
		}
		parseContext.parse(text);
		List<Object> insts = parseContext.toList();
		System.out.println(JSONEncoder.encode(insts));
		Template t = new Template(insts);
		Writer out = new StringWriter();
		t.render(new HashMap(context), out);
		Assert.assertEquals("第"+index+"个测试错误："+text+":\n"+result,result, out.toString());
	}
}
