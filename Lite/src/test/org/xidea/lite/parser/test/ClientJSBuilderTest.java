package org.xidea.lite.parser.test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.lite.parser.ParseContext;
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
		List<Object> liteCode = context2.toResultTree();
		String result = new Java6JSBuilder().buildJS("test", liteCode);
		System.out.println("==JS Code==");
		System.out.println(result);
		boolean isError = Pattern.compile("[\r\n]alert",Pattern.MULTILINE).matcher(result).find();
		Assert.assertTrue("生成失败"+result,!isError);
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
		
		List<Object> clientLiteCode = context2.toResultTree();
		System.out.println("==JS Code==");
		System.out.println(clientLiteCode);
		String result = (String)clientLiteCode.get(0);
		boolean isError = Pattern.compile("[\r\n]alert",Pattern.MULTILINE).matcher(result).find();
		Assert.assertTrue("生成失败"+result,!isError);
	}

}
