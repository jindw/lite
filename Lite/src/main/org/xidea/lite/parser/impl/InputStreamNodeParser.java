package org.xidea.lite.parser.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.parser.NodeParser;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;

public class InputStreamNodeParser implements NodeParser<InputStream> {
	@SuppressWarnings("unused")
	private static Log log = LogFactory.getLog(InputStreamNodeParser.class);
	private static Pattern CHARSET_PATTERN = Pattern
			.compile("^.*(?:charset|coding|encoding)=([\\w\\-]+)");
	private static Pattern IMPL_PATTERN = Pattern
			.compile("^.*(?:impl|parser|ext|extension|extention)=([\\w\\-\\_\\/\\.]+)");

	private static ExtensionParser extensionParser = new ExtensionParser();
	public void parse(final InputStream in, ParseContext context,
			ParseChain chain) {
		String text = loadText(in);
		String ext = getParam(text, IMPL_PATTERN);
		text = text.replaceFirst("^#!.*\\r?\\n?", "");
		if(ext != null){
			extensionParser.processResource(ext, context);
		}
		context.parse(text);
	}

	private String loadText(InputStream in) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			in = ParseUtil.trimBOM(in);
			for (int c = in.read(buf); c >= 0; c = in.read(buf)) {
				out.write(buf, 0, c);
			}
			byte[] data = out.toByteArray();
			String text = new String(data, "utf-8");
			String charset = null;
			if (text.startsWith("#!")) {
				charset = getParam(text, CHARSET_PATTERN);
			}
			if (charset == null) {
				if (Arrays.equals(data, text.getBytes("utf-8"))) {
					charset = "utf-8";
				} else {
					charset = "GBK";
				}
			}
			if (!charset.equalsIgnoreCase("utf-8")
					&& !charset.equalsIgnoreCase("utf8")) {
				text = new String(data, charset);
			}
			return text;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String getParam(String text,Pattern pattern) {
		Matcher matcher = pattern.matcher(text);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

}
