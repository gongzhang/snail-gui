package co.gongzh.snail;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;


public final class ViewGraphics extends Graphics2D {
	
	private static class AlphaCompositePool {
		final AlphaComposite[] composites = new AlphaComposite[256];
		{
			composites[0] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f);
			composites[255] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
		}
		AlphaComposite getAlphaComposite(final float alpha) {
			int i = (int) (alpha * 255.0f);
			i = i < 0 ? 0 : i > 255 ? 255 : i;
			if (composites[i] == null) {
				composites[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
			}
			return composites[i];
		}
	}
	
	private final Graphics2D g2;
	private float current_alpha;
	private static final AlphaCompositePool alphaCompositePool = new AlphaCompositePool();
	
	public ViewGraphics(Graphics2D g2) {
		this.g2 = g2;
		current_alpha = 1.0f;
	}
	
	public final Graphics2D getGraphics2D() {
		return g2;
	}
	
	float getAlpha() {
		return current_alpha;
	}
	
	void setAlpha(float alpha) {
		this.current_alpha = alpha;
		g2.setComposite(alphaCompositePool.getAlphaComposite(alpha));
	}
	
	public void drawImage(Image img, int left, int top) {
		img.paintOnScreen(g2, left, top, img.width, img.height);
	}
	
	public void drawImage(Image img, int left, int top, int width, int height) {
		img.paintOnScreen(g2, left, top, width, height);
	}

	@Override
	public void addRenderingHints(Map<?, ?> hints) {
		g2.addRenderingHints(hints);
	}

	@Override
	public void clip(Shape s) {
		g2.clip(s);
	}

	@Override
	public void draw(Shape s) {
		g2.draw(s);
	}

	@Override
	public void drawGlyphVector(GlyphVector g, float x, float y) {
		g2.drawGlyphVector(g, x, y);
	}
	
	public final boolean drawImage(java.awt.Image img, AffineTransform xform) {
		return this.drawImage(img, xform, null);
	}

	@Override
	public boolean drawImage(java.awt.Image img, AffineTransform xform, ImageObserver obs) {
		return g2.drawImage(img, xform, obs);
	}

