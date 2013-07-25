package co.gongzh.snail.util;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 * Generally used 2D-vector, which is based on integer.
 * Creates instance by static factory methods.
 * <p>
 * Note that the <code>Vector2D</code> instance is mutable.
 * @see Vector2D#make(int, int)
 * @author Gong Zhang
 */
public final class Vector2D implements Cloneable, Serializable {
	
	private static final long serialVersionUID = -6773220733288650023L;
	
	public int x;
	public int y;
	
	public static Vector2D make() {
		return new Vector2D();
	}
	
	public static Vector2D make(int x, int y) {
		return new Vector2D().set(x, y);
	}
	
	public static Vector2D make(Vector2D v) {
		return new Vector2D().set(v);
	}
	
	public static Vector2D make(Point p) {
		return new Vector2D().set(p);
	}
	
	public static Vector2D make(Point2D p) {
		return new Vector2D().set(p);
	}
	
	public static Vector2D make(double x, double y) {
		return new Vector2D().set(x, y);
	}
	
	/**
	 * Creates a new <code>Vector2D</code> instance 
	 * which reflects the result of "<code>v1 + v2</code>".
	 */
	public static Vector2D add(Vector2D v1, Vector2D v2) {
		return new Vector2D().set(v1.x + v2.x, v1.y + v2.y);
	}
	
	/**
	 * Creates a new <code>Vector2D</code> instance 
	 * which reflects the result of "<code>v1 - v2</code>".
	 */
	public static Vector2D subtract(Vector2D v1, Vector2D v2) {
		return new Vector2D().set(v1.x - v2.x, v1.y - v2.y);
	}
	
	/**
	 * Creates a new <code>Vector2D</code> instance 
	 * which reflects the result of "<code>v * k</code>".
	 */
	public static Vector2D multiplied(Vector2D v, double k) {
		return new Vector2D().set(v.x * k, v.y * k);
	}
	
	/**
	 * Creates a new <code>Vector2D</code> instance 
	 * which reflects the result of "<code>-v</code>".
	 */
	public static Vector2D reverse(Vector2D v) {
		return new Vector2D().set(-v.x, -v.y);
	}
	
	//// Constructors ////
	
	private Vector2D() {
	}

	//// Operation Methods ////
	
	/**
	 * Resets the vector by specified parameters.
	 * @param v
	 * @return this
	 */
	public Vector2D set(Vector2D v) {
		this.x = v.x;
		this.y = v.y;
		return this;
	}
	
	/**
	 * Resets the vector by specified parameters.
	 * @param x
	 * @param y
	 * @return this
	 */
	public Vector2D set(int x, int y) {
		this.x = x;
		this.y = y;
		return this;
	}
	
	/**
	 * Resets the vector by specified parameters.
	 * @param point
	 * @return this
	 */
	public Vector2D set(Point point) {
		this.x = point.x;
		this.y = point.y;
		return this;
	}
	
	/**
	 * Resets the vector by specified parameters.
	 * @param point
	 * @return this
	 */
	public Vector2D set(Point2D point) {
		this.x = (int) point.getX();
		this.y = (int) point.getY();
		return this;
	}
	
	/**
	 * Resets the vector by specified parameters.
	 * @param x
	 * @param y
	 * @return this
	 */
	public Vector2D set(double x, double y) {
		this.x = (int) x;
		this.y = (int) y;
		return this;
	}
	
	/**
	 * Increases this vector by specified vector <code>v</code>.
	 * @param v
	 */
	public void increase(Vector2D v) {
		x += v.x;
		y += v.y;
	}
	
	/**
	 * Decreases this vector by specified vector <code>v</code>.
	 * @param v
	 */
	public void decrease(Vector2D v) {
		x -= v.x;
		y -= v.y;
	}
	
	/**
	 * Multiplies this vector by specified parameter <code>k</code>.
	 * @param k
	 */
	public void multiplied(double k) {
		set(x * k, y * k);
	}
	
	/**
	 * Reverses this vector.
	 */
	public void reverse() {
		x = -x;
		y = -y;
	}
	
	//// Conversion Methods ////
	
	public Point toPoint() {
		return new Point(x, y);
	}
	
	public Point2D.Float toPoint2DFloat() {
		return new Point2D.Float(x, y);
	}
	
	public Point2D.Double toPoint2D() {
		return new Point2D.Double(x, y);
	}
	
	//// Overriding Methods ////
	
	@Override
	public int hashCode() {
		return x + (y << 16);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Vector2D) {
			Vector2D vector = (Vector2D) obj;
			return x == vector.x && y == vector.y;
		} else {
			return false;
		}
	}
	
	@Override
	public Vector2D clone() {
		return new Vector2D().set(this);
	}
	
	@Override
	public String toString() {
		return String.format("Vector2D[x=%d,y=%d]", x, y);
	}
	
}
