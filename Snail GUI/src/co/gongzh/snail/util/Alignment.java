package co.gongzh.snail.util;

public final class Alignment {
	
	public static final Alignment LEFT_TOP = new Alignment(Horizontal.LEFT, Vertical.TOP);
	public static final Alignment LEFT_CENTER = new Alignment(Horizontal.LEFT, Vertical.CENTER);
	public static final Alignment LEFT_BOTTOM = new Alignment(Horizontal.LEFT, Vertical.BOTTOM);
	
	public static final Alignment CENTER_TOP = new Alignment(Horizontal.CENTER, Vertical.TOP);
	public static final Alignment CENTER_CENTER = new Alignment(Horizontal.CENTER, Vertical.CENTER);
	public static final Alignment CENTER_BOTTOM = new Alignment(Horizontal.CENTER, Vertical.BOTTOM);
	
	public static final Alignment RIGHT_TOP = new Alignment(Horizontal.RIGHT, Vertical.TOP);
	public static final Alignment RIGHT_CENTER = new Alignment(Horizontal.RIGHT, Vertical.CENTER);
	public static final Alignment RIGHT_BUTTOM = new Alignment(Horizontal.RIGHT, Vertical.BOTTOM);
	
	public static enum Horizontal {
		LEFT("left"), CENTER("center"), RIGHT("right");
		private final String string;
		private Horizontal(String string) {
			this.string = string;
		}
		@Override
		public String toString() {
			return string;
		}
	}
	
	public static enum Vertical {
		TOP("top"), CENTER("center"), BOTTOM("bottom");
		private final String string;
		private Vertical(String string) {
			this.string = string;
		}
		@Override
		public String toString() {
			return string;
		}
	}
	
	private final Horizontal horizontal;
	private final Vertical vertical;
	
	private Alignment(Horizontal horizontal, Vertical vertical) {
		this.horizontal = horizontal;
		this.vertical = vertical;
	}
	
	public Horizontal horizontal() {
		return horizontal;
	}
	
	public Vertical vertical() {
		return vertical;
	}
	
	@Override
	public String toString() {
		return String.format("[%s&%s]", horizontal, vertical);
	}
	
}
