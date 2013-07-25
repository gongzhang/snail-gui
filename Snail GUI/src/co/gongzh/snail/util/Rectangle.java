package co.gongzh.snail.util;

import java.io.Serializable;


public final class Rectangle implements Serializable, Cloneable {
	
	private static final long serialVersionUID = 2458912050755833092L;
	
	public final Vector2D origin;
	public final Vector2D size;
	
	public static Rectangle make() {
		return new Rectangle(0, 0, 0, 0);
	}
	
	public static Rectangle make(int x, int y, int width, int height) {
		return new Rectangle(x, y, width, height);
	}
	
	private Rectangle(int x, int y, int w, int h) {
		origin = Vector2D.make(x, y);
		size = Vector2D.make(w, h);
	}
	
	public Rectangle set(int x, int y, int width, int height) {
		origin.x = x;
		origin.y = y;
		size.x = width;
		size.y = height;
		return this;
	}
	
	public Rectangle set(Rectangle rect) {
		origin.x = rect.origin.x;
		origin.y = rect.origin.y;
		size.x = rect.size.x;
		size.y = rect.size.y;
		return this;
	}
	
	@Override
	public int hashCode() {
		return origin.x + origin.y << 8 + size.x << 16 + size.y << 24;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Rectangle) {
			Rectangle rect = (Rectangle) obj;
			return rect.origin.equals(origin) &&
				rect.size.equals(size);
		} else {
			return false;
		}
	}
	
	@Override
	public Rectangle clone() {
		return Rectangle.make(origin.x, origin.y, size.x, size.y);
	}
	
	@Override
	public String toString() {
		return String.format("Rectangle[origin=(%d,%d),size=(%d,%d)]", origin.x, origin.y, size.x, size.y);
	}
	
}
