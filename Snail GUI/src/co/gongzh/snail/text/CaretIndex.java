package co.gongzh.snail.text;

public final class CaretIndex {

	public static CaretIndex before(int charIndex) {
		return new CaretIndex(true, charIndex);
	}
	
	public static CaretIndex after(int charIndex) {
		return new CaretIndex(false, charIndex);
	}
	
	public final boolean before;
	public final int charIndex;
	
	private CaretIndex(boolean before, int charIndex) {
		this.before = before;
		this.charIndex = charIndex;
	}
	
	public CaretIndex other() {
		if (before) return toAfter();
		else return toBefore();
	}
	
	public CaretIndex toBefore() {
		if (!before) return CaretIndex.before(charIndex + 1);
		else return this;
	}
	
	public CaretIndex toAfter() {
		if (before) return CaretIndex.after(charIndex - 1);
		else return this;
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s:%d]", super.toString(), before ? "before" : "after", charIndex);
	}
	
	@Override
	public int hashCode() {
		return charIndex * (before ? 1 : -1);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CaretIndex) {
			CaretIndex index = (CaretIndex) obj;
			return before == index.before && charIndex == index.charIndex;
		} else {
			return false;
		}
	}
	
}
