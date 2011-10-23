package org.xidea.lite.tools.test;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;
import org.xidea.lite.tools.ImageResource;
import org.xidea.lite.tools.ImageSprite;

public class ImageSpriteTest {
	/**
	 * @throws URISyntaxException 
	 * 
	 */
	@Test
	public void test() throws Exception{
		URL root = this.getClass().getResource("./");
		System.out.println(root);
		ImageSprite is = new ImageSprite(root.toURI());
		ImageResource buf = is.createImage(276, 410);
		ImageResource sina = is.getImage("sina.gif");
		ImageResource baidu = is.getImage("baidu.gif");
		ImageResource google = is.getImage("google.png");
		buf.drawImage(sina, 0, 300);
		buf.drawImage(baidu, 0, 100);
		buf.drawImage(google, 0, 0);
		File dest = new File(new File(root.toURI()),"dest.png");
		FileOutputStream out = new FileOutputStream(dest);
		out.write(is.compress(buf));
		out.close();
		System.out.println(dest);
	}

}
