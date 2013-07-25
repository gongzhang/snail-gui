package co.gongzh.snail.util;
import java.io.Serializable;


public final class Range implements Serializable, Cloneable, Comparable<Range> {

	private static final long serialVersionUID = 2809656740497414241L;
	
	public int offset;
	public int length;
	
	public static Range make(int offset, int length) {
		return new Range(offset, length);
	}
	
	public Range(int offset, int length) {
		this.offset = offset;
		this.length = length;
	}
	
	public boolean contains(int offset) {
		return offset >= this.offset && offset < this.offset + this.length;
	}
	
	public void set(int offset, int length) {
		this.offset = offset;
		this.length = length;
	}
	
	@Override
	public int hashCode() {
		return offset ^ length;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Range) {
			Range range = (Range) obj;
			return range.offset == this.offset && range.length == this.length;
		} else {
			return false;
		}
	}
	
	@Override
	public Range clone() {
		return Range.make(offset, length);
	}
	
	@Override
	public String toString() {
		return String.format("Range[offset=%d,length=%d]", offset, length);
	}

	@Override
	public int compareTo(Range range) {
		int rst = this.offset - range.offset;
		return rst != 0 ? rst : this.length - range.length;
	}
	
}
