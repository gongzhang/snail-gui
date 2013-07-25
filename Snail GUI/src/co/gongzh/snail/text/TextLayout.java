package co.gongzh.snail.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextHitInfo;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

import co.gongzh.snail.text.AttributedString.Iterator;
import co.gongzh.snail.util.Alignment;
import co.gongzh.snail.util.Range;
import co.gongzh.snail.util.Vector2D;

public class TextLayout implements ITextLayout {
	
	private static class SingleLineTextLayout {
		
		int x = 0, y = 0; // relative to (0, 0), not (0, baseline)
		final int width, height;
		final int ascent, descent;
		
		final int actualTextLength;
		
		/**
		 * The value is <code>null</code> when this line is empty.
		 */
		private final java.awt.font.TextLayout layout;
		
		/**
		 * Constructor for empty line.
		 */
		SingleLineTextLayout(FontRenderContext frc, Font font) {
			java.awt.font.TextLayout placeholder = new java.awt.font.TextLayout(new AttributedString("Mg", font, Color.black).iterator(), frc);
			layout = null;
			width = 0;
			ascent = (int) Math.ceil(placeholder.getAscent());
			descent = (int) Math.ceil(placeholder.getDescent());
			height = ascent + descent;
			actualTextLength = 0;
		}
		
		/**
		 * Constructor for non-empty line. The <code>text</code> is must not null.
		 */
		SingleLineTextLayout(java.awt.font.TextLayout layout, int actualTextLength) {
			this.layout = layout;
			width = (int) Math.ceil(layout.getAdvance());
			ascent = (int) Math.ceil(layout.getAscent());
			descent = (int) Math.ceil(layout.getDescent());
			height = this.ascent + (int) Math.ceil(layout.getDescent());
			this.actualTextLength = actualTextLength;
		}
		
		void draw(Graphics2D g) {
			if (layout != null) {
				layout.draw(g, x, y + ascent);
			}
		}
		
		int advanceAtCaretIndex(int caretIndex) {
			if (layout == null) return 0;
			TextHitInfo hit;
			if (caretIndex < 0) hit = TextHitInfo.beforeOffset(0);
			else if (caretIndex > actualTextLength) hit = TextHitInfo.beforeOffset(actualTextLength);
			else hit = TextHitInfo.beforeOffset(caretIndex);
			float[] info = layout.getCaretInfo(hit);
			return (int) Math.ceil(info[0]);
		}
		
		int caretIndexAtAdvance(int advance) {
			if (layout == null) return 0;
			TextHitInfo hit = layout.hitTestChar(advance, 0);
			return hit.getInsertionIndex();
		}
		
		int charIndexAtAdvance(int advance) {
			if (layout == null) return -1;
			TextHitInfo hit = layout.hitTestChar(advance, 0);
			return hit.getCharIndex();
		}
		
	}
	
	private final List<SingleLineTextLayout> lines;
	private final List<Integer> lineBeginIndexes;
	private final Vector2D size;
	
	private final int lineSpacing;
	private final int textLength;
	
	public TextLayout(
			Iterator text,
			FontRenderContext frc,
			Alignment.Horizontal alignment,
			int lineSpacing,
			Font initialFont) {
		this(text, frc, alignment, lineSpacing, initialFont, 0, null);
	}
	
	public TextLayout(
			Iterator text,
			FontRenderContext frc,
			Alignment.Horizontal alignment,
			int lineSpacing,
			Font initialFont,
			int widthLimit,
			BreakIterator breakIterator) {
		
		// splits the string with '\n'
		List<Iterator> paragraphs = new ArrayList<Iterator>(1);
		List<Integer> paragraphBeginIndexes = new ArrayList<Integer>(1);
		split(text, 0, paragraphs, paragraphBeginIndexes);
		
		// splits the string with line break measurer
		this.lines = new ArrayList<SingleLineTextLayout>(paragraphs.size());
		this.lineBeginIndexes = new ArrayList<Integer>(paragraphBeginIndexes.size());
		for (int i = 0; i < paragraphs.size(); i++) {
			// for each paragraph
			final Iterator paragraph = paragraphs.get(i);
			final int paragraphBeginIndex = paragraphBeginIndexes.get(i);
			if (paragraph.length() == 0) {
				// empty paragraph
				lines.add(new SingleLineTextLayout(frc, initialFont));
				lineBeginIndexes.add(paragraphBeginIndex);
			} else if (breakIterator == null) {
				// non-empty paragraph with out line break
				lines.add(new SingleLineTextLayout(new java.awt.font.TextLayout(paragraph, frc), paragraph.length()));
				lineBeginIndexes.add(paragraphBeginIndex);
				paragraph.last();
				initialFont = paragraph.getFont();
			} else {
				// non-empty paragraph
				Iterator it = paragraph.clone();
				LineBreakMeasurer measurer = new LineBreakMeasurer(paragraph, breakIterator, frc);
				int start = 0;
				while (start < paragraph.length()) {
					int next = measurer.nextOffset(widthLimit);
					int limit;
					
					// check tailing and heading whitespace
					if (Character.isWhitespace(it.setIndex(next - 1)) ||
						Character.isWhitespace(it.setIndex(start))) {
						limit = start + limitLength(it.iterator(Range.make(start, next - start)), frc, widthLimit);
					} else {
						limit = next;
					}

					final java.awt.font.TextLayout layout = measurer.nextLayout(widthLimit, limit, false);
					final int len = measurer.getPosition() - start;
					lines.add(new SingleLineTextLayout(layout, len));
					lineBeginIndexes.add(paragraphBeginIndex + start);
					start = measurer.getPosition();
					// reset initial font
					it.setIndex(start - 1);
					initialFont = it.getFont();
				}
			}
		}
		
		// computes total height and max width
		this.lineSpacing = lineSpacing;
		size = Vector2D.make();
		for (int i = 0; i < lines.size(); i++) {
			SingleLineTextLayout line = lines.get(i);
			if (line.width > size.x) size.x = line.width;
			if (i > 0) size.y += lineSpacing;
			size.y += line.height;
		}
		
		// computes origin
		int top = 0;
		for (int i = 0; i < lines.size(); i++) {
			SingleLineTextLayout line = lines.get(i);
			line.y = top;
			top += lineSpacing + line.height;
			switch (alignment) {
			case LEFT:
				line.x = 0;
				break;
			case CENTER:
				line.x = (size.x - line.width) / 2;
				break;
			case RIGHT:
				line.x = size.x - line.width;
				break;
			}
		}
		
		textLength = text.length();
	}
	
