package org.xidea.lite.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.jsi.JSIRuntime;
import org.xidea.lite.impl.ParseUtil;

class LiteCompilerHelper {
	private static final Log log = LogFactory.getLog(LiteCompilerHelper.class);

	static void writeOutput(Map<String, byte[]> resultMap, File output)
			throws IOException {
		if (!output.exists()) {
			if (output.getName().endsWith(".zip")) {
				output.getParentFile().mkdir();
				output.createNewFile();
			} else {
				output.mkdir();
			}
		}
		if (output.getName().endsWith(".zip")) {
			writeZipResult(resultMap, output);
		} else {
			writeDirResult(resultMap, output);
		}
	}
	static void checkOutput(String[] includes) {
		boolean notOnlyXhtml = false;
		if (includes != null) {
			for (String inc : includes) {
				if (!inc.endsWith(".xhtml")) {
					notOnlyXhtml = true;
					break;
				}
			}
		} else {
			notOnlyXhtml = true;
		}
		if (notOnlyXhtml) {
			log.error("输出目录与源码目录一致,这将导致源码目录可能被覆盖！");
			System
					.console()
					.printf(
							"-output is equals to this -root; source code may be override. please put yes to continue: yes || no?");
			if (!"yes".equalsIgnoreCase(System.console().readLine())) {
				System.exit(0);
			}
		}
	}
	static void buildPHP(String path, String litecode, String encoding,Map<String, byte[]> resultMap)
			throws IOException {
		JSIRuntime runtime = ParseUtil.getJSIRuntime();
		Object translator = runtime
				.eval("new ($import('org.xidea.lite.impl.php:PHPTranslator',{}))('"
						+ path + "'," + litecode + ")");
		String result = (String) runtime.invoke(translator, "translate");
		resultMap.put(LiteCompilerHelper.translatePath(path) + ".php",
				result.getBytes(encoding));
	}

	private static void writeZipResult(Map<String, byte[]> resultMap,
			File output) throws IOException {
		ZipOutputStream zipos = new ZipOutputStream(
				new FileOutputStream(output));
		for (String path : resultMap.keySet()) {
			zipos.setMethod(ZipOutputStream.DEFLATED);
			zipos.putNextEntry(new ZipEntry(path.substring(1)));
			zipos.write(resultMap.get(path));
		}
		zipos.flush();
		zipos.finish();
		zipos.close();
	}

	private static void writeDirResult(Map<String, byte[]> resultMap,
			File output) throws IOException {
		for (String path : resultMap.keySet()) {
			byte[] data = resultMap.get(path);
			File cachedFile = new File(output, path);
			cachedFile.getParentFile().mkdirs();
			OutputStream out = new FileOutputStream(cachedFile);
			try {
				out.write(data);
				out.flush();
				log.info("文件写入成功:" + cachedFile);
			} finally {
				out.close();
			}
		}

	}

	static String translatePath(final String path) {
		return "/WEB-INF/litecode/" + path.replace('/', '^');
	}
}
