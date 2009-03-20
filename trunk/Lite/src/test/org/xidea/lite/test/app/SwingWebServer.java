package org.xidea.lite.test.app;

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
import java.io.IOException;
import java.net.URI;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class SwingWebServer extends SimpleWebServer {

	public SwingWebServer(File webBase) {
		super(webBase);
	}

	public void reset(File webBase) {
		super.reset(webBase);
		if (!webBase.exists()) {
			webBase.mkdirs();
		}
		FileFilter2 filter = new FileFilter2();
		webBase.listFiles(filter);
		if(filter.count == 0){
		}
	}

	class FileFilter2 implements FileFilter {
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

		final SwingWebServer server = new SwingWebServer(root.getAbsoluteFile());
		server.start();
		final JFrame frame = new JFrame("Lite XML 模版测试服务器");
		frame.setLayout(null);
		frame.setSize(300, 50);
		final JTextField siteInput = new JTextField(root.getAbsolutePath());
		siteInput.setBounds(2, 2, 120, 20);
		frame.add(siteInput);
		final JButton changeHomeButton = new JButton("重定位网站地址");
		changeHomeButton.setBounds(124, 2, 120, 20);
		frame.add(changeHomeButton);
		final JLabel homeLabel = new JLabel("<html><a href='#'>网站首页</a></html>");
		homeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		homeLabel.setBounds(2, 32, 120, 20);
		frame.add(homeLabel);
		homeLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(
							new URI("http://localhost:" + server.getPort()));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		final JLabel toolsLabel = new JLabel(
				"<html><a href='#'>管理首页</a></html>");
		toolsLabel.setBounds(124, 32, 120, 20);
		toolsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		frame.add(toolsLabel);
		toolsLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				try {
					JOptionPane.showConfirmDialog(frame, "还没写完呢,等等把!");
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
						server.reset(new File(siteInput.getText()));
					} catch (IOException e1) {
						JOptionPane.showConfirmDialog(frame, "ERROR:"
								+ e1.getMessage());
					}
				}
			}
		});
		frame.setSize(252, 90);
		frame.setResizable(false);

		frame.setVisible(true);
		frame.setAlwaysOnTop(true);
	}
}
