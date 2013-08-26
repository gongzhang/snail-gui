package co.gongzh.snail.text;

import java.awt.Color;
import java.util.List;

import co.gongzh.snail.PaintMode;
import co.gongzh.snail.View;
import co.gongzh.snail.ViewGraphics;
import co.gongzh.snail.util.Rectangle;

public abstract class SelectionView extends View {
	
	public SelectionView() {
		setBackgroundColor(null);
		setPaintMode(PaintMode.DIRECTLY);
	}
	
	/**
	 * Resets the selection area. The <code>rectangles</code> parameter can be <code>null</code>
	 * which means it's an empty selection. The implementation can store <code>rectangles</code>
	 * as a reference instead of copy a new list.
	 * @param rectangles
	 */
	public abstract void setSelectionArea(List<Rectangle> rectangles);
	public abstract void setSelectionFocused();
	public abstract void setSelectionUnfocused();

	public static class DefaultSelectionView extends SelectionView {

		private boolean focused;
		private List<Rectangle> rectangles;
		private static final Color FOCUSED_COLOR = new Color(0.0f, 0.8f, 0.9f, 0.4f);
		private static final Color UNFOCUSED_COLOR = new Color(0.3f, 0.3f, 0.3f, 0.2f);
		
		public DefaultSelectionView() {
			rectangles = null;
			focused = false;
		}
		
		@Override
		public void setSelectionArea(List<Rectangle> rectangles) {
			this.rectangles = rectangles;
			setNeedsRepaint();
		}

		@Override
		public void setSelectionFocused() {
			focused = true;
			setNeedsRepaint();
		}

		@Override
		public void setSelectionUnfocused() {
			focused = false;
			setNeedsRepaint();
		}
		
		@Override
		protected void repaintView(ViewGraphics g) {
			if (rectangles == null || rectangles.isEmpty()) return;
			g.setColor(focused ? FOCUSED_COLOR : UNFOCUSED_COLOR);
			for (Rectangle rect : rectangles) {
				g.fillRect(rect.origin.x, rect.origin.y, rect.size.x, rect.size.y);
			}
		}
		
	}
	
}
