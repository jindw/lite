package org.xidea.lite.plugin;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;


import org.w3c.dom.Node;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.Parser;


public class PluginLoader{
	List<InstructionParser> iparserList = new ArrayList<InstructionParser>();
	List<Parser<? extends Node>> parserList = new ArrayList<Parser<? extends Node>>();

	public void load(String source){
		RhinoContext rhinoContext = new RhinoContext(this);
		rhinoContext.setUp(source);
	}

	public void load(Reader source) throws IOException{
		this.load(loadText(source));
	}
	public void addInstructionParser(InstructionParser iparser) {
		iparserList.add(iparser);
	}

	public List<InstructionParser> getInstructionParserList() {
		return iparserList;
	}

	public List<Parser<? extends Node>> getNodeParserList() {
		return parserList;
	}

	public void addNodeParser(Parser<? extends Node> parser) {
		parserList.add(parser);
	}

	static String loadText(Reader in) {
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
