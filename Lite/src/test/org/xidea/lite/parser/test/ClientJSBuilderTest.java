package org.xidea.lite.parser.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.impl.ELParser;
import org.xidea.lite.parser.impl.Java6JSBuilder;
import org.xidea.lite.parser.impl.ParseContextImpl;
import org.xml.sax.SAXException;

public class ClientJSBuilderTest {
	private ExpressionFactory clientExpressionFactory = new ExpressionFactory() {
		public Expression create(Object el) {
			throw new UnsupportedOperationException();
		}

		public Object parse(String expression) {
			return expression;
		}

	};

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testBuildJS() throws SAXException, IOException {
		URL url = this.getClass().getResource("format-test.xhtml");
		ParseContext context2 = new ParseContextImpl(url);
		// 前端直接压缩吧？反正保留那些空白也没有调试价值
		// context2.setCompress(context.isCompress());
		context2.setCompress(true);
		context2.setExpressionFactory(clientExpressionFactory);
		context2.parse(context2.loadXML(url));
		List<Object> liteCode = context2.toList();
		String result = new Java6JSBuilder().buildJS(liteCode, "test");
		System.out.println("==JS Code==");
		System.out.println(result);
		boolean isError = Pattern.compile("[\r\n]alert", Pattern.MULTILINE)
				.matcher(result).find();
		Assert.assertTrue("生成失败" + result, !isError);
	}

	@Test
	public void testClient() throws SAXException, IOException {
		URL url = this.getClass().getResource("asciitable-client.xhtml");
		ParseContext context2 = new ParseContextImpl(url);
		// 前端直接压缩吧？反正保留那些空白也没有调试价值
		// context2.setCompress(context.isCompress());
		context2.setCompress(true);
		context2.setExpressionFactory(clientExpressionFactory);
		context2.parse(context2.loadXML(url));

		List<Object> clientLiteCode = context2.toList();
		System.out.println("==JS Code==");
		System.out.println(clientLiteCode);
		String result = (String) clientLiteCode.get(0);
		boolean isError = Pattern.compile("[\r\n]alert", Pattern.MULTILINE)
				.matcher(result).find();
		Assert.assertTrue("生成失败" + result, !isError);
	}

	@Test
	public void testCTClient() throws SAXException, IOException {
		InputStreamReader source = new InputStreamReader(this.getClass()
				.getResourceAsStream("ct-client.txt"), "utf-8");
		ParseContext context2 = new ParseContextImpl(new URL("http://w/"),
				null, null, new InstructionParser[] { ELParser.EL, ELParser.IF,
						ELParser.FOR, ELParser.ELSE, ELParser.CLIENT,
						ELParser.END, ELParser.VAR });
		// 前端直接压缩吧？反正保留那些空白也没有调试价值
		// context2.setCompress(context.isCompress());
		context2.parse(loadText(source));
		System.out.println(context2.toList());
	}

	public static String loadText(Reader in) {
		// Reader in = new InputStreamReader(sin, "utf-8");
		StringWriter out = new StringWriter();
		int count;
		char[] cbuf = new char[1024];
		try {
			while ((count = in.read(cbuf)) > -1) {
				out.write(cbuf, 0, count);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out.toString();
	}
}
