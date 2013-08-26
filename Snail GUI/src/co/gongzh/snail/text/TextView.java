package co.gongzh.snail.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.text.BreakIterator;
import java.util.List;

import co.gongzh.snail.View;
import co.gongzh.snail.ViewGraphics;
import co.gongzh.snail.event.EventHandler;
import co.gongzh.snail.event.Key;
import co.gongzh.snail.text.AttributedString.Iterator;
import co.gongzh.snail.util.Alignment;
import co.gongzh.snail.util.Insets;
import co.gongzh.snail.util.Range;
import co.gongzh.snail.util.Vector2D;

public class TextView extends View implements ITextLayout {
	
	public static Font DEFAULT_FONT = Font.decode(Font.DIALOG);
	public static Color DEFAULT_TEXT_COLOR = Color.BLACK;
	
	public final static Key TEXT_CHANGED = new Key("textChanged", TextView.class, null); // notify the plain text changed
	public final static Key TEXT_LAYOUT_CHANGED = new Key("textLayoutChanged", TextView.class, null); // notify the text layout changed
	
	private final AttributedString text;
	
	private Font defaultFont;
	private Color defaultTextColor;
	private Alignment textAlignment;
	private final Insets insets;
	private BreakIterator breakIterator;
	private int lineSpacing;
	private TextAntialiasing textAntialiasing;
	
	private TextLayout layout;
	private final Vector2D layoutOrigin;
	private Vector2D layoutOriginOffset;
	
	public TextView() {
		text = new AttributedString();
		defaultFont = DEFAULT_FONT;
		defaultTextColor = DEFAULT_TEXT_COLOR;
		textAlignment = Alignment.LEFT_TOP;
		insets = Insets.make();
		breakIterator = null;
		lineSpacing = 0;
		textAntialiasing = TextAntialiasing.ON;
		layout = null;
		layoutOrigin = Vector2D.make();
		layoutOriginOffset = null;
		addEventHandler(SIZE_CHANGED, viewSizeChanged);
		addEventHandler(REPAINT, viewRepaint);
 	}
	
	//// Text Property Methods ////
	
	public Iterator textIterator() {
		return text.iterator();
	}
	
	public char charAt(int index) {
		return text.charAt(index);
	}
	
	public int textLength() {
		return text.length();
	}
	
	public AttributedString getText() {
		return text.clone();
	}
	
	public String getPlainText() {
		return text.toString();
	}
	
	public void setText(AttributedString str) {
		text.clear();
		text.append(str);
		fireEvent(TEXT_CHANGED, null);
		invalidLayout();
	}
	
	public void setText(String str) {
		text.clear();
		text.append(str, defaultFont, defaultTextColor);
		fireEvent(TEXT_CHANGED, null);
		invalidLayout();
	}
	
	public Range insertText(int index, AttributedString str) {
		Range range = text.insert(index, str);
		fireEvent(TEXT_CHANGED, null);
		invalidLayout();
		return range;
	}
	
	public Range insertText(int index, String str) {
		Range range = text.insert(index, str, defaultFont, defaultTextColor);
		fireEvent(TEXT_CHANGED, null);
		invalidLayout();
		return range;
	}
	
	public void deleteText(Range range) {
		text.delete(range);
		fireEvent(TEXT_CHANGED, null);
		invalidLayout();
	}
	
	public Font getDefaultFont() {
		return defaultFont;
	}
	
	public void setDefaultFont(Font defaultFont) {
		this.defaultFont = defaultFont;
		// NOTE: at most of situation, change default font won't have
		// an effect on layout. Only if the text is empty (or the first
		// line is empty), the layout needs to be updated.
		if (text.length() == 0 || text.charAt(0) == '\n') {
			invalidLayout();
		}
	}
	
	public Color getDefaultTextColor() {
		return defaultTextColor;
	}
	
	public void setDefaultTextColor(Color defaultTextColor) {
		this.defaultTextColor = defaultTextColor;
	}
	
	public void setFont(Range range, Font font) {
		text.setFont(range, font);
		invalidLayout();
	}
	
	public void setTextColor(Range range, Color color) {
		text.setColor(range, color);
		invalidLayout();
	}
	
	public final void setFont(Font font) {
		setFont(Range.make(0, text.length()), font);
	}
	
	public final void setTextColor(Color color) {
		setTextColor(Range.make(0, text.length()), color);
	}
	
	//// Other Properties ////
	
	public TextAntialiasing getTextAntialiasing() {
		return textAntialiasing;
	}
	
	public void setTextAntialiasing(TextAntialiasing textAntialiasing) {
		if (this.textAntialiasing != textAntialiasing) {
			this.textAntialiasing = textAntialiasing;
			invalidLayout();
		}
	}
	
	public Alignment getTextAlignment() {
		return textAlignment;
	}
	
	public void setTextAlignment(Alignment textAlignment) {
		if (this.textAlignment != textAlignment) {
			this.textAlignment = textAlignment;
			invalidLayout();
		}
	}
	
	public BreakIterator getBreakIterator() {
		return breakIterator;
	}
	
	public void setBreakIterator(BreakIterator breakIterator) {
		if (this.breakIterator != breakIterator) {
			this.breakIterator = breakIterator;
			invalidLayout();
		}
	}
	
	public int getLineSpacing() {
		return lineSpacing;
	}
	
	public void setLineSpacing(int lineSpacing) {
		if (this.lineSpacing != lineSpacing) {
			this.lineSpacing = lineSpacing;
			invalidLayout();
		}
	}
	
	public Insets getInsets() {
		return insets;
	}
	
	public void setInsets(Insets insets) {
		if (!this.insets.equals(insets)) {
			this.insets.set(insets);
			invalidLayout();
		}
	}
	
