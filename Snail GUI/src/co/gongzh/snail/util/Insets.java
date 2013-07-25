package co.gongzh.snail.util;

import java.io.Serializable;

public final class Insets implements Serializable, Cloneable {

	private static final long serialVersionUID = 8569666996003603037L;
	
	public int top;
	public int left;
	public int bottom;
	public int right;
	
	public static Insets make() {
		return new Insets(0, 0, 0, 0);
	}
	
	public static Insets make(int top, int left, int bottom, int right) {
		return new Insets(top, left, bottom, right);
	}
	
	private Insets(int top, int left, int bottom, int right) {
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
	}
	
	public Insets set(int top, int left, int bottom, int right) {
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
		return this;
	}
	
	public Insets set(Insets insets) {
		this.top = insets.top;
		this.left = insets.left;
		this.bottom = insets.bottom;
		this.right = insets.right;
		return this;
	}
	
	@Override
	public int hashCode() {
		return top + left << 8 + bottom << 16 + right << 24;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Insets) {
			Insets insets = (Insets) obj;
			return insets.top == top &&
					insets.left == left &&
					insets.bottom == bottom &&
					insets.right == right;
		} else {
			return false;
		}
	}
	
	@Override
	public Insets clone() {
		return Insets.make(top, left, bottom, right);
	}
	
	@Override
	public String toString() {
		return String.format("Insets[top=%d,left=%d,bottom=%d,right=%d]",top, left, bottom, right);
	}
	
}