	public void draw(Graphics2D g) {
		for (SingleLineTextLayout line : lines) {
			line.draw(g);
		}
	}
	
	public Vector2D size() {
		return size.clone();
	}
	
	@Override
	public int charIndexOnPoint(Vector2D point) {
		int lineIndex = lineIndexOnPoint(point);
		if (lineIndex != -1) {
			SingleLineTextLayout line = lines.get(lineIndexNearPoint(point));
			int subIndex = line.charIndexAtAdvance(point.x - line.x);
			return lineBeginIndexes.get(lineIndex) + subIndex;
		} else {
			return -1;
		}
	}
	
	@Override
	public int charIndexNearPoint(Vector2D point) {
		int lineIndex = lineIndexNearPoint(point);
		SingleLineTextLayout line = lines.get(lineIndex);
		int subIndex = line.charIndexAtAdvance(point.x - line.x);
		return lineBeginIndexes.get(lineIndex) + subIndex;
	}
	
	@Override
	public CaretIndex caretIndexNearPoint(Vector2D point) {
		int lineIndex = lineIndexNearPoint(point);
		SingleLineTextLayout line = lines.get(lineIndex);
		int subIndex = line.caretIndexAtAdvance(point.x - line.x);
		if (lineIndex != lines.size() - 1) {
			// not last line
			int beginIndex = lineBeginIndexes.get(lineIndex);
			int nextBeginIndex = lineBeginIndexes.get(lineIndex + 1);
			if (beginIndex + subIndex == nextBeginIndex) {
				// auto new line
				return CaretIndex.after(beginIndex + subIndex - 1);
			} else {
				return CaretIndex.before(beginIndex + subIndex);
			}
		} else {
			// last line
			return CaretIndex.before(lineBeginIndexes.get(lineIndex) + subIndex);
		}
	}
	
	@Override
	public Vector2D basePointAtCaretIndex(CaretIndex caretIndex) {
		if (!isValidCaretIndex(caretIndex)) {
			throw new IndexOutOfBoundsException();
		}
		int lineIndex = lineIndexAtCaretIndex(caretIndex);
		SingleLineTextLayout line = lines.get(lineIndex);
		int beginIndex = lineBeginIndexes.get(lineIndex);
		int subIndex;
		if (!caretIndex.before) subIndex = caretIndex.charIndex - beginIndex + 1;
		else subIndex = caretIndex.charIndex - beginIndex;
		int advance = line.advanceAtCaretIndex(subIndex);
		return Vector2D.make(advance + line.x, line.y + line.ascent);
	}
	
	@Override
	public CaretIndex nextCaretIndex(CaretIndex caretIndex) {
		if (!isValidCaretIndex(caretIndex)) {
			throw new IndexOutOfBoundsException();
		}
		if (!caretIndex.before) caretIndex = caretIndex.other();
		// before
		CaretIndex rst = CaretIndex.before(caretIndex.charIndex + 1);
		if (isValidCaretIndex(rst)) return rst;
		else return caretIndex;
	}
	
	@Override
	public CaretIndex previousCaretIndex(CaretIndex caretIndex) {
		if (!isValidCaretIndex(caretIndex)) {
			throw new IndexOutOfBoundsException();
		}
		if (!caretIndex.before) caretIndex = caretIndex.other();
		// before
		CaretIndex rst = CaretIndex.before(caretIndex.charIndex - 1);
		if (isValidCaretIndex(rst)) return rst;
		else return caretIndex;
	}
	
