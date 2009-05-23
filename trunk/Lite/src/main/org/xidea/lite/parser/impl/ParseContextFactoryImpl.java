package org.xidea.lite.parser.impl;

import java.net.URL;
import java.util.Map;

import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.ParseContextFactory;
import org.xidea.lite.parser.Parser;
import org.xidea.lite.parser.ResultTransformer;

public class ParseContextFactoryImpl implements ParseContextFactory {

	public ParseContext createContext(URL base) {
		return new ParseContextImpl(base, null, null, null);
	}
	public ParseContext createContext(URL base, Map<String, String> featrues,
			Parser<? extends Object>[] parsers, InstructionParser[] ips) {
		return new ParseContextImpl(base, featrues, parsers, ips);
	}

	public ParseContext createContext(ParseContext parent) {
		return new ParseContextImpl(parent);
	}
	public ParseContext createContext(ParseContext parent,
			ResultTransformer transformer) {
		ParseContext context = new ParseContextImpl(parent);
		return context;
	}

}
