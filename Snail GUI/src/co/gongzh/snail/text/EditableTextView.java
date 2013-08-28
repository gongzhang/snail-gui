package co.gongzh.snail.text;

import static java.awt.event.KeyEvent.CHAR_UNDEFINED;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_BACK_SPACE;
import static java.awt.event.KeyEvent.VK_C;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.awt.event.KeyEvent.VK_END;
import static java.awt.event.KeyEvent.VK_HOME;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_V;
import static java.awt.event.KeyEvent.VK_X;
import static java.awt.event.KeyEvent.VK_UP;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_ENTER;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;

import co.gongzh.snail.KeyEvent;
import co.gongzh.snail.View;
import co.gongzh.snail.event.EventHandler;
import co.gongzh.snail.event.Key;
import co.gongzh.snail.text.TextChangeEvent.TextChangeType;
import co.gongzh.snail.util.Range;
import co.gongzh.snail.util.Vector2D;

public class EditableTextView extends EditableTextViewBase implements InputMethodCompatible {

	public final static Key TEXT_CHANGED_BY_UI = new Key("textChangedByUI", EditableTextView.class, TextChangeEvent.class);
	
	private final Range composedTextRange;
	private boolean composing;
	
	public EditableTextView() {
		composedTextRange = Range.make(0, 0);
		composing = false;
		addEventHandler(LOST_KEYBOARD_FOCUS, new EventHandler() {
			@Override
			public void handle(View sender, Key eventName, Object arg) {
				// delete uncommitted text
				if (composing && composedTextRange.length > 0) {
					composing = false;
					setCaretPosition(CaretIndex.before(composedTextRange.offset));
					deleteText(composedTextRange);
					composedTextRange.set(0, 0);
					
					// NOTE:
					// the following endComposition() call does not work very well.
					// at least, it does not close the input candidate box.
					getViewContext().getSwingContainer().getInputContext().endComposition();
				}
			}
		});
		addEventHandler(KEY_PRESSED, keyboardEventHandler);
		addEventHandler(KEY_RELEASED, keyboardEventHandler);
		addEventHandler(KEY_TYPED, keyboardEventHandler);
	}
	
	//// Input Method Support ////
	
	@Override
	public boolean isInputMethodEnabled() {
		return true;
	}
	
	private final InputMethodListener inputMethodListener = new InputMethodListener() {
		
		@Override
		public void inputMethodTextChanged(InputMethodEvent event) {
			if (!composing) {
				composing = true;
				if (getSelectionRange().length > 0) {
					// replace current selected text
					setCaretPosition(CaretIndex.before(getSelectionRange().offset));
					String delText = getPlainText(getSelectionRange());
					deleteText(getSelectionRange());
					fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.DELETE, getSelectionRange().offset, delText));
				}
				composedTextRange.set(getCaretPosition().toBefore().charIndex, 0);
			}
			
			AttributedCharacterIterator it = event.getText();
			int committed_cnt = event.getCommittedCharacterCount();
	        
			AttributedString committed = getCommittedText(it, committed_cnt);
			AttributedString composed = getComposedText(it, committed_cnt);
			
			// delete current composed text
			deleteText(composedTextRange);
			composedTextRange.length = 0;
			
			// commit text
			if (committed.length() > 0) {
				insertText(composedTextRange.offset, committed);
				fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.INSERT, composedTextRange.offset, committed.toString()));
				composedTextRange.offset += committed.length();
			}
			
			if (composed.length() > 0) {
	            insertText(composedTextRange.offset, composed);
	            composedTextRange.set(composedTextRange.offset, composed.length());
	            int index = event.getCaret().getInsertionIndex();
				setCaretPosition(CaretIndex.before(composedTextRange.offset + index));
			} else {
				setCaretPosition(CaretIndex.before(composedTextRange.offset));
				composedTextRange.set(0, 0);
				composing = false;
			}
			
			event.consume();
		}
		
		private AttributedString getCommittedText(AttributedCharacterIterator it, int committed_cnt) {
			AttributedString string = new AttributedString();
			if (it == null) return string;
            int i = committed_cnt;
            char c = it.first();
            while (i-- > 0) {
            	string.append(String.valueOf(c), null, null);
                c = it.next();
            }
            applyFontAndColorOnCommittedText(string);
            filterUserInputText(string);
            return string;
		}
		
		private AttributedString getComposedText(AttributedCharacterIterator it, int committed_cnt) {
			AttributedString string = new AttributedString();
			if (it == null) return string;
            if (it.getEndIndex() - (it.getBeginIndex() + committed_cnt) > 0) {
            	char c = it.setIndex(it.getBeginIndex() + committed_cnt);
            	while (it.getIndex() != it.getEndIndex()) {
            		string.append(String.valueOf(c), null, null);
                    c = it.next();
            	}
            }
            applyFontAndColorOnComposedText(string);
            return string;
		}
		
		@Override
		public void caretPositionChanged(InputMethodEvent event) {
			int index = event.getCaret().getInsertionIndex();
			setCaretPosition(CaretIndex.before(index));
			event.consume();
		}
		
	};
	
	private final InputMethodRequests inputMethodRequests = new InputMethodRequests() {
		
		@Override
		public Rectangle getTextLocation(TextHitInfo offset) {
	        CaretIndex caretIndex;
	        if (offset == null) {
	            // no composed text: return caret for committed text
	            caretIndex = getCaretPosition();
	        } else {
	            // composed text: return caret within composed text
	            caretIndex = CaretIndex.before(composedTextRange.offset + offset.getInsertionIndex());
	        }

	        Vector2D basePoint = basePointAtCaretIndex(caretIndex);
	        int ascent = ascentAtCaretIndex(caretIndex);
	        int descent = descentAtCaretIndex(caretIndex);
	        
	        final Rectangle rectangle = new Rectangle();
	        basePoint = transformPointToRootView(basePoint);
	        rectangle.x = basePoint.x;
	        rectangle.y = basePoint.y - ascent;
	        rectangle.width = 0;
	        rectangle.height = ascent + descent;
	        
	        // translate to screen coordinates
	        Point location = getViewContext().getSwingContainer().getLocationOnScreen();
	        rectangle.translate(location.x, location.y);
	        return rectangle;
		}
		
		@Override
		public AttributedCharacterIterator getSelectedText(Attribute[] attributes) {
			return getText().iterator(getSelectionRange());
		}
		
		@Override
		public TextHitInfo getLocationOffset(int x, int y) {
			
			// NOTE:
			// here is a MAC OS java implementation bug. the incoming (x, y) is not
			// standard swing screen coordinates, however, it's MAC OS Quartz2D coordinates which
			// is that (0,0) is left-bottom corner, not left-top corner.
			
//			Point point = new Point(x, y);
//			SwingUtilities.convertPointFromScreen(point, getViewContext().getSwingContainer());
//			Vector2D vec = getPositionInRootView();
//			point.x -= vec.x;
//			point.y -= vec.y;
//			
//			int index = charIndexOnPoint(Vector2D.make(point));
//			if (index >= composedTextRange.offset && index < composedTextRange.offset + composedTextRange.length) {
//				System.out.println(index - composedTextRange.offset);
//				return TextHitInfo.leading(index - composedTextRange.offset);
//			} else {
//				return TextHitInfo.leading(0);
//			}
			
			return TextHitInfo.leading(0);
		}
		
		@Override
		public int getInsertPositionOffset() {
			return composing ? composedTextRange.offset : getCaretPosition().toBefore().charIndex;
		}
		
		@Override
		public int getCommittedTextLength() {
			return textLength() - composedTextRange.length;
		}
		
		@Override
		public AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex, Attribute[] attributes) {
			AttributedString string = getText();
			if (composing) string.delete(composedTextRange);
			return string.iterator(Range.make(beginIndex, endIndex - beginIndex));
		}
		
		@Override
		public AttributedCharacterIterator cancelLatestCommittedText(Attribute[] attributes) {
			return null;
		}
		
	};

	@Override
	public final InputMethodListener getInputMethodEventHandler() {
		return inputMethodListener;
	}

	@Override
	public final InputMethodRequests getInputMethodInfoProvider() {
		return inputMethodRequests;
	}
	
	private final EventHandler keyboardEventHandler = new EventHandler() {
		
		@Override
		public void handle(View sender, Key eventName, Object arg) {
			KeyEvent e = (KeyEvent) arg;
			if (eventName.equals(KEY_TYPED)) {
				textViewKeyTyped(e);
			} else if (eventName.equals(KEY_PRESSED)) {
				final boolean command = e.isMetaOrCtrlDown();
				final boolean shift = e.isShiftDown();
				final CaretIndex caret = getCaretPosition();
				final Range sel = getSelectionRange().clone();
				doEditableTextViewKeyCommand(e.getKeyCode(), command, shift, caret, sel);
			} else if (eventName.equals(KEY_RELEASED)) {
				// do nothing, keep consistency
			}
			e.handle();
		}
		
	};
	
	private void textViewKeyTyped(KeyEvent e) {
		char ch = e.getKeyChar();
		if (ch != CHAR_UNDEFINED
			&& ch != VK_BACK_SPACE
			&& ch != VK_DELETE
			&& ch != '\n'
			&& ch != '\r'
			&& ch != '\t'
			&& !e.isMetaOrCtrlDown()) {
			final AttributedString text = new AttributedString(String.valueOf(ch), null, null);
			filterUserInputText(text);
			if (text.length() > 0) {
				applyFontAndColorOnCommittedText(text);
				
				Range sel = getSelectionRange();
				if (sel.length > 0) {
					setCaretPosition(CaretIndex.before(sel.offset));
					String delText = getPlainText(sel);
					deleteText(sel);
					fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.DELETE, sel.offset, delText));
				}
				
				final int index = getCaretPosition().toBefore().charIndex;
				insertText(index, text);
				fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.INSERT, index, text.toString()));
				setCaretPosition(CaretIndex.before(index + text.length()));
			}
		}
	}
	
	//// Subview Customizable Methods ////
	
	/**
	 * The default mouse behavior is that handle all mouse event with
	 * left mouse button except user is composing with input method.
	 */
	@Override
	boolean isAllowedDefaultMouseBehavior() {
		return !composing;
	}
	
	@Override
	protected CaretView createCaretView() {
		return super.createCaretView();
	}
	
	@Override
	protected SelectionView createSelectionView() {
		return super.createSelectionView();
	}

	protected void applyFontAndColorOnCommittedText(AttributedString text) {
		text.setFont(Range.make(0, text.length()), getDefaultFont());
		text.setColor(Range.make(0, text.length()), getDefaultTextColor());
	}
	
	protected void applyFontAndColorOnComposedText(AttributedString text) {
		text.setFont(Range.make(0, text.length()), getDefaultFont());
		text.setColor(Range.make(0, text.length()), getDefaultTextColor());
	}
	
	protected void filterUserInputText(AttributedString text) {
		// by default, accepts all characters in text
	}
	
	protected void doEditableTextViewKeyCommand(int keyCode, boolean ctrl, boolean shift, CaretIndex caret, Range sel) {
		switch (keyCode) {
		case VK_BACK_SPACE:
			if (sel.length > 0) {
				setSelectionRange(Range.make(sel.offset, 0));
				setCaretPosition(CaretIndex.before(sel.offset));
				String delText = getPlainText(sel);
				deleteText(sel);
				fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.DELETE, sel.offset, delText));
			} else if (caret.toBefore().charIndex > 0) {
				if (!shift) {
					setCaretPosition(CaretIndex.before(caret.toBefore().charIndex - 1));
					Range delRange = Range.make(caret.toBefore().charIndex - 1, 1);
					String delText = getPlainText(delRange);
					deleteText(delRange);
					fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.DELETE, delRange.offset, delText));
				} else {
					setCaretPosition(CaretIndex.before(0));
					Range delRange = Range.make(0, caret.toBefore().charIndex);
					String delText = getPlainText(delRange);
					deleteText(delRange);
					fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.DELETE, delRange.offset, delText));
				}
			}
			break;
			
		case VK_DELETE:
			if (sel.length > 0) {
				setSelectionRange(Range.make(sel.offset, 0));
				setCaretPosition(CaretIndex.before(sel.offset));
				String delText = getPlainText(sel);
				deleteText(sel);
				fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.DELETE, sel.offset, delText));
			} else if (caret.toBefore().charIndex < textLength()) {
				if (!shift) {
					Range delRange = Range.make(caret.toBefore().charIndex, 1);
					String delText = getPlainText(delRange);
					deleteText(delRange);
					fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.DELETE, delRange.offset, delText));
				} else {
					Range delRange = Range.make(caret.toBefore().charIndex, textLength() - caret.toBefore().charIndex);
					String delText = getPlainText(delRange);
					deleteText(delRange);
					fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.DELETE, delRange.offset, delText));
				}
			}
			break;
			

		case VK_HOME:
			if (shift) setSelectionRange(Range.make(0, caret.toBefore().charIndex));
			else setSelectionRange(null);
			setCaretPosition(CaretIndex.before(0));
			break;
			
		case VK_END:
			if (shift) setSelectionRange(Range.make(caret.toBefore().charIndex, textLength() - caret.toBefore().charIndex));
			else setSelectionRange(null);
			setCaretPosition(CaretIndex.before(textLength()));
			break;
			
		case VK_LEFT:
			if (ctrl) {
				if (shift) setSelectionRange(Range.make(0, caret.toBefore().charIndex));
				else setSelectionRange(Range.make(0, 0));
				setCaretPosition(CaretIndex.before(0));
			} else {
				if (sel.length > 0) {
					setSelectionRange(Range.make(sel.offset, 0));
					setCaretPosition(CaretIndex.before(sel.offset));
				} else {
					if (caret.toBefore().charIndex > 0) setCaretPosition(CaretIndex.before(caret.toBefore().charIndex - 1));
				}
			}
			break;
			
		case VK_RIGHT:
			if (ctrl) {
				if (shift) setSelectionRange(Range.make(caret.toBefore().charIndex, textIterator().length() - caret.toBefore().charIndex));
				else setSelectionRange(Range.make(0, 0));
				setCaretPosition(CaretIndex.before(textLength()));
			} else {
				if (sel.length > 0) {
					setSelectionRange(Range.make(sel.offset + sel.length, 0));
					setCaretPosition(CaretIndex.before(sel.offset + sel.length));
				} else {
					if (caret.toBefore().charIndex < textIterator().length()) setCaretPosition(CaretIndex.before(caret.toBefore().charIndex + 1));
				}
			}
			break;
			
		case VK_UP:
			if (ctrl) {
				if (shift) setSelectionRange(Range.make(0, caret.toBefore().charIndex));
				else setSelectionRange(Range.make(0, 0));
				setCaretPosition(CaretIndex.before(0));
			} else {
				setSelectionRange(Range.make(0, 0));
				int line = lineIndexOfCaretIndex(caret);
				if (line > 0) {
					Vector2D point = basePointAtCaretIndex(caret);
					point.y -= ascentAtCaretIndex(caret) + getLineSpacing() + 1;
					setCaretPosition(caretIndexNearPoint(point));
				}
			}
			break;

		case VK_DOWN:
			if (ctrl) {
				if (shift) setSelectionRange(Range.make(caret.toBefore().charIndex, textIterator().length() - caret.toBefore().charIndex));
				else setSelectionRange(Range.make(0, 0));
				setCaretPosition(CaretIndex.before(textLength()));
			} else {
				setSelectionRange(Range.make(0, 0));
				int line = lineIndexOfCaretIndex(caret);
				if (line < lineIndexOfCharIndex(textIterator().length() - 1)) {
					Vector2D point = basePointAtCaretIndex(caret);
					point.y += descentAtCaretIndex(caret) + getLineSpacing() + 1;
					setCaretPosition(caretIndexNearPoint(point));
				}
			}
			break;
		
		case VK_A:
			if (ctrl) {
				setSelectionRange(Range.make(0, textLength()));
				setCaretPosition(CaretIndex.before(textLength()));
			}
			break;
		
		case VK_X:
		case VK_C:
			if (ctrl && sel.length > 0) {
				String string = getText().toString().substring(sel.offset, sel.offset + sel.length);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection content = new StringSelection(string);
				clipboard.setContents(content, content);
				if (keyCode == VK_X) {
					setSelectionRange(null);
					setCaretPosition(CaretIndex.before(sel.offset));
					String delText = getPlainText(sel);
					deleteText(sel);
					fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.DELETE, sel.offset, delText));
				}
			}
			break;
			
		case VK_V:
			if (ctrl) {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				try {
					String string = (String) clipboard.getData(DataFlavor.stringFlavor);
					if (string != null) {
						if (sel.length > 0) {
							setSelectionRange(null);
							setCaretPosition(CaretIndex.before(sel.offset));
							String delText = getPlainText(sel);
							deleteText(sel);
							fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.DELETE, sel.offset, delText));
						}
						AttributedString text = new AttributedString(string, null, null);
						filterUserInputText(text);
						applyFontAndColorOnCommittedText(text);
						Range range = insertText(getCaretPosition().toBefore().charIndex, text);
						fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.INSERT, range.offset, text.toString()));
						setCaretPosition(CaretIndex.before(range.offset + range.length));
					}
				} catch (UnsupportedFlavorException e1) {
				} catch (IOException e1) {
				}
			}
		break;
		
		case VK_ENTER:
			{
				if (!shift && !ctrl) {
					final AttributedString text = new AttributedString("\n", null, null);
					filterUserInputText(text);
					if (text.length() > 0) {
						applyFontAndColorOnCommittedText(text);
						if (sel.length > 0) {
							setCaretPosition(CaretIndex.before(sel.offset));
							String delText = getPlainText(sel);
							deleteText(sel);
							fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.DELETE, sel.offset, delText));
						}
						final int index = getCaretPosition().toBefore().charIndex;
						insertText(index, text);
						fireEvent(TEXT_CHANGED_BY_UI, new TextChangeEvent(TextChangeType.INSERT, index, text.toString()));
						setCaretPosition(CaretIndex.before(index + text.length()));
					}
				}
				
			}
			break;
			
		}
	}
	
}
