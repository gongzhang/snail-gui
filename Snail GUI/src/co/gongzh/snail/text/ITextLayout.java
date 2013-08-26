package co.gongzh.snail.text;

import java.util.List;

import co.gongzh.snail.util.Range;
import co.gongzh.snail.util.Vector2D;

interface ITextLayout {
	
	public int charIndexOnPoint(Vector2D point);
	
	public int charIndexNearPoint(Vector2D point);
	
	public CaretIndex caretIndexNearPoint(Vector2D point);
	
	public Vector2D basePointAtCaretIndex(CaretIndex caretIndex);
	
	public CaretIndex nextCaretIndex(CaretIndex caretIndex);
	
	public CaretIndex previousCaretIndex(CaretIndex caretIndex);
	
	public int ascentAtCaretIndex(CaretIndex caretIndex);
	
	public int descentAtCaretIndex(CaretIndex caretIndex);
	
	public List<Range> charRangesOfLines();
	
	public int lineIndexOfCharIndex(int index);
	
	public int lineIndexOfCaretIndex(CaretIndex index);
	
}
