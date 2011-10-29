package org.xidea.lite.tools.image.test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.xidea.lite.tools.ImageUtil;
import org.xidea.lite.tools.image.ImageCut;

public class ImageCutTest {

	@Test
	public void testBoxCut() throws IOException{
		URL url = this.getClass().getResource("box1.png");
		cutBox(url);
		
	}

	private void cutBox(URL url) throws IOException {
		BufferedImage image = ImageIO.read(url);
		int width = image.getWidth();
		int height = image.getHeight();
		int[] data = image.getRGB(0, 0, width, height, null, 0, width);
		ImageCut ic = new ImageCut();
		int[] pos = ic.seachBorder(data, width, height);
		System.out.println(pos[0]);
		System.out.println(pos[1]);
		System.out.println(pos[2]);
		System.out.println(pos[3]);
		int x = pos[0];
		int y = pos[1];
		int width2 = pos[2]-x;
		int height2 = pos[3]-y;
	}
}
