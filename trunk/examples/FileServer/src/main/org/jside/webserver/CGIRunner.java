package org.jside.webserver;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CGIRunner {
	private static final Log log = LogFactory.getLog(CGIRunner.class);
	/** script/command to be executed */
	private String command = null;
	/** environment used when invoking the cgi script */
	private Map<String, String> env = null;
	/** working directory used when invoking the cgi script */
	private File wd = null;
	/** command line parameters to be passed to the invoked script */
	private String[] arguments = null;
	/** response object used to set headers & get output stream */
	private RequestContext response = null;
	/** boolean tracking whether this object has enough info to run() */
	private String[] cgiExecutable = {"php-cgi"};
	private long stderrTimeout = 1000 * 60 * 2;

	/**
	 * Creates a CGIRunner and initializes its environment, working directory,
	 * and query parameters. <BR>
	 * Input/output streams (optional) are set using the <code>setInput</code>
	 * and <code>setResponse</code> methods, respectively.
	 * 
	 * @param command
	 *            string full path to command to be executed
	 * @param env
	 *            Hashtable with the desired script environment
	 * @param wd
	 *            File with the script's desired working directory
	 * @param params
	 *            ArrayList with the script's query command line paramters as
	 *            strings
	 */
	public CGIRunner(RequestContext req, String command,
			Map<String, String> env, File wd, String[] arguments) {

		this.response = req;
		this.command = command;
		this.env = env;
		this.wd = wd;
		this.arguments = arguments;
	}

	/**
	 * Converts a Hashtable to a String array by converting each key/value pair
	 * in the Hashtable to a String in the form "key=value" (hashkey + "=" +
	 * hash.get(hashkey).toString())
	 * 
	 * @param h
	 *            Hashtable to convert
	 * 
	 * @return converted string array
	 * 
	 * @exception NullPointerException
	 *                if a hash key has a null value
	 * 
	 */
	protected String[] hashToStringArray(Map<String, String> h)
			throws NullPointerException {
		ArrayList<String> v = new ArrayList<String>();
		for (Map.Entry<String, String> e : h.entrySet()) {
			v.add(e.getKey() + "=" + e.getValue());
		}
		return v.toArray(new String[v.size()]);
	}

	/**
	 * Executes a CGI script with the desired environment, current working
	 * directory, and input/output streams
	 * 
	 * <p>
	 * This implements the following CGI specification recommedations:
	 * <UL>
	 * <LI>Servers SHOULD provide the "<code>query</code>" component of the
	 * script-URI as command-line arguments to scripts if it does not contain
	 * any unencoded "=" characters and the command-line arguments can be
	 * generated in an unambiguous manner.
	 * <LI>Servers SHOULD set the AUTH_TYPE metavariable to the value of the "
	 * <code>auth-scheme</code>" token of the "<code>Authorization</code>" if it
	 * was supplied as part of the request header. See
	 * <code>getCGIEnvironment</code> method.
	 * <LI>Where applicable, servers SHOULD set the current working directory to
	 * the directory in which the script is located before invoking it.
	 * <LI>Server implementations SHOULD define their behavior for the following
	 * cases:
	 * <ul>
	 * <LI><u>Allowed characters in pathInfo</u>: This implementation does not
	 * allow ASCII NUL nor any character which cannot be URL-encoded according
	 * to internet standards;
	 * <LI><u>Allowed characters in path segments</u>: This implementation does
	 * not allow non-terminal NULL segments in the the path -- IOExceptions may
	 * be thrown;
	 * <LI><u>"<code>.</code>" and "<code>..</code>" path segments</u>: This
	 * implementation does not allow "<code>.</code>" and "<code>..</code>" in
	 * the the path, and such characters will result in an IOException being
	 * thrown;
	 * <LI><u>Implementation limitations</u>: This implementation does not
	 * impose any limitations except as documented above. This implementation
	 * may be limited by the servlet container used to house this
	 * implementation. In particular, all the primary CGI variable values are
	 * derived either directly or indirectly from the container's implementation
	 * of the Servlet API methods.
	 * </ul>
	 * </UL>
	 * </p>
	 * 
	 * @exception IOException
	 *                if problems during reading/writing occur
	 * 
	 * @see java.lang.Runtime#exec(String command, String[] envp, File dir)
	 */
	public void run() throws IOException {

		if (log.isDebugEnabled()) {
			log.debug("runCGI(envp=[" + env + "], command=" + command + ")");
		}
		if ((command.indexOf(File.separator + "." + File.separator) >= 0)
				|| (command.indexOf(File.separator + "..") >= 0)
				|| (command.indexOf(".." + File.separator) >= 0)) {
			throw new IOException(this.getClass().getName()
					+ "Illegal Character in CGI command "
					+ "path ('.' or '..') detected.  Not " + "running CGI ["
					+ command + "].");
		}
		/*
		 * original content/structure of this section taken from
		 * http://developer.java.sun.com/developer/ bugParade/bugs/4216884.html
		 * with major modifications by Martin Dengler
		 */
		Runtime rt = null;
		InputStream cgiOutput = null;
		BufferedReader commandsStdErr = null;
		Thread errReaderThread = null;
		BufferedOutputStream commandsStdIn = null;
		Process proc = null;
		int bufRead = -1;
		// create query arguments
		ArrayList<String> cmdAndArgs = new ArrayList<String>(cgiExecutable.length+1);
		for (String arg : cgiExecutable) {
			cmdAndArgs.add(arg);
		}
		cmdAndArgs.add(command);
		if (arguments != null) {
			for (String arg : arguments) {
				cmdAndArgs.add(arg);
			}
		}
//		StringBuffer commands = new StringBuffer(cgiExecutable[0]);
//		commands.append(" ");
//		commands.append(cmdAndArgs.toString());
//		cmdAndArgs = commands;
		try {
			rt = Runtime.getRuntime();
			proc = rt.exec(
					cmdAndArgs.toArray(new String[cmdAndArgs.size()]), hashToStringArray(env), wd);
			String sContentLength = (String) env.get("CONTENT_LENGTH");
			if (!"".equals(sContentLength)) {
//				System.out.println(sContentLength);
				commandsStdIn = new BufferedOutputStream(proc.getOutputStream());
				flow(response.getInputStream(), commandsStdIn,Integer.parseInt(sContentLength));
				commandsStdIn.flush();
				commandsStdIn.close();
			}
			/*
			 * we want to wait for the process to exit, Process.waitFor() is
			 * useless in our situation; see
			 * http://developer.java.sun.com/developer/
			 * bugParade/bugs/4223650.html
			 */
			boolean isRunning = true;
			commandsStdErr = new BufferedReader(new InputStreamReader(proc
					.getErrorStream()));
			final BufferedReader stdErrRdr = commandsStdErr;
			errReaderThread = new Thread() {
				public void run() {
					log(stdErrRdr);
				};
			};
			errReaderThread.start();
			InputStream cgiHeaderStream = new HTTPHeaderInputStream(proc
					.getInputStream());
			BufferedReader cgiHeaderReader = new BufferedReader(
					new InputStreamReader(cgiHeaderStream));
			while (isRunning) {
				try {
					// set headers
					String line = null;
					while (((line = cgiHeaderReader.readLine()) != null)
							&& !("".equals(line))) {
						if (log.isDebugEnabled()) {
							log.debug("runCGI: addHeader(\"" + line + "\")");
						}
						if (line.startsWith("HTTP")) {
							response.setStatus(getSCFromHttpStatusLine(line),
									"");
						} else if (line.indexOf(":") >= 0) {
							String header = line
									.substring(0, line.indexOf(":")).trim();
							if (header.equalsIgnoreCase("status")) {
								String value = line.substring(
										line.indexOf(":") + 1).trim();
								response.setStatus(
										getSCFromCGIStatusHeader(value), "");
							} else {
								response.addResponseHeader(line.trim());
							}
						} else {
							log.error("runCGI: bad header line \"" + line
									+ "\"");
						}
					}
					// write output
					byte[] bBuf = new byte[2048];
					OutputStream out = response.getOutputStream();
					cgiOutput = proc.getInputStream();
					try {
						while ((bufRead = cgiOutput.read(bBuf)) != -1) {
							if (log.isDebugEnabled()) {
								log.debug("runCGI: output " + bufRead
										+ " bytes of data");
							}
							out.write(bBuf, 0, bufRead);
						}
					} finally {
						// Attempt to consume any leftover byte if something
						// bad happens,
						// such as a socket disconnect on the servlet side;
						// otherwise, the
						// external process could hang
						if (bufRead != -1) {
							while ((bufRead = cgiOutput.read(bBuf)) != -1) {
							}
						}
					}
					proc.exitValue(); // Throws exception if alive
					isRunning = false;
				} catch (IllegalThreadStateException e) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException ignored) {
					}
				}
			} // replacement for Process.waitFor()
		} catch (IOException e) {
			log.error("Caught exception " , e);
			throw e;
		} finally {
			// Close the output stream if used
			if (cgiOutput != null) {
				try {
					cgiOutput.close();
				} catch (IOException ioe) {
					log.error("Exception closing output stream " + ioe);
				}
			}
			// Make sure the error stream reader has finished
			if (errReaderThread != null) {
				try {
					errReaderThread.join(stderrTimeout);
				} catch (InterruptedException e) {
					log.error("Interupted waiting for stderr reader thread");
				}
			}
			if (log.isDebugEnabled()) {
				log.debug("Running finally block");
			}
			if (proc != null) {
				proc.destroy();
				proc = null;
			}
		}
	}

	/**
	 * Parses the Status-Line and extracts the status code.
	 * 
	 * @param line
	 *            The HTTP Status-Line (RFC2616, section 6.1)
	 * @return The extracted status code or the code representing an internal
	 *         error if a valid status code cannot be extracted.
	 */
	private int getSCFromHttpStatusLine(String line) {
		int statusStart = line.indexOf(' ') + 1;
		if (statusStart < 1 || line.length() < statusStart + 3) {
			// Not a valid HTTP Status-Line
			log.error("runCGI: invalid HTTP Status-Line:" + line);
			return 500;// HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
		String status = line.substring(statusStart, statusStart + 3);
		int statusCode;
		try {
			statusCode = Integer.parseInt(status);
		} catch (NumberFormatException nfe) {
			// Not a valid status code
			log.error("runCGI: invalid status code:" + status);
			return 500;// HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
		return statusCode;
	}

	/**
	 * Parses the CGI Status Header value and extracts the status code.
	 * 
	 * @param value
	 *            The CGI Status value of the form <code>
        *             digit digit digit SP reason-phrase</code>
	 * @return The extracted status code or the code representing an internal
	 *         error if a valid status code cannot be extracted.
	 */
	private int getSCFromCGIStatusHeader(String value) {
		if (value.length() < 3) {
			// Not a valid status value
			log.error("runCGI: invalid status value:" + value);
			return 500;// HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
		String status = value.substring(0, 3);
		int statusCode;
		try {
			statusCode = Integer.parseInt(status);
		} catch (NumberFormatException nfe) {
			// Not a valid status code
			log.error("runCGI: invalid status code:" + status);
			return 500;// HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
		return statusCode;
	}

	private static final Pattern ERROR = Pattern.compile(
			"^(?:\\w+\\s*)?(?:(Fatal)|(Error)|(Warning)|(Notice))\\:",
			Pattern.CASE_INSENSITIVE);

	protected void log(BufferedReader rdr) {
		String line = null;
		int lineCount = 0;
		try {
			while ((line = rdr.readLine()) != null) {
				String cs= response.getEncoding();
				response.getOutputStream().write(line.getBytes(cs == null?"UTF-8":cs));
				Matcher m = ERROR.matcher(line);
				if (m.find()) {
					int c = m.groupCount();
					if (c > 1 && m.group(1) != null) {
						log.fatal("CGI STDERR:" + line);
					} else if (c > 2 && m.group(2) != null) {
						log.error("CGI STDERR:" + line);
					} else if (c > 3 && m.group(3) != null) {
						log.warn("CGI STDERR:" + line);
					} else {// if(m.group(4) != null){
						log.debug("CGI STDERR:" + line);
					}
				} else {
					log.info("CGI STDERR:" + line);
				}

				lineCount++;
			}
		} catch (IOException e) {
			log.error("log error", e);
		} finally {
			try {
				rdr.close();
			} catch (IOException ce) {
				log.error("log error", ce);
			}
			;
		}
		;
		if (lineCount > 0 && log.isDebugEnabled()) {
			log.debug("runCGI: " + lineCount + " lines received on stderr");
		}
		;
	}

	public void setCgiExecutable(String[] cgiExecutable) {
		this.cgiExecutable = cgiExecutable;
	}

	/**
	 * This is an input stream specifically for reading HTTP headers. It reads
	 * upto and including the two blank lines terminating the headers. It allows
	 * the content to be read using bytes or characters as appropriate.
	 */
	protected static class HTTPHeaderInputStream extends InputStream {
		private static final int STATE_CHARACTER = 0;
		private static final int STATE_FIRST_CR = 1;
		private static final int STATE_FIRST_LF = 2;
		private static final int STATE_SECOND_CR = 3;
		private static final int STATE_HEADER_END = 4;
		private InputStream input;
		private int state;

		HTTPHeaderInputStream(InputStream theInput) {
			input = theInput;
			state = STATE_CHARACTER;
		}

		/**
		 * @see java.io.InputStream#read()
		 */
		public int read() throws IOException {
			if (state == STATE_HEADER_END) {
				return -1;
			}
			int i = input.read();
			// Update the state
			// State machine looks like this
			//
			// -------->--------
			// | (CR) |
			// | |
			// CR1--->--- |
			// | | |
			// ^(CR) |(LF) |
			// | | |
			// CHAR--->--LF1--->--EOH
			// (LF) | (LF) |
			// |(CR) ^(LF)
			// | |
			// (CR2)-->---
			if (i == 10) {
				// LF
				switch (state) {
				case STATE_CHARACTER:
					state = STATE_FIRST_LF;
					break;
				case STATE_FIRST_CR:
					state = STATE_FIRST_LF;
					break;
				case STATE_FIRST_LF:
				case STATE_SECOND_CR:
					state = STATE_HEADER_END;
					break;
				}
			} else if (i == 13) {
				// CR
				switch (state) {
				case STATE_CHARACTER:
					state = STATE_FIRST_CR;
					break;
				case STATE_FIRST_CR:
					state = STATE_HEADER_END;
					break;
				case STATE_FIRST_LF:
					state = STATE_SECOND_CR;
					break;
				}
			} else {
				state = STATE_CHARACTER;
			}
			return i;
		}
	}

	public static void flow(InputStream is, OutputStream os,int length) throws IOException {
		byte[] buf = new byte[Math.min(1024, length)];
		int numRead;
		while (length >0 && (numRead = is.read(buf)) >= 0) {
			length -= numRead;
//			System.out.println(length);
			os.write(buf, 0, numRead);
		}
	}
}
