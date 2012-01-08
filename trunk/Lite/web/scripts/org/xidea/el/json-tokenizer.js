/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
/**
 * JSON解码器器的纯JS实现
 */
function JSONTokenizer(value){
    this.value = value.replace(/^\s+|\s+$/g,'');
	this.start = 0;
	this.end = this.value.length;
}
JSONTokenizer.prototype = {
	parse : function() {
		this.skipComment();
		var c = this.value.charAt(this.start);
		if (c == '"') {
			return this.findString();
		} else if (c == '-' || c >= '0' && c <= '9') {
			return this.findNumber();
		} else if (c == '[') {
			return this.findList();
		} else if (c == '{') {
			return this.findMap();
		} else {
			var key = this.findId();
			if ("true".equals(key)) {
				return Boolean.TRUE;
			} else if ("false".equals(key)) {
				return Boolean.FALSE;
			} else if ("null".equals(key)) {
				return null;
			} else {
				throw new Error("语法错误:" + this.value + "@"
						+ this.start);
			}
		}
	},
	findMap : function() {
		this.start++;
		this.skipComment();
		var result = {};
		while (true) {
			// result.push(parse());
			var key =  this.parse();
			this.skipComment();
			var c = this.value.charAt(this.start++);
			if (c != ':') {
				throw new Error("错误对象语法:" + this.value + "@"
						+ this.start);
			}
			var valueObject = this.parse();
			this.skipComment();
			c = this.value.charAt(this.start++);
			if (c == '}') {
				result[key]= valueObject;
				return result;
			} else if (c != ',') {
				throw new Error("错误对象语法:" + this.value + "@"
						+ this.start);
			} else {
				result.put(key, valueObject);

			}
		}
	},

	findList:function() {
		var result = [];
		// this.start--;
		this.start++;
		this.skipComment();
		if (this.value.charAt(this.start) == ']') {
			this.start++;
			return result;
		} else {
			result.push(this.parse());
		}
		while (true) {
			this.skipComment();
			var c = this.value.charAt(this.start++);
			if (c == ']') {
				return result;
			} else if (c == ',') {
				this.skipComment();
				result.push(this.parse());
			} else {
				throw new Error("错误数组语法:" + this.value + "@"
						+ this.start);
			}
		}
	},

	findNumber:function() {
		var i = this.start;// skip -;
		var isFloatingPoint = false;

		var c = this.value.charAt(i++);
		if (c == '-') {
			c = this.value.charAt(i++);
		}
		if (c == '0') {
			if (i < this.end) {
				return this.parseZero();
			} else {
				this.start = i;
				return 0;
			}
		}
		var ivalue = c - '0';
		while (i < this.end) {
			c = this.value.charAt(i++);
			if (c >= '0' && c <= '9') {
				ivalue = (ivalue * 10) + (c - '0');
			} else {
				break;
			}
		}
		if (c == '.') {
			c = this.value.charAt(i++);
			while (c >= '0' && c <= '9') {
				isFloatingPoint = true;
				if (i < this.end) {
					c = this.value.charAt(i++);
				} else {
					break;
				}
			}
			if (!isFloatingPoint) {
				// c = '.';
				// i--;
				this.start = i - 2;
				return ivalue;
			}
		}
		if (c == 'E' || c == 'e') {
			isFloatingPoint = true;
			c = this.value.charAt(i++);
			if (c == '+' || c == '-') {
				c = this.value.charAt(i++);
			}
			while (c >= '0' && c <= '9') {
				if (i < this.end) {
					c = this.value.charAt(i++);
				} else {
					break;
				}
			}
		} else {
			c = this.value.charAt(i - 1);
			if (c < '0' || c > '9') {
				i--;
			}
		}

		if (isFloatingPoint) {
			return this.value.substring(this.start, this.start = i)*1;
		} else {
			this.start = i;
			return ivalue;
		}
	},
	parseZero: function(){
		var value = this.value.substr(this.start);
		value = value.replace(/([+-]?0(?:x[0-9a-f]+|\.?[0-9]*))[\s\S]*/i,'$1');
		this.start += value.length;
		//print(value+'/'+parseInt(value))
		if(value.indexOf('.')<0){
			return parseInt(value);
		}
		return parseFloat(value);
	},
	findId:function() {
		var p = this.start;
		if (/[\w\$_]/.test(this.value.charAt(p++))) {
			while (p < this.end) {
				if (!/[\w\$_]/.test(this.value.charAt(p))) {
					break;
				}
				p++;
			}
			return (this.value.substring(this.start, this.start = p));
		}
		throw new Error("无效id");

	},

	/**
	 * {@link Decompiler#printSourceString
	 */
	findString:function() {
		var quoteChar = this.value.charAt(this.start++);
		var buf = [];
		while (this.start < this.end) {
			var c = this.value.charAt(this.start++);
			switch (c) {
			case '\\':
				var c2 = this.value.charAt(this.start++);
				switch (c2) {
				case 'b':
					buf.push('\b');
					break;
				case 'f':
					buf.push('\f');
					break;
				case 'n':
					buf.push('\n');
					break;
				case 'r':
					buf.push('\r');
					break;
				case 't':
					buf.push('\t');
					break;
				case 'v':
					buf.push(0xb);
					break; // Java lacks \v.
				case ' ':
					buf.push(' ');
					break;
				case '\\':
				case '\/':
					buf.push(c2);
					break;
				case '\'':
					buf.push('\'');
					break;
				case '\"':
					buf.push('"');
					break;
				case 'u':
					var c = this.value.substring(
							this.start, this.start + 4);
					c = parseInt(c, 16);
					
					buf.push(String.fromCharCode(c));
					this.start += 4;
					break;
				case 'x':
					var c = this.value.substring(this.start, this.start + 2);
					c = parseInt(c, 16);
					buf.push(String.fromCharCode(c));
					this.start += 2;
					break;
				default:
					buf.push(c);
					buf.push(c2);
				}
				break;
			case '"':
			case '\'':
				if (c == quoteChar) {
					return (buf.join(''));
				}
			default:
				buf.push(c);

			}
		}
		throw new Error("未结束字符串:" + this.value
				+ "@" + this.start);
	},

	skipComment:function() {
		while (true) {
			while (this.start < this.end) {
			    var c = this.value.charAt(this.start);
				if (c == ' ' || c =='\t') {
				      this.start++;
				}else{
				    break;
				}
				
			}
			if (this.start < this.end && this.value.charAt(this.start) == '/') {
				this.start++;
				var next = this.value.charAt(this.start++);
				if (next == '/') {
					var end1 = this.value.indexOf('\n', this.start);
					var end2 = this.value.indexOf('\r', this.start);
					var cend = Math.min(end1, end2);
					if (cend < 0) {
						cend = Math.max(end1, end2);
					}
					if (cend > 0) {
						this.start = cend;
					} else {
						this.start = this.end;
					}
				} else if (next == '*') {
					var cend = this.value.indexOf("*/", this.start);
					if (cend > 0) {
						this.start = cend + 2;
					} else {
						throw new Error("未結束注釋:" + this.value
								+ "@" + this.start);
					}
				}
			} else {
				break;
			}
		}
	},

	skipSpace:function(nextChar) {
		while (this.start < this.end) {
			var c = this.value.charAt(this.start);
			if (c == ' ' || c =='\t' || c == '\r' || c == '\n') {
			      this.start++;
			}else{
			    break;
			}
		}
		if (nextChar > '\x00' && this.start < this.end) {
			var next = this.value.charAt(this.start);
			if (nextChar == next) {
				return true;
			}
		}
		return false;
	}
}

//function parseNumber(text, radix) {
//	return parseInt(text, radix);
//}


if(typeof require == 'function'){
exports.JSONTokenizer=JSONTokenizer;
}