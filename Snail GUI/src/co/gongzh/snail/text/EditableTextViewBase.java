package co.gongzh.snail.text;

import java.util.ArrayList;
import java.util.List;

import co.gongzh.snail.MouseEvent;
import co.gongzh.snail.View;
import co.gongzh.snail.event.EventHandler;
import co.gongzh.snail.event.Key;
import co.gongzh.snail.util.Insets;
import co.gongzh.snail.util.Range;
import co.gongzh.snail.util.Rectangle;
import co.gongzh.snail.util.Vector2D;

abstract class EditableTextViewBase extends TextView {
	
	public static final Key CARET_CHANGED = new Key("caretChanged", EditableTextView.class, null);
	public static final Key SELECTION_CHANGED = new Key("selectionChanged", EditableTextView.class, null);
	
	private final Range selectionRange;
	private final SelectionView selectionView;
	
	private CaretIndex caretPosition;
	private final CaretView caretView;
	
	public EditableTextViewBase() {
		caretPosition = CaretIndex.before(0);
		selectionRange = Range.make(0, 0);
		
		// selection view and caret view
		selectionView = createSelectionView();
		addSubview(selectionView);
		caretView = createCaretView();
		addSubview(caretView);
		
		// register handers
		addEventHandler(GOT_KEYBOARD_FOCUS, new EventHandler() {
			@Override
			public void handle(View sender, Key eventName, Object arg) {
				textViewGotKeyboardFocus();
			}
		});
		addEventHandler(LOST_KEYBOARD_FOCUS, new EventHandler() {
			@Override
			public void handle(View sender, Key eventName, Object arg) {
				textViewLostKeyboardFocus();
			}
		});
		addEventHandler(TEXT_LAYOUT_CHANGED, new EventHandler() {
			@Override
			public void handle(View sender, Key eventName, Object arg) {
				textViewLayoutChange();
			}
		});
		addEventHandler(TEXT_CHANGED, new EventHandler() {
			@Override
			public void handle(View sender, Key eventName, Object arg) {
				textViewTextChanged();
			}
		});
		addEventHandler(MOUSE_PRESSED, mouseEventHandler);
		addEventHandler(MOUSE_RELEASED, mouseEventHandler);
		addEventHandler(MOUSE_DRAGGED, mouseEventHandler);
		addEventHandler(MOUSE_CLICKED, mouseEventHandler);
	}
	
	//// Selection View Methods ////
	
	protected SelectionView createSelectionView() {
		return new SelectionView.DefaultSelectionView();
	}
	
	public SelectionView getSelectionView() {
		return selectionView;
	}

	public Range getSelectionRange() {
		return selectionRange.clone();
	}
	
	public void setSelectionRange(Range range) {
		if (range == null || range.length == 0) range = Range.make(0, 0);
		if (range.offset < 0 || range.offset + range.length > textLength()) {
			throw new IndexOutOfBoundsException();
		}
		if (!selectionRange.equals(range)) {
			selectionRange.set(range.offset, range.length);
			resetSelectionArea();
			fireEvent(SELECTION_CHANGED, null);
		}
	}
	
	private void resetSelectionArea() {
		if (selectionRange.length == 0) {
			selectionView.setSelectionArea(null);
		} else {
			final List<Rectangle> rst = new ArrayList<Rectangle>(0);
			final List<Range> ranges = charRangesOfLines();
			final int startLine = lineIndexOfCharIndex(ranges, selectionRange.offset);
			final int endLine = lineIndexOfCharIndex(ranges, selectionRange.offset + selectionRange.length - 1);
			final int upper = getLineSpacing() / 2;
			final int lower = (getLineSpacing() + 1) / 2;
			final Insets insets = getInsets();
			for (int i = startLine; i <= endLine; i++) {
				final CaretIndex first = CaretIndex.before(Math.max(ranges.get(i).offset, selectionRange.offset));
				CaretIndex last = CaretIndex.before(Math.min(ranges.get(i).offset + ranges.get(i).length, selectionRange.offset + selectionRange.length)).other();
				if (charAt(last.charIndex) == '\n') last = CaretIndex.after(last.charIndex - 1);
				final int ascent = ascentAtCaretIndex(first);
				final int descent = descentAtCaretIndex(first);
				final Vector2D firstPoint = basePointAtCaretIndex(first);
				final Vector2D lastPoint = basePointAtCaretIndex(last);
				Rectangle rectangle = Rectangle.make();
				rectangle.origin.x = firstPoint.x - insets.left;
				rectangle.size.x = lastPoint.x - firstPoint.x;
				rectangle.origin.y = firstPoint.y - ascent - upper - insets.top;
				rectangle.size.y = upper + ascent + descent + lower;
				rst.add(rectangle);
			}
			selectionView.setSelectionArea(rst);
		}
		View.scaleViewWithInsetsToSuperView(selectionView, getInsets());
	}
	
	private int lineIndexOfCharIndex(List<Range> ranges, int charIndex) {
		for (int i = 0; i < ranges.size(); i++) {
			Range range = ranges.get(i);
			if (charIndex >= range.offset && charIndex < range.offset + range.length) {
				return i;
			}
		}
		return -1;
	}
	
	//// Caret View Methods ////
	
	protected CaretView createCaretView() {
		return new CaretView.DefaultCaretView();
	}
	
	public CaretView getCaretView() {
		return caretView;
	}
	
	public CaretIndex getCaretPosition() {
		return caretPosition;
	}
	
	public void setCaretPosition(CaretIndex index) {
		if (!caretPosition.equals(index)) {
			caretPosition = index;
			relocateCaretView();
			fireEvent(CARET_CHANGED, null);
		}
	}
	
	private void relocateCaretView() {
		if (isKeyboardFocus()) {
			setTextLayoutOffset(null);
			Vector2D vec = basePointAtCaretIndex(caretPosition);
			int ascent = ascentAtCaretIndex(caretPosition);
			int descent = descentAtCaretIndex(caretPosition);
			caretView.locateCaretView(vec, ascent, descent);
			
			// determines text layout origin offset
			final Insets insets = getInsets();
			int offset_x = 0, offset_y = 0;
			
			offset_y = caretView.getBottom() - insets.bottom;
			if (offset_y > 0) {
				offset_y = insets.top - caretView.getTop();
				if (offset_y < 0) offset_y = 0;
			}
			
			offset_x = caretView.getRight() - insets.right;
			if (offset_x > 0) {
				offset_x = insets.left - caretView.getLeft();
				if (offset_x < 0) offset_x = 0;
			}
			
			if (offset_x != 0 || offset_y != 0) {
				Vector2D offset = Vector2D.make(offset_x, offset_y);
				setTextLayoutOffset(offset);
				caretView.locateCaretView(Vector2D.add(vec, offset), ascent, descent);
			}
		}
	}
	
	//// Delegates ////
	
	private void textViewGotKeyboardFocus() {
		caretView.showCaret();
		relocateCaretView();
		selectionView.setSelectionFocused();
	}
	
	private void textViewLostKeyboardFocus() {
		caretView.hideCaret();
		setTextLayoutOffset(null);
		selectionView.setSelectionUnfocused();
	}
	
	private void textViewTextChanged() {
		// validates current caret position
		CaretIndex index = caretPosition;
		if (!index.before) index = index.other();
		if (index.charIndex > textLength()) {
			caretPosition = CaretIndex.before(textLength());
			fireEvent(CARET_CHANGED, null);
		}
		
		// validates current selection range
		if (selectionRange.length > 0) {
			selectionRange.set(0, 0);
			fireEvent(SELECTION_CHANGED, null);
		}
		
		// NOTE: the textViewLayoutChange methods will be called later.
	}
	
	private void textViewLayoutChange() {
		relocateCaretView();
		resetSelectionArea();
	}
	
	boolean isAllowedDefaultMouseBehavior() {
		return true;
	}

	private final EventHandler mouseEventHandler = new EventHandler() {
		
		private CaretIndex pressedCaretIndex;
		
		@Override
		public void handle(View sender, Key eventName, Object arg) {
			MouseEvent event = (MouseEvent) arg;
			if (event.getButton() == java.awt.event.MouseEvent.BUTTON1) {
				if (isAllowedDefaultMouseBehavior()) {
					Vector2D vec = event.getPosition(sender);
					if (eventName.equals(MOUSE_PRESSED)) {
						
						// press
						if (!isKeyboardFocus()) requestKeyboardFocus();
						setSelectionRange(null);
						setCaretPosition(pressedCaretIndex = caretIndexNearPoint(vec));
						caretView.disableFlicker();
						
					} else if (eventName.equals(MOUSE_RELEASED)) {
						
						// release
						caretView.enableFlicker();
						
					} else if (eventName.equals(MOUSE_DRAGGED)) {
						
						// drag
						CaretIndex newIndex = caretIndexNearPoint(vec);
						setCaretPosition(newIndex);
						int start = Math.min(pressedCaretIndex.toBefore().charIndex, newIndex.toBefore().charIndex);
						int end = Math.max(pressedCaretIndex.toBefore().charIndex, newIndex.toBefore().charIndex);
						setSelectionRange(Range.make(start, end - start));
						
					} else if (eventName.equals(MOUSE_CLICKED)) {
						// do nothing, just handle the event to keep consistency
					}
				}
				event.handle();
			}
 		}
	};
	
}
