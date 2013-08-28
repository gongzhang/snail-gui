package co.gongzh.snail.text;


final public class TextChangeEvent {
	
	public static enum TextChangeType {
		INSERT, DELETE
	}
	
	public final TextChangeType type;
	public final int offset;
	public final String text;
	
	public TextChangeEvent(TextChangeType type, int offset, String text) {
		this.type = type;
		this.offset = offset;
		this.text = text;
	}
	
	@Override
	public String toString() {
		return String.format("[%s at %d: \"%s\"]", type == TextChangeType.INSERT ? "INSERT" : "DELETE", offset, text);
	}
	
}