	@Override
	public int ascentAtCaretIndex(CaretIndex caretIndex) {
		if (!isValidCaretIndex(caretIndex)) {
			throw new IndexOutOfBoundsException();
		}
		int lineIndex = lineIndexAtCaretIndex(caretIndex);
		return lines.get(lineIndex).ascent;
	}
	
	@Override
	public int descentAtCaretIndex(CaretIndex caretIndex) {
		if (!isValidCaretIndex(caretIndex)) {
			throw new IndexOutOfBoundsException();
		}
		int lineIndex = lineIndexAtCaretIndex(caretIndex);
		return lines.get(lineIndex).descent;
	}
	
	@Override
	public List<Range> charRangesOfLines() {
		List<Range> ranges = new ArrayList<Range>(lineBeginIndexes.size());
		for (int i = 0; i < lineBeginIndexes.size() - 1; i++) {
			int begin = lineBeginIndexes.get(i);
			ranges.add(Range.make(begin, lineBeginIndexes.get(i + 1) - begin));
		}
		ranges.add(Range.make(lineBeginIndexes.get(lineBeginIndexes.size() - 1), lines.get(lines.size() - 1).actualTextLength));
		return ranges;
	}
	
	public int lineIndexOfCharIndex(int index) {
		if (index < 0 || index >= textLength) {
			throw new IndexOutOfBoundsException();
		}
		List<Range> ranges = charRangesOfLines();
		for (int i = 0; i < ranges.size(); i++) {
			Range range = ranges.get(i);
			if (index >= range.offset && index < range.offset + range.length) {
				return i;
			}
		}
		throw new IllegalStateException();
	}
	
	public int lineIndexOfCaretIndex(CaretIndex index) {
		if (index.toBefore().charIndex < 0 || index.toBefore().charIndex > textLength) {
			throw new IndexOutOfBoundsException();
		}
		return lineIndexAtCaretIndex(index);
	}
	
	//// Private Methods ////
	
	private boolean isValidCaretIndex(CaretIndex index) {
		index = index.toBefore();
		return index.charIndex >= 0 && index.charIndex <= textLength;
	}
	
	private int lineIndexNearPoint(Vector2D point) {
		final int halfSpace = lineSpacing / 2;
		for (int i = 0; i < lines.size(); i++) {
			SingleLineTextLayout line = lines.get(i);
			if (point.y <= line.y + line.height + halfSpace) {
				return i;
			}
		}
		return lines.size() - 1;
	}
	
	private int lineIndexOnPoint(Vector2D point) {
		for (int i = 0; i < lines.size(); i++) {
			SingleLineTextLayout line = lines.get(i);
			if (point.y < line.y) return -1;
			if (point.y >= line.y && point.y <= line.y + line.height &&
					point.x >= line.x && point.x <= line.x + line.width) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Client must guarantees that the <code>caretIndex</code> is valid.
	 * @param caretIndex
	 * @return
	 */
	private int lineIndexAtCaretIndex(CaretIndex caretIndex) {
		for (int i = 1; i < lineBeginIndexes.size(); i++) {
			final int beginIndex =lineBeginIndexes.get(i);
			if (caretIndex.charIndex < beginIndex) {
				if (caretIndex.charIndex == beginIndex - 1 && !caretIndex.before) {
					// determines auto new line or not
					int actualBeginIndex = lineBeginIndexes.get(i - 1) + lines.get(i - 1).actualTextLength;
					if (actualBeginIndex < beginIndex) {
						return i;
					} else {
						return i - 1;
					}
				} else {
					return i - 1;
				}
			}
		}
		return lineBeginIndexes.size() - 1;
	}
	
	//// Helper Methods ////
	
	private static void split(Iterator text, int beginIndex, List<Iterator> out_strings, List<Integer> out_beginIndexes) {
		text.setIndex(beginIndex);
		while (text.getIndex() < text.getEndIndex()) {
			if (text.current() == '\n') {
				out_strings.add(text.iterator(Range.make(beginIndex, text.getIndex() - beginIndex)));
				out_beginIndexes.add(beginIndex);
				split(text, text.getIndex() + 1, out_strings, out_beginIndexes);
				return;
			} else {
				text.next();
			}
		}
		
		if (beginIndex == 0) {
			out_strings.add(text.iterator(Range.make(0, text.length())));
			out_beginIndexes.add(0);
		} else {
			out_strings.add(text.iterator(Range.make(beginIndex, text.length() - beginIndex)));
			out_beginIndexes.add(beginIndex);
		}
	}
	
	private static int limitLength(Iterator line, FontRenderContext frc, int widthLimit) {
		java.awt.font.TextLayout layout = new java.awt.font.TextLayout(line, frc);
		for (int i = line.length(); i >= 1; i--) {
			float width = layout.getCaretInfo(TextHitInfo.beforeOffset(i))[0];
			if (width <= widthLimit) return i;
		}
		return 1;
	}
	
}