	@Override
	public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
		g2.drawImage(img, op, x, y);
	}

	@Override
	public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
		g2.drawRenderableImage(img, xform);
	}

	@Override
	public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
		g2.drawRenderedImage(img, xform);
	}

	@Override
	public void drawString(String str, int x, int y) {
		g2.drawString(str, x, y);
	}

	@Override
	public void drawString(String str, float x, float y) {
		g2.drawString(str, x, y);
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, int x, int y) {
		g2.drawString(iterator, x, y);
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, float x, float y) {
		g2.drawString(iterator, x, y);
	}

	@Override
	public void fill(Shape s) {
		g2.fill(s);
	}

	@Override
	public Color getBackground() {
		return g2.getBackground();
	}

	@Override
	public Composite getComposite() {
		return g2.getComposite();
	}

	@Override
	public GraphicsConfiguration getDeviceConfiguration() {
		return g2.getDeviceConfiguration();
	}

	@Override
	public FontRenderContext getFontRenderContext() {
		return g2.getFontRenderContext();
	}

	@Override
	public Paint getPaint() {
		return g2.getPaint();
	}

	@Override
	public Object getRenderingHint(Key hintKey) {
		return g2.getRenderingHint(hintKey);
	}

	@Override
	public RenderingHints getRenderingHints() {
		return g2.getRenderingHints();
	}

	@Override
	public Stroke getStroke() {
		return g2.getStroke();
	}

	@Override
	public AffineTransform getTransform() {
		return g2.getTransform();
	}

	@Override
	public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
		return g2.hit(rect, s, onStroke);
	}

	@Override
	public void rotate(double theta) {
		g2.rotate(theta);
	}

	@Override
	public void rotate(double theta, double x, double y) {
		g2.rotate(theta, x, y);
	}

	@Override
	public void scale(double sx, double sy) {
		g2.scale(sx, sy);
	}

	@Override
	public void setBackground(Color color) {
		g2.setBackground(color);
	}

	@Override
	public void setComposite(Composite comp) {
		g2.setComposite(comp);
	}

	@Override
	public void setPaint(Paint paint) {
		g2.setPaint(paint);
	}

	@Override
	public void setRenderingHint(Key hintKey, Object hintValue) {
		g2.setRenderingHint(hintKey, hintValue);
	}

	@Override
	public void setRenderingHints(Map<?, ?> hints) {
		g2.setRenderingHints(hints);
	}

	@Override
	public void setStroke(Stroke s) {
		g2.setStroke(s);
	}

	@Override
	public void setTransform(AffineTransform Tx) {
		g2.setTransform(Tx);
	}

	@Override
	public void shear(double shx, double shy) {
		g2.shear(shx, shy);
	}

	@Override
	public void transform(AffineTransform Tx) {
		g2.transform(Tx);
	}

	@Override
	public void translate(int x, int y) {
		g2.translate(x, y);
	}

	@Override
	public void translate(double tx, double ty) {
		g2.translate(tx, ty);
	}

	@Override
	public void clearRect(int x, int y, int width, int height) {
		g2.clearRect(x, y, width, height);
	}

	@Override
	public void clipRect(int x, int y, int width, int height) {
		g2.clipRect(x, y, width, height);
	}
	
	@Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
		g2.copyArea(x, y, width, height, dx, dy);
	}

	@Override
	public Graphics create() {
		return g2.create();
	}

	@Override
	public void dispose() {
		g2.dispose();
	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		g2.drawArc(x, y, width, height, startAngle, arcAngle);
	}

	@Override
	public boolean drawImage(java.awt.Image img, int x, int y, ImageObserver observer) {
		return g2.drawImage(img, x, y, observer);
	}
	
	public final boolean drawImage(java.awt.Image img, int x, int y) {
		return this.drawImage(img, x, y, null);
	}

	@Override
	public boolean drawImage(java.awt.Image img, int x, int y, Color bgcolor, ImageObserver observer) {
		return g2.drawImage(img, x, y, bgcolor, observer);
	}

	@Override
	public boolean drawImage(java.awt.Image img, int x, int y, int width, int height, ImageObserver observer) {
		return g2.drawImage(img, x, y, width, height, observer);
	}
	
	public final boolean drawImage(java.awt.Image img, int x, int y, int width, int height) {
		return this.drawImage(img, x, y, width, height, null);
	}

	@Override
	public boolean drawImage(java.awt.Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
		return g2.drawImage(img, x, y, width, height, bgcolor, observer);
	}

	@Override
	public boolean drawImage(java.awt.Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
		return g2.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
	}

	@Override
	public boolean drawImage(java.awt.Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
		return g2.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		g2.drawLine(x1, y1, x2, y2);
	}

	@Override
	public void drawOval(int x, int y, int width, int height) {
		g2.drawOval(x, y, width, height);
	}

	@Override
	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		g2.drawPolygon(xPoints, yPoints, nPoints);
	}

	@Override
	public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
		g2.drawPolyline(xPoints, yPoints, nPoints);
	}

	@Override
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		g2.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
	}

	@Override
	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		g2.fillArc(x, y, width, height, startAngle, arcAngle);
	}

	@Override
	public void fillOval(int x, int y, int width, int height) {
		g2.fillOval(x, y, width, height);
	}

	@Override
	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		g2.fillPolygon(xPoints, yPoints, nPoints);
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		g2.fillRect(x, y, width, height);
	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		g2.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
	}

	@Override
	public Shape getClip() {
		return g2.getClip();
	}

	@Override
	public Rectangle getClipBounds() {
		return g2.getClipBounds();
	}

	@Override
	public Color getColor() {
		return g2.getColor();
	}

	@Override
	public Font getFont() {
		return g2.getFont();
	}

	@Override
	public FontMetrics getFontMetrics(Font f) {
		return g2.getFontMetrics(f);
	}

	@Override
	public void setClip(Shape clip) {
		g2.setClip(clip);
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
		g2.setClip(x, y, width, height);
	}

	@Override
	public void setColor(Color c) {
		g2.setColor(c);
	}

	@Override
	public void setFont(Font font) {
		g2.setFont(font);
	}

	@Override
	public void setPaintMode() {
		g2.setPaintMode();
	}

	@Override
	public void setXORMode(Color c1) {
		g2.setXORMode(c1);
	}

}
