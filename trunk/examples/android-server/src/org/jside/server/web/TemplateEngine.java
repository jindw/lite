package org.jside.server.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import org.jside.server.FileServer;
import org.xidea.el.json.JSONDecoder;
import org.xidea.lite.Template;

public class TemplateEngine {
	public void render(String path, Object context, Writer out) {
		try {
			if(path.startsWith("/")){
				path = path.substring(1);
			}
			loadTemplate(path).render(context, out);
		} catch (IOException e) {
			e.printStackTrace();
			e.printStackTrace(new PrintWriter(out, true));
		}
	}

	public static Template loadTemplate(String path) throws IOException {
		InputStream in = TemplateEngine.class.getResourceAsStream(path);
		try {
			List data = (List) JSONDecoder
			.decode(loadText(in, "utf-8"));
			return new Template((List)data.get(1));
		} finally {
			in.close();
		}
	}

	public static String loadText(InputStream in, String encoding)
			throws IOException {
		if (in == null) {
			return null;
		}
		Reader reader = new InputStreamReader(in, encoding);
		StringBuilder buf = new StringBuilder();
		char[] cbuf = new char[1024];
		for (int len = reader.read(cbuf); len > 0; len = reader.read(cbuf)) {
			buf.append(cbuf, 0, len);
		}
		return buf.toString();
	}
}
