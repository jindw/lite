package org.xidea.lite.tools.image.test;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.CompositeContext;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.xidea.lite.tools.ImageUtil;

public class ImageTest {

	@Test
	public void test() throws Exception {
		URL url = this.getClass().getResource("box1.png");
		File data = new File(url.toURI());
		System.out.println(data);
		BufferedImage image = ImageIO.read(data);
		BufferedImage image2 = ImageUtil.quantize(image, Color.RED);
		ImageIO.write(image2, "png", new File(data.getParentFile(), "out.png"));
	}
	public static BufferedImage matte(BufferedImage source, Color matteColor) {
		final int width = source.getWidth();
		final int height = source.getHeight();

		// A workaround for possibly different custom image types we can get:
		// draw a copy of the image
		final BufferedImage sourceConverted = new BufferedImage(width, height,
				BufferedImage.TYPE_BYTE_INDEXED);
		sourceConverted.getGraphics().drawImage(source, 0, 0, null);

		final BufferedImage matted = new BufferedImage(width, height,
				BufferedImage.TYPE_BYTE_INDEXED);

		final BufferedImage matte = new BufferedImage(width, height,
				BufferedImage.TYPE_BYTE_INDEXED);
		final int matteRgb = matteColor.getRGB();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				matte.setRGB(x, y, matteRgb);
			}
		}

		CompositeContext context = AlphaComposite.DstOver.createContext(matte
				.getColorModel(), sourceConverted.getColorModel(), null);
		context.compose(matte.getRaster(), sourceConverted.getRaster(), matted
				.getRaster());

		return sourceConverted;
	}
}
