package co.gongzh.snail;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;


public class Image {
	
	public static interface ImageSourceLoader {
		public BufferedImage loadImage();
		public void unloadImage(BufferedImage image);
	}

	protected final GraphicsConfiguration gc;
	protected VolatileImage vImg;
	public final int width, height;
	protected final ImageSourceLoader loader;

	public Image(GraphicsConfiguration gc, ImageSourceLoader loader) {
		this.gc = gc;
		this.loader = loader;
		BufferedImage img = loader.loadImage();
		width = img.getWidth();
		height = img.getHeight();
		vImg = gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
		renderVolatileImageOffscreen(img);
		loader.unloadImage(img);
	}
	
	private void renderVolatileImageOffscreen(BufferedImage img) {
		do {
            if (vImg.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
                vImg = gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
            }
            Graphics2D g = vImg.createGraphics();
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC);
			g.setComposite(ac);
            g.drawImage(img, 0, 0, null);
            g.dispose();
        } while (vImg.contentsLost());
	}
	
	public final void paintOnScreen(Graphics2D g, int left, int top, int w, int h) {
		BufferedImage img = null;
		do {
	        int returnCode = vImg.validate(gc);
	        if (returnCode == VolatileImage.IMAGE_RESTORED) {
	        	if (img == null) img = loader.loadImage();
	            renderVolatileImageOffscreen(img);
	        } else if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
	        	if (img == null) img = loader.loadImage();
	            vImg = gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
	            renderVolatileImageOffscreen(img);
	        }
	        paint(g, left, top, w, h);
		} while (vImg.contentsLost());
		if (img != null) {
			loader.unloadImage(img);
		}
	}
	
	protected void paint(Graphics2D g, int left, int top, int width, int height) {
		g.drawImage(vImg, left, top, width, height, null);
	}

}
