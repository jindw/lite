package org.xidea.lite.tools.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.xidea.jsi.JSIDependence;
import org.xidea.jsi.JSIPackage;
import org.xidea.jsi.impl.ClasspathRoot;
import org.xidea.jsi.impl.FileRoot;

public class TransformToCommonModel {
	static File projectRoot;
	static {
		try {
			File classes = new File(TransformToCommonModel.class.getResource(
					"/").toURI());
			projectRoot = new File(classes, "../../../").getCanonicalFile();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws IOException {
		File root = new File(projectRoot, "web/scripts");
		File dest = new File(projectRoot, "build/dest/require");
		final String encoding = "utf-8";
		boolean condition = true;
		FileRoot jsi = new FileRoot(root.getAbsolutePath(), encoding) {
			ClasspathRoot base = new ClasspathRoot(encoding);

			public String loadText(String pkgName, String scriptName) {
				String rtv = super.loadText(pkgName, scriptName);
				if (rtv == null) {
					rtv = base.loadText(pkgName, scriptName);
				}
				return rtv;

			}
		};
		List<String> pkgs = FileRoot.findPackageList(root);
		for (String packageName : pkgs) {
			JSIPackage pkg = jsi.requirePackage(packageName);
			pkg.initialize();
			Map<String, List<String>> som = pkg.getScriptObjectMap();
			Map<String, List<JSIDependence>> dm = pkg.getDependenceMap();
			for (String path : som.keySet()) {
				List<JSIDependence> dps = dm.get(path);
				StringBuilder before = new StringBuilder();
				StringBuilder after = new StringBuilder();

				String source = pkg.loadText(path);
				if (dps == null) {
					System.out.println("no dep:" + path + ":" + dm);
				} else {
					for (JSIDependence dp : dps) {
						String tname = dp.getTargetObjectName();
						JSIPackage tp = dp.getTargetPackage();
						StringBuilder buf = dp.isAfterLoad() ? after : before;
						String tpath = dp.getTargetFileName();
						String base = toRalative(packageName, tp.getName());
						if (tname != null) {
							appendRequire(buf, tpath, base, tname, source);
						} else {
							for (String tname2 : tp.getScriptObjectMap().get(
									tpath)) {
								appendRequire(buf, tpath, base, tname2, source);
							}
						}
					}
				}
				if (condition && before.length() > 0) {
					before.insert(0, "if(typeof require != 'function'){");
					before.append('}');
				}
				before.append(source);
				before.append("\r\n");
				if (condition && after.length() > 0) {
					before.append("if(typeof require != 'function'){");
				}
				List<String> vars = som.get(path);
				for (String var : vars) {
					before.append("exports." + var + "=" + var + ";\r\n");
				}

				before.append(after);
				if (condition && after.length() > 0) {
					before.append("}");
				}

				File to = new File(dest, packageName.replace('.', '/') + '/'
						+ path);
				to.getParentFile().mkdirs();
				FileOutputStream out = new FileOutputStream(to);
				out.write(before.toString().getBytes(encoding));
				out.flush();
				out.close();
			}

		}
	}

	private static void appendRequire(StringBuilder buf, String tpath,
			String base, String tname, String source) {
		if (source.indexOf(tname) >= 0) {
			if(base.startsWith("org/xidea/jsi")){
				if(tname.equals("console")){
					return;
				}
				System.out.println(base+tname);
			}
			buf.append("var " + tname + "=require('" + base
					+ tpath.replaceFirst("\\.js$", "") + "')." + tname
					+ ";\r\n");
		}
	}

	private static String toRalative(String packageName, String targetName) {
		if (packageName.equals(targetName)) {
			return "./";
		}

		if (targetName.startsWith(packageName + '.')) {
			return '.' + targetName.substring(packageName.length()).replace(
					'.', '/') + '/';
		}
		if (packageName.startsWith(targetName + '.')) {
			return packageName.substring(targetName.length()).replaceAll(
					"[^\\.]", "").replaceAll("[\\.]", "../");
		}
		return targetName.replace('.', '/') + '/';
	}

}
