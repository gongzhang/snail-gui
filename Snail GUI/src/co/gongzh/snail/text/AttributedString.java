package co.gongzh.snail.text;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.Serializable;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import co.gongzh.snail.util.FlyweightList;
import co.gongzh.snail.util.Range;

public final class AttributedString implements Serializable, Cloneable {
	
	private static final long serialVersionUID = 6301513983262513946L;
	
	private final StringBuilder string;
	private final FlyweightList<Font> fonts;
	private final FlyweightList<Color> colors;
	
	public AttributedString() {
		string = new StringBuilder();
		fonts = new FlyweightList<Font>();
		colors = new FlyweightList<Color>();
	}
	
	public AttributedString(AttributedString str) {
		this();
		insert(0, str);
	}
	
	public AttributedString(String str, Font font, Color color) {
		this();
		insert(0, str, font, color);
	}
	
	public Range append(String str, Font font, Color color) {
		return insert(string.length(), str, font, color);
	}
	
	public Range append(AttributedString str) {
		return insert(string.length(), str);
	}
	
	public Range insert(int index, AttributedString str) {
		Range range = Range.make(index, str.length());
		string.insert(index, str.string);
		fonts.insert(index, str.fonts);
		colors.insert(index, str.colors);
		return range;
	}
	
	public Range insert(int index, String str, Font font, Color color) {
		Range range = Range.make(index, str.length());
		string.insert(index, str);
		fonts.add(range, font);
		colors.add(range, color);
		return range;
	}
	
	public void delete(Range range) {
		string.delete(range.offset, range.offset + range.length);
		fonts.remove(range);
		colors.remove(range);
	}
	
	public void clear() {
		string.delete(0, string.length());
		fonts.clear();
		colors.clear();
	}

	public int length() {
		return string.length();
	}

	public char charAt(int index) {
		return string.charAt(index);
	}

	public AttributedString substring(Range range) {
		AttributedString str = new AttributedString();
		str.string.append(string.substring(range.offset, range.offset + range.length));
		str.fonts.set(fonts.subList(range));
		str.colors.set(colors.subList(range));
		return str;
	}

	public Font getFont(int index) {
		return fonts.get(index);
	}
	
	public void setFont(Range range, Font font) {
		fonts.set(range, font);
	}
	
	public Color getColor(int index) {
		return colors.get(index);
	}
	
	public void setColor(Range range, Color color) {
		colors.set(range, color);
	}
	
	public Iterator iterator() {
		return new NormalIterator(this);
	}
	
	public Iterator iterator(Range range) {
		if (range.offset < 0 || range.length < 0 || range.offset + range.length > length()) {
			throw new IndexOutOfBoundsException();
		}
		return new SubIterator(new NormalIterator(this), range.offset, range.length);
	}

	@Override
	public AttributedString clone() {
		AttributedString str = new AttributedString();
		str.string.append(string);
		str.fonts.set(fonts);
		str.colors.set(colors);
		return str;
	}
	
	@Override
	public String toString() {
		return string.toString();
	}
	
