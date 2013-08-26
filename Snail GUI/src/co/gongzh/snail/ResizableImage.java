package co.gongzh.snail;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;


public class ResizableImage extends Image {
	
	protected final int x1, x2, y1, y2;
	
	public ResizableImage(GraphicsConfiguration gc, ImageSourceLoader loader, int x1, int y1, int x2, int y2) {
		super(gc, loader);
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
	}
	
	public ResizableImage(GraphicsConfiguration gc, ImageSourceLoader loader, int capWidth, int capHeight) {
		super(gc, loader);
		this.x1 = capWidth;
		this.x2 = this.width - capWidth;
		this.y1 = capHeight;
		this.y2 = this.height - capHeight;
	}
	
	public ResizableImage(GraphicsConfiguration gc, ImageSourceLoader loader, int capThickness) {
		super(gc, loader);
		this.x1 = capThickness;
		this.x2 = this.width - capThickness;
		this.y1 = capThickness;
		this.y2 = this.height - capThickness;
	}
	
	@Override
	protected void paint(Graphics2D g, int left, int top, int w, int h) {
		if (w <= 0 || h <= 0) return;
		
		int sx1 = this.x1;
		int sx2 = this.x2;
		int sy1 = this.y1;
		int sy2 = this.y2;
		
		int x1 = this.x1;
		int x2 = w - width + this.x2;
		if (x2 < x1) {
			sx1 = x1 = x2 = w / 2;
			sx2 = width - sx1;
		}
		
		int y1 = this.y1;
		int y2 = h - height + this.y2;
		if (y2 < y1) {
			sy1 = y1 = y2 = h / 2;
			sy2 = height - sy1;
		}
		
		// translate back
		if (left != 0 || top != 0) {
			g.translate(left, top);
		}
		
		// four corners
		g.drawImage(vImg, 0, 0, x1, y1, 0, 0, sx1, sy1, null);
		g.drawImage(vImg, x2, 0, w, y1, sx2, 0, width, sy1, null);
		g.drawImage(vImg, 0, y2, x1, h, 0, sy2, sx1, height, null);
		g.drawImage(vImg, x2, y2, w, h, sx2, sy2, width, height, null);
		
		// four borders
		if (x2 > x1) {
			g.drawImage(vImg, x1, 0, x2, y1, sx1, 0, sx2, sy1, null);
			g.drawImage(vImg, x1, y2, x2, h, sx1, sy2, sx2, height, null);
		}
		if (y2 > y1) {
			g.drawImage(vImg, 0, y1, x1, y2, 0, sy1, sx1, sy2, null);
			g.drawImage(vImg, x2, y1, w, y2, sx2, sy1, width, sy2, null);
		}
		
		// center
		if (x2 > x1 && y2 > y1) {
			g.drawImage(vImg, x1, y1, x2, y2, sx1, sy1, sx2, sy2, null);
		}
		
		// translate back
		if (left != 0 || top != 0) {
			g.translate(-left, -top);
		}
	}
	
}
