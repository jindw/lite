/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jside.webserver.action;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.jside.webserver.CGIEnvironment;
import org.jside.webserver.CGIRunner;
import org.jside.webserver.RequestContext;
import org.jside.webserver.RequestUtil;

/**
 * 
 * @author Martin T Dengler [root@martindengler.com]
 * @author Amy Roh
 * @version $Revision: 896371 $, $Date: 2010-01-06 11:30:07 +0100 (Wed, 06 Jan
 *          2010) $
 * @since Tomcat 4.0
 */
public final class CGIAdaptor {
	private File workDir;

	public CGIAdaptor() {
	}

	public File getWorkDir() {
		return workDir;
	}

	public void setWorkDir(File workDir) {
		this.workDir = workDir;
	}

	public void execute() throws IOException {
		RequestContext req = RequestUtil.get();
		CGIEnvironment cgiEnv = new CGIEnvironment(req);
		String filename = cgiEnv.scriptFilename;
		if (filename != null) {
			File file = new File(filename);
			if (file.exists()) {
				Map<String, String> env = cgiEnv.toMap(System.getenv());
				String[] args = new String[0];
				File work = workDir;
				if(work == null){
					work = file.getParentFile();
				}
				CGIRunner cgi = new CGIRunner(req, filename, env, work, args);
				cgi.run();
				return;
			}
		}
		req.setStatus(404, "CGI not Found");

	} // doGet

}