	@Override
	public int hashCode() {
		return string.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AttributedString) {
			AttributedString str = (AttributedString) obj;
			return str.string.equals(string) &&
					str.fonts.equals(fonts) &&
					str.colors.equals(colors);
		} else {
			return false;
		}
	}
	
	public static interface Iterator extends AttributedCharacterIterator, Cloneable {
		
		public Font getFont();
		
		public Color getColor();
		
		public int length();
		
		public Iterator iterator(Range range);
		
		@Override
		public Iterator clone();
		
	}
	
	private static final class NormalIterator implements Iterator {

		private final AttributedString string;
		private int ptr;
		
		private final Range fontRange;
		private Font font;
		private final Range colorRange;
		private Color color;
		
		private NormalIterator(AttributedString target) {
			string = target;
			ptr = 0;
			fontRange = Range.make(0, 0);
			colorRange = Range.make(0, 0);
			font = null;
			color = null;
			if (string.length() > 0) {
				font = string.fonts.get(0, fontRange);
				color = string.colors.get(0, colorRange);
			}
		}
		
		private void setPointer(int new_ptr) {
			if (new_ptr >= 0 && new_ptr < string.length()) {
				if (!fontRange.contains(new_ptr)) {
					font = string.fonts.get(new_ptr, fontRange);
				}
				if (!colorRange.contains(new_ptr)) {
					color = string.colors.get(new_ptr, colorRange);
				}
				ptr = new_ptr;
			} else {
				ptr = string.length();
				fontRange.set(ptr, 1);
				font = null;
				colorRange.set(ptr, 1);
				color = null;
			}
		}
		
		public Font getFont() {
			return (Font) getAttribute(TextAttribute.FONT);
		}

		public Color getColor() {
			return (Color) getAttribute(TextAttribute.FOREGROUND);
		}

		public int length() {
			return getEndIndex();
		}

		@Override
		public char setIndex(int position) {
			if (position == string.length()) {
				setPointer(position);
				return DONE;
			} else if (position >= 0 && position < string.length()) {
				setPointer(position);
				return string.charAt(position);
			} else {
				throw new IllegalArgumentException();
			}
		}
		
		@Override
		public char current() {
			if (ptr == string.length()) return DONE;
			else return string.charAt(ptr);
		}

		@Override
		public char first() {
			return setIndex(getBeginIndex());
		}

		@Override
		public int getBeginIndex() {
			return 0;
		}

		@Override
		public int getEndIndex() {
			return string.length();
		}

		@Override
		public int getIndex() {
			return ptr;
		}

		@Override
		public char last() {
			if (getBeginIndex() != getEndIndex()) {
				return setIndex(getEndIndex() - 1);
			} else {
				return setIndex(getEndIndex());
			}
		}

		@Override
		public char next() {
			int new_ptr = ptr + 1;
			if (new_ptr >= getEndIndex()) {
				setIndex(getEndIndex());
				return DONE;
			} else {
				return setIndex(new_ptr);
			}
		}

		@Override
		public char previous() {
			int new_ptr = ptr - 1;
			if (new_ptr < getBeginIndex()) {
				setIndex(getBeginIndex());
				return DONE;
			} else {
				return setIndex(new_ptr);
			}
		}

		@Override
		public Set<Attribute> getAllAttributeKeys() {
			HashSet<Attribute> set = new HashSet<Attribute>();
			if (string.fonts.hasNonNullValues()) set.add(TextAttribute.FONT);
			if (string.colors.hasNonNullValues()) set.add(TextAttribute.FOREGROUND);
			return set;
		}

		@Override
		public Object getAttribute(Attribute attribute) {
			if (attribute == TextAttribute.FONT) {
				return font;
			} else if (attribute == TextAttribute.FOREGROUND) {
				return color;
			} else {
				return null;
			}
		}
		
		@Override
		public Map<Attribute, Object> getAttributes() {
			HashMap<Attribute, Object> map = new HashMap<Attribute, Object>();
			if (font != null) map.put(TextAttribute.FONT, font);
			if (color != null) map.put(TextAttribute.FOREGROUND, color);
			return map;
		}

		@Override
		public int getRunLimit() {
			return getRunLimit(getAllAttributeKeys());
		}

		@Override
		public int getRunLimit(Attribute attribute) {
			if (attribute == TextAttribute.FONT) {
				return fontRange.offset + fontRange.length;
			} else if (attribute == TextAttribute.FOREGROUND) {
				return colorRange.offset + colorRange.length;
			} else {
				return ptr + 1;
			}
		}

		@Override
		public int getRunLimit(Set<? extends Attribute> attributes) {
			int limit = string.length();
			for (Attribute attr : attributes) {
				int l = getRunLimit(attr);
				if (l < limit) limit = l;
			}
			return limit;
		}

		@Override
		public int getRunStart() {
			return getRunStart(getAllAttributeKeys());
		}

		@Override
		public int getRunStart(Attribute attribute) {
			if (attribute == TextAttribute.FONT) {
				return fontRange.offset;
			} else if (attribute == TextAttribute.FOREGROUND) {
				return colorRange.offset;
			} else {
				return ptr;
			}
		}

		@Override
		public int getRunStart(Set<? extends Attribute> attributes) {
			int start = 0;
			for (Attribute attr : attributes) {
				int s = getRunStart(attr);
				if (s > start) start = s;
			}
			return start;
		}
		
		@Override
		public NormalIterator clone() {
			NormalIterator it = new NormalIterator(string);
			it.ptr = ptr;
			it.fontRange.set(fontRange.offset, fontRange.length);
			it.font = font;
			it.colorRange.set(colorRange.offset, colorRange.length);
			it.color = color;
			return it;
		}

		@Override
		public Iterator iterator(Range range) {
			return string.iterator(range);
		}
		
	}

	private static final class SubIterator implements Iterator {
	
		private final NormalIterator target;
		private final int offset;
		private final int length;
		
		private SubIterator(NormalIterator target, int offset, int length) {
			this.target = target;
			this.offset = offset;
			this.length = length;
			target.setIndex(offset);
		}
		
		public Font getFont() {
			return (Font) getAttribute(TextAttribute.FONT);
		}

		public Color getColor() {
			return (Color) getAttribute(TextAttribute.FOREGROUND);
		}

		public int length() {
			return getEndIndex();
		}
		
		@Override
		public char current() {
			return target.current();
		}
	
		@Override
		public char first() {
			if (length == 0) {
				setIndex(getBeginIndex());
				return DONE;
			} else {
				return setIndex(getBeginIndex());
			}
		}
	
		@Override
		public int getBeginIndex() {
			return 0;
		}
	
		@Override
		public int getEndIndex() {
			return length;
		}
	
		@Override
		public int getIndex() {
			return target.getIndex() - offset;
		}
	
		@Override
		public char last() {
			if (length == 0) {
				setIndex(getEndIndex());
				return DONE;
			} else {
				return setIndex(getEndIndex() - 1);
			}
		}
	
		@Override
		public char next() {
			if (getIndex() >= length - 1) {
				setIndex(length);
				return DONE;
			} else {
				return target.next();
			}
		}
	
		@Override
		public char previous() {
			if (getIndex() <= 0) {
				setIndex(getBeginIndex());
				return DONE;
			} else {
				return target.previous();
			}
		}
	
		@Override
		public char setIndex(int arg0) {
			return target.setIndex(arg0 + offset);
		}
	
		@Override
		public Set<Attribute> getAllAttributeKeys() {
			return target.getAllAttributeKeys();
		}
	
		@Override
		public Object getAttribute(Attribute attribute) {
			return target.getAttribute(attribute);
		}
	
		@Override
		public Map<Attribute, Object> getAttributes() {
			return target.getAttributes();
		}
	
		@Override
		public int getRunLimit() {
			return normalizeIndex(target.getRunLimit() - offset);
		}
	
		@Override
		public int getRunLimit(Attribute attribute) {
			return normalizeIndex(target.getRunLimit(attribute) - offset);
		}
	
		@Override
		public int getRunLimit(Set<? extends Attribute> attributes) {
			return normalizeIndex(target.getRunLimit(attributes) - offset);
		}
	
		@Override
		public int getRunStart() {
			return normalizeIndex(target.getRunStart() - offset);
		}
	
		@Override
		public int getRunStart(Attribute attribute) {
			return normalizeIndex(target.getRunStart(attribute) - offset);
		}
	
		@Override
		public int getRunStart(Set<? extends Attribute> attributes) {
			return normalizeIndex(target.getRunStart(attributes) - offset);
		}
		
		@Override
		public Iterator clone() {
			Iterator it = target.string.iterator(Range.make(offset, length));
			it.setIndex(this.getIndex());
			return it;
		}
		
		private int normalizeIndex(int index) {
			if (index < 0) return 0;
			else if (index > length) return length;
			else return index;
		}

		@Override
		public Iterator iterator(Range range) {
			return target.string.iterator(Range.make(offset + range.offset, range.length));
		}
		
	}
	
}
