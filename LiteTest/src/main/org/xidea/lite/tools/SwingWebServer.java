package org.xidea.lite.tools;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.xidea.lite.tools.webserver.RequestHandle;

public class SwingWebServer extends SimpleWebServer {
	final private JFrame frame = new JFrame("Lite XML 模版测试服务器");

	public SwingWebServer(File webBase) {
		super(webBase);
	}

	@Override
	protected void processRequest(RequestHandle handle) throws IOException {
		String url = handle.getRequestURI();
		if (url.equals("/edit.action")) {
			final String path = handle.getParameter("path");
			final File templateFile = new File(super.webBase, path);
			final File jsonFile = new File(super.webBase, path.replaceFirst(
					"([^\\/]+).xhtml$", "$1.json"));
			if ("POST".equals(handle.getMethod())) {
				String template = handle.getParameter("template");
				save(templateFile, template);
				String json = handle.getParameter("json");
				save(jsonFile, json);
				handle.printRederect(path);
			} else {
				Map<Object, Object> context = new HashMap<Object, Object>();
				String template = null;
				String json = null;
				if (templateFile.exists()) {
					template = loadText(new FileInputStream(templateFile));
				}
				if (jsonFile.exists()) {
					json = loadText(new FileInputStream(jsonFile));
				}
				context.put("json", json);
				context.put("template", template);
				StringWriter out = new StringWriter();
				engine.render("/WEB-INF/edit.xhtml", context, out);
				handle.printContext(out, "text/html");
			}
		} else {
			super.processRequest(handle);
		}
	}

	private void save(File file, String data) {
		if (data != null && data.trim().length() > 0) {
			try {
				Writer out = new OutputStreamWriter(new FileOutputStream(file),
						"UTF-8");
				out.write(data);
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void reset(File webBase) {
		super.webBase = webBase;
		if (!webBase.exists()) {
			webBase.mkdirs();
		}
		TemplateFilter filter = new TemplateFilter();
		webBase.listFiles(filter);
		if (filter.count == 0) {
			int result = JOptionPane.showConfirmDialog(frame,
					"没有找到任何模板文件，是否将模板实例解压至网站根目录（"
							+ this.webBase.getAbsolutePath() + "）");
			if (result == JOptionPane.OK_OPTION) {
				ZipInputStream zin = new java.util.zip.ZipInputStream(this
						.getClass().getResourceAsStream("web.jar"));
				try {
					int count = 0;
					while (true) {
						ZipEntry zip = zin.getNextEntry();
						if (zip != null) {
							String key = zip.getName();
							File file = new File(this.webBase, key);
							if (zip.isDirectory()) {
								file.mkdirs();
							} else {
								file.getParentFile().mkdirs();
								FileOutputStream out = new FileOutputStream(
										file);
								byte[] buf = new byte[1024];
								int i;
								while ((i = zin.read(buf)) >= 0) {
									out.write(buf, 0, i);
								}
								count++;
								out.close();
							}
						} else {
							JOptionPane.showConfirmDialog(frame, "共有:" + count
									+ "个文件解压到");
							break;
						}
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		super.reset(webBase);
	}

	class TemplateFilter implements FileFilter {
		int count;

		public boolean accept(File file) {
			if (file.isDirectory()) {
				file.listFiles(this);
			} else {
				if (file.getName().toLowerCase().endsWith(POST_FIX_XHTML)) {
					count++;
				}
			}
			return false;
		}
	}

	public static void main(String[] a) throws Exception {
		File root = new File(".");
		if (new File(root, "web/WEB-INF").exists()) {
			root = new File(root, "web");
		}
		root = root.getCanonicalFile();
		final SwingWebServer server = new SwingWebServer(root.getAbsoluteFile());
		server.doStart(root);
	}

	private void doStart(File root) {
		this.start();
		frame.setLayout(null);
		final JTextField siteInput = new JTextField(root.getAbsolutePath());
		siteInput.setBounds(2, 2, 180, 20);
		frame.add(siteInput);
		siteInput.setEditable(false);
		final JButton changeHomeButton = new JButton("重定位网站根目录");
		changeHomeButton.setBounds(184, 2, 138, 20);
		frame.add(changeHomeButton);

		final JLabel homeLabel = new JLabel("<html><a href='#'>网站首页</a></html>");
		final JCheckBox formatCheck = new JCheckBox("格式化");
		final JCheckBox compressCheck = new JCheckBox("压缩");
		final JCheckBox xhtmlCheck = new JCheckBox("严谨语法",true);
		xhtmlCheck.setToolTipText("采用严谨的xhtml语法");

		formatCheck.setBounds(2, 32, 68, 20);
		compressCheck.setBounds(70, 32, 60, 20);
		xhtmlCheck.setBounds(130, 32, 120, 20);
		frame.add(formatCheck);
		frame.add(compressCheck);
		frame.add(xhtmlCheck);
		formatCheck.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				format = formatCheck.isSelected();
				reset(webBase);
			}
			
		});
		compressCheck.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				compress = compressCheck.isSelected();
				reset(webBase);
			}
		});
		xhtmlCheck.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				xhtml = compressCheck.isSelected();
				reset(webBase);
			}
		});
		homeLabel.setBounds(264, 32, 100, 20);
		frame.add(homeLabel);
		homeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		homeLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(
							new URI("http://localhost:"
									+ SwingWebServer.this.getPort()));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(1);
			}
		});
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("选择目录");
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}

			@Override
			public String getDescription() {
				return "网站目录";
			}
		});
		changeHomeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fileChooser.setSelectedFile(new File(siteInput.getText()));
				int status = fileChooser.showOpenDialog(frame);
				if (status == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					try {
						// siteInput.setBorder(BorderFactory.createLineBorder(Color.BLACK));
						siteInput.setText(file.getCanonicalPath());
						SwingWebServer.this
								.reset(new File(siteInput.getText()));
					} catch (IOException e1) {
						JOptionPane.showConfirmDialog(frame, "ERROR:"
								+ e1.getMessage());
					}
				}
			}
		});
		frame.setSize(330, 90);
		frame.setResizable(false);

		frame.setVisible(true);
		frame.setAlwaysOnTop(true);
	}
}
