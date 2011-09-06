package org.xidea.lite.test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;

import org.w3c.dom.Document;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.json.JSONDecoder;
import org.xidea.lite.Template;
import org.xidea.lite.impl.ParseConfigImpl;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseContext;
import org.xml.sax.SAXException;

public class LiteTest {
	public static String testTemplate(Map<String,String>relativeSourceMap,Object context,String path) throws IOException, SAXException{
		ParseContext pc = buildContext(relativeSourceMap, path);
		Template tpl  = new Template(pc.toList(), pc.getFeatureMap());
		if(context instanceof String){
			context = ExpressionFactory.getInstance().create((String)context).evaluate(relativeSourceMap);
		}
		StringWriter out = new StringWriter();
		tpl.render(context, out);
		String javaresult = out.toString();
		return javaresult;
	}

	private static ParseContext buildContext(Map<String, String> relativeSourceMap, String path) throws SAXException, IOException {
		ParseConfigMock config = new ParseConfigMock(relativeSourceMap);
		ParseContextImpl pc = new ParseContextImpl(config, path);
		URI uri = pc.createURI(path);
		Document xml = pc.loadXML(uri);
		pc.parse(xml);
		return pc;
	}
	
	private static class ParseConfigMock extends ParseConfigImpl{
		private Map<String, String> sourceMap;

		public ParseConfigMock(Map<String, String> sourceMap) {
			super(URI.create("test:///"), null);
			this.sourceMap = sourceMap;
		}
		public Document loadXML(URI uri) throws IOException, SAXException{
			String source = this.sourceMap.get(uri.getPath());
			String id = uri.toString();
			if(source == null){
				System.out.println(id);
				System.out.println(uri.getPath());
				System.out.println(this.sourceMap.keySet());
			}
			source = ParseUtil.normalize(source, id);
			return ParseUtil.loadXMLBySource(source, id);
		}
	}
}
