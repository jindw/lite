package org.xidea.lite.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.TextParser;
import org.xidea.lite.parser.NodeParser;



public class PluginLoader{
	protected List<TextParser> textParserList = new ArrayList<TextParser>();
	protected List<NodeParser<? extends Node>> nodeParserList = new ArrayList<NodeParser<? extends Node>>();
	private static Log log = LogFactory.getLog(PluginLoader.class);

	@SuppressWarnings("unchecked")
	public void setup(ParseContext context, URL pluginSource) {
		if(pluginSource != null){
			try {
				this.load(pluginSource);
			} catch (IOException e) {
				log.error(e);
			}
		}
		setup(context);
	}
	@SuppressWarnings("unchecked")
	public void setup(ParseContext context) {
		for(TextParser parser : textParserList){
			context.addTextParser(parser);
		}
		for(NodeParser parser : nodeParserList){
			context.addNodeParser(parser);
		}
	}
	public void load(Reader source){//不用string 是为了避免 string source url混淆
		RhinoContext rhinoContext = new RhinoContext(this);
		rhinoContext.initialize(loadText(source));
	}

	public void load(URL source) throws IOException{
		InputStream in = source.openStream();
		try{
			this.load(new InputStreamReader(in,"UTF-8"));
		}finally{
			in.close();
		}
	}
	public void addInstructionParser(TextParser iparser) {
		textParserList.add(iparser);
	}


	public List<NodeParser<? extends Node>> getNodeParserList() {
		return nodeParserList;
	}

	public void addNodeParser(NodeParser<? extends Node> parser) {
		nodeParserList.add(parser);
	}

	public static String loadText(Reader in) {
		//Reader in = new InputStreamReader(sin, "utf-8");
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
