package co.gongzh.snail.text;

import java.awt.RenderingHints;

public enum TextAntialiasing {
	
	OFF(RenderingHints.VALUE_TEXT_ANTIALIAS_OFF),
	DEFAULT(RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT),
	GASP(RenderingHints.VALUE_TEXT_ANTIALIAS_GASP),
	ON(RenderingHints.VALUE_TEXT_ANTIALIAS_ON),
	LCD_HBGR(RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR),
	LCD_HRGB(RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB),
	LCD_VBGR(RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR),
	LCD_VRGB(RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB),
	;
	
	private Object hint;
	
	TextAntialiasing(Object hint) {
		this.hint = hint;
	}
	
	public Object hint() {
		return hint;
	}

}
