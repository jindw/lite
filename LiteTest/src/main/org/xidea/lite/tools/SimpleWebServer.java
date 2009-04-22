package org.xidea.lite.tools;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xidea.el.json.JSONDecoder;
import org.xidea.lite.Template;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.parser.HTMLNodeParser;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.XMLParser;
import org.xidea.lite.tools.webserver.MutiThreadWebServer;
import org.xidea.lite.tools.webserver.RequestHandle;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SimpleWebServer extends MutiThreadWebServer {
	public static final String INDEX_XHTML = "index.xhtml";
	public static final String POST_FIX_XHTML = ".xhtml";
	protected File webBase;
	protected TemplateEngine engine;
	protected boolean compress;
	protected boolean format;
	protected boolean xhtml = true;
	protected long lastAcessTime = System.currentTimeMillis();

	XMLParser xmlParser = new XMLParser();
	XMLParser htmlParser = new XMLParser() {
		private org.cyberneko.html.parsers.DOMParser htmlParser = new org.cyberneko.html.parsers.DOMParser();
		{
			try {
				htmlParser.setFeature("http://xml.org/sax/features/namespaces",
						true);
				htmlParser
						.setFeature(
								"http://cyberneko.org/html/features/scanner/cdata-sections",
								true);
				htmlParser.setProperty(
						"http://cyberneko.org/html/properties/names/elems",
						"default");
				htmlParser.setProperty(
						"http://cyberneko.org/html/properties/names/attrs",
						"default");
				htmlParser
						.setProperty(
								"http://cyberneko.org/html/properties/default-encoding",
								"utf-8");
			} catch (Exception e) {
				e.printStackTrace();
			}

			this.addNodeParser(new HTMLNodeParser(this) {

				protected Node parse(Node node, ParseContext context) {
					if (node.getLocalName().equalsIgnoreCase("script")) {
						Node firstChild = node.getFirstChild();
						if (firstChild != null
								&& firstChild.getNextSibling() == null) {
							if (firstChild.getNodeType() == Node.TEXT_NODE) {
								String text = firstChild.getNodeValue().trim();
								text = text.replaceAll("&lt;", "<").replaceAll(
										"&gt;", ">").replaceAll("&amp;", "&");
								context.beginIndent();
								context.append("<script");
								parser.parseNode(node.getAttributes(), context);
								context.append(">");
								parser.parseText(context, text,
										Template.EL_TYPE);
								context.append("</script>");
								context.endIndent();
								return null;
							}
						}
					}
					return node;
				}
			});
		}

		public Document loadXML(URL url, ParseContext context)
				throws SAXException, IOException {
			context.setCurrentURL(url);
			InputSource in = new InputSource(url.openStream());
			synchronized (htmlParser) {
				htmlParser.parse(in);
				Document doc = htmlParser.getDocument();
				return doc;
			}
		}
	};

	public SimpleWebServer(File webBase) {
		reset(webBase);
	}

	public void reset(File webBase) {
		this.webBase = webBase;
		engine = new TemplateEngine(webBase) {
			{
				if (!xhtml) {
					this.parser = htmlParser;
				}

			}

			protected URL getResource(String pagePath)
					throws MalformedURLException {
				File file = new File(webRoot, pagePath);
				if (file.exists()) {
					return file.toURI().toURL();
				} else {
					return this.getClass().getResource(pagePath);
				}
			}

			@Override
			protected ParseContext createParseContext() {
				ParseContext context = super.createParseContext();
				context.setCompress(SimpleWebServer.this.compress);
				context.setFormat(SimpleWebServer.this.format);
				return context;
			}

		};
	}

	protected void processRequest(RequestHandle handle) throws IOException {
		lastAcessTime = System.currentTimeMillis();
		String url = handle.getRequestURI();
		File file = new File(webBase, url);
		if (file.exists()) {

			if (file.isDirectory()) {
				File index = new File(file, INDEX_XHTML);
				if (index.exists()) {
					url = url.substring(0, url.lastIndexOf('/') + 1)
							+ INDEX_XHTML;
					file = index;
				}
			}
			if (url.endsWith(POST_FIX_XHTML)) {
				Writer out = new StringWriter();
				File json = new File(webBase, url.substring(0, url
						.lastIndexOf('.'))
						+ ".json");
				Map<Object, Object> object = new HashMap<Object, Object>();
				if (json.exists()) {
					String text = loadText(new FileInputStream(json));
					if (text.startsWith("\uFEFF")) {
						text = text.substring(1);
					}
					try {
						object = (Map) JSONDecoder.decode(text);
					} catch (Exception e) {
						PrintWriter pout = new PrintWriter(out);
						pout.print("<!--");
						e.printStackTrace(pout);
						pout.print("-->");
						pout.close();
					}
				}
				object.put("requestURI", url);
				engine.render(url, object, out);
				handle.printContext(out, "text/html");
				return;
			}
			if (file.isDirectory() && !url.endsWith("/")) {
				handle.printRederect(url + '/');
			} else {
				handle.printFile(file);
			}
		} else {
			String text = loadText(this.getClass().getResourceAsStream(url));
			if (text == null) {
				System.out.println(url + "不存在");
			}
			handle.printContext(text, "text/html");
		}
	}

	public String loadText(InputStream ins) {
		try {
			Reader in = new InputStreamReader(ins, "utf-8");
			StringBuilder buf = new StringBuilder();
			char[] cbuf = new char[1024];
			for (int len = in.read(cbuf); len > 0; len = in.read(cbuf)) {
				buf.append(cbuf, 0, len);
			}
			return buf.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	protected void closeIfTimeout(int time) {
		long passed = System.currentTimeMillis() - this.lastAcessTime;
		if (passed > time) {
			System.out.println("timeout and close server:" + time);
			System.exit(1);
		} else {
			// System.out.println("wait time:" +passed);
		}
	}

	public static void main(String[] a) throws Exception {
		File root = new File(".");
		if (new File(root, "web/WEB-INF").exists()) {
			root = new File(root, "web");
		}
		final SimpleWebServer server = new SimpleWebServer(root
				.getAbsoluteFile());
		server.start();

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				server.closeIfTimeout(1000 * 60 * 5);
			}

		}, 1000 * 60, 1000 * 60);
		Desktop.getDesktop().browse(
				new URI("http://localhost:" + server.getPort()));
	}
}