	//// View Methods ////
	
	private final EventHandler viewSizeChanged = new EventHandler() {
		@Override
		public void handle(View sender, Key eventName, Object arg) {
			invalidLayout();
		}
	};
	
	private final EventHandler viewRepaint = new EventHandler() {
		@Override
		public void handle(View sender, Key eventName, Object arg) {
			ViewGraphics g = (ViewGraphics) arg;
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAntialiasing.hint());
			g.setFont(defaultFont);
			g.setColor(defaultTextColor);
			Shape clip = g.getClip();
			final int width = getWidth() - insets.left - insets.right;
			final int height = getHeight() - insets.top - insets.bottom;
			g.clipRect(insets.left, insets.top, width, height);
			Vector2D origin = getLayoutOrigin(g);
			g.translate(origin.x, origin.y);
			getLayout(g).draw(g);
			g.translate(-origin.x, -origin.y);
			g.setClip(clip);
		}
	};
	
	@Override
	public int getPreferredWidth() {
		return getLayout(null).size().x + insets.left + insets.right;
	}
	
	@Override
	public int getPreferredHeight() {
		return getLayout(null).size().y + insets.top + insets.bottom;
	}
	
	//// ITextLayout Methods ////

	@Override
	public int charIndexOnPoint(Vector2D point) {
		point = Vector2D.subtract(point, getLayoutOrigin(null));
		return getLayout(null).charIndexOnPoint(point);
	}

	@Override
	public int charIndexNearPoint(Vector2D point) {
		point = Vector2D.subtract(point, getLayoutOrigin(null));
		return getLayout(null).charIndexNearPoint(point);
	}

	@Override
	public CaretIndex caretIndexNearPoint(Vector2D point) {
		point = Vector2D.subtract(point, getLayoutOrigin(null));
		return getLayout(null).caretIndexNearPoint(point);
	}

	@Override
	public Vector2D basePointAtCaretIndex(CaretIndex caretIndex) {
		Vector2D vec = getLayout(null).basePointAtCaretIndex(caretIndex);
		return Vector2D.add(vec, getLayoutOrigin(null));
	}

	@Override
	public CaretIndex nextCaretIndex(CaretIndex caretIndex) {
		return getLayout(null).nextCaretIndex(caretIndex);
	}

	@Override
	public CaretIndex previousCaretIndex(CaretIndex caretIndex) {
		return getLayout(null).previousCaretIndex(caretIndex);
	}

	@Override
	public int ascentAtCaretIndex(CaretIndex caretIndex) {
		return getLayout(null).ascentAtCaretIndex(caretIndex);
	}

	@Override
	public int descentAtCaretIndex(CaretIndex caretIndex) {
		return getLayout(null).descentAtCaretIndex(caretIndex);
	}
	
	@Override
	public List<Range> charRangesOfLines() {
		return getLayout(null).charRangesOfLines();
	}
	
	@Override
	public int lineIndexOfCaretIndex(CaretIndex index) {
		return getLayout(null).lineIndexOfCaretIndex(index);
	}
	
	@Override
	public int lineIndexOfCharIndex(int index) {
		return getLayout(null).lineIndexOfCharIndex(index);
	}
	
	//// Helper Methods ////
	
	/**
	 * Invalids current text layout and origin. Also sets needs repaint flag.
	 */
	private void invalidLayout() {
		if (layout != null) {
			layout = null;
			fireEvent(TEXT_LAYOUT_CHANGED, null);
			setNeedsRepaint();
		}
	}
	
	protected void setTextLayoutOffset(Vector2D offset) {
		if (offset != null) {
			layoutOriginOffset = offset.clone();
		} else {
			layoutOriginOffset = null;
		}
		layout = null;
		setNeedsRepaint();
	}
	
	private TextLayout getLayout(Graphics2D g) {
		if (layout == null) {
			if (g == null) {
				g = getTemporaryGraphicsContext();
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAntialiasing.hint());
				g.setFont(defaultFont);
				g.setColor(defaultTextColor);
			}
			if (breakIterator == null) {
				layout = new TextLayout(
						text.iterator(),
						g.getFontRenderContext(),
						textAlignment.horizontal(),
						lineSpacing,
						defaultFont);
			} else {
				layout = new TextLayout(
						text.iterator(),
						g.getFontRenderContext(),
						textAlignment.horizontal(),
						lineSpacing,
						defaultFont,
						Math.max(getWidth() - insets.left - insets.right, 0),
						breakIterator);
			}
			// computes layout origin
			final int width = getWidth() - insets.left - insets.right;
			final int height = getHeight() - insets.top - insets.bottom;
			int x = 0, y = 0;
			switch (textAlignment.horizontal()) {
			case LEFT:
				x = insets.left;
				break;
			case CENTER:
				x = insets.left + (width - layout.size().x) / 2;
				break;
			case RIGHT:
				x = insets.left + (width - layout.size().x);
				break;
			}
			switch (textAlignment.vertical()) {
			case TOP:
				y = insets.top;
				break;
			case CENTER:
				y = insets.top + (height - layout.size().y) / 2;
				break;
			case BOTTOM:
				y = insets.top + (height - layout.size().y);
				break;
			}
			layoutOrigin.set(x, y);
			
			// apply offset to layoutOrigin
			if (layoutOriginOffset != null) {
				layoutOrigin.increase(layoutOriginOffset);
				layoutOriginOffset = null;
			}
		}
		return layout;
	}
	
	private Vector2D getLayoutOrigin(Graphics2D g) {
		if (layout == null) getLayout(g);
		return layoutOrigin;
	}

}
