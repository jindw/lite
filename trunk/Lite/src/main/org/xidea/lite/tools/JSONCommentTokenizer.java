package org.xidea.lite.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xidea.el.json.JSONEncoder;
import org.xidea.el.json.JSONTokenizer;

public class JSONCommentTokenizer extends JSONTokenizer {
	private String path = "";
	private Map<String, String> commentMap = new HashMap<String, String>();

	public JSONCommentTokenizer(String source) {
		super(source, false);
	}

	private String readComment() {
		int start = this.start;
		skipComment();
		return this.value.substring(start, this.start);
	}

	private void addComment(String comment) {
		if (comment != null) {
			String content = commentMap.get(path);
			comment = comment.trim();
			if (comment.length() > 0) {
				if (content != null) {
					comment = content + "\n" + comment;
				}
				commentMap.put(path, comment);
			}
		}
	}

	protected Object parse() {
		this.addComment(readComment());
		return super.parse();
	}

	protected Map<String, Object> findMap() {
		start++;
		String preComment = readComment();
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		if (value.charAt(start) == '}') {
			start++;
			this.addComment(preComment);
			return result;
		}
		final String pathPrefix = this.path;
		while (true) {
			// result.add(parse());
			try {
				char c = value.charAt(start);
				final String key;
				if (c == '"') {
					key = findString();
				} else {
					if (c == '\'') {
						key = findString();
					} else {
						key = findId();
					}
				}
				this.path = pathPrefix + '[' + JSONEncoder.encode(key) + ']';
				this.addComment(preComment);
				preComment = null;
				this.addComment(readComment());
				c = value.charAt(start++);
				if (c != ':') {
					throw buildError("无效对象语法");
				}
				Object valueObject = parse();
				this.addComment(readComment());
				c = value.charAt(start++);
				if (c == ',') {
					result.put(key, valueObject);
					preComment = readComment();
				} else if (c == '}') {
					result.put(key, valueObject);
					return result;
				} else{
					throw buildError("无效对象语法");
				}
			} finally {
				this.path = pathPrefix;
			}
		}
	}

	protected List<Object> findList() {
		ArrayList<Object> result = new ArrayList<Object>();
		// start--;
		start++;
		String pathPrefix = this.path;
		String preComment = readComment();
		if (value.charAt(start) == ']') {
			start++;
			this.addComment(preComment);
			return result;
		} else {
			this.path = pathPrefix + "[0]";
			result.add(parse());
		}

		int index = 1;
		while (true) {
			try {
				this.addComment(readComment());
				this.path = pathPrefix + '[' + (index++) + ']';
				char c = value.charAt(start++);
				if (c == ']') {
					return result;
				} else if (c == ',') {
					result.add(parse());
				} else {
					throw buildError("无效数组语法:");
				}
			} finally {
				this.path = pathPrefix;
			}
		}
	}


}
