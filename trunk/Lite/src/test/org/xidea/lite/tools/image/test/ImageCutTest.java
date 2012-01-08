package org.xidea.lite.tools.image.test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.lite.tools.image.ImageCut;

public class ImageCutTest {

	@Test
	public void testBoxCut() throws IOException{
		cutBox("8119041");

		cutBox("8139041");
	}

	private void cutBox(String name) throws IOException {
		URL url = this.getClass().getResource(name+".png");
		BufferedImage image = ImageIO.read(url);
		int width = image.getWidth();
		int height = image.getHeight();
		int[] data = image.getRGB(0, 0, width, height, null, 0, width);
		ImageCut ic = new ImageCut();
		int[] pos = ic.seachOuterBorder(data, width, height);
		int x0 = pos[0];
		int y0 = pos[1];
		int x1 = pos[2];//-x;
		int y1 = pos[3];//-y;
		String margin = new StringBuilder().append(x0).append(y0).append(x1).append(y1).toString();
		Assert.assertEquals("", name, margin);
	}
}
