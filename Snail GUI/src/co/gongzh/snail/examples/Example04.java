package co.gongzh.snail.examples;

import java.awt.Color;

import javax.swing.JFrame;

import co.gongzh.snail.View;
import co.gongzh.snail.ViewContext;
import co.gongzh.snail.ViewGraphics;
import co.gongzh.snail.text.EditableTextView;

public class Example04 extends View {
	
	Example04() {
		Table table = new Table();
		table.setPosition(50, 50);
		table.setSize(400, 300);
		addSubview(table);
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 400);
		
		ViewContext context = new ViewContext(frame.getContentPane());
		context.setRootView(new Example04());
		frame.setVisible(true);
	}

}

class Table extends View {
	
	{
		// initialize with 5 rows
		addSubview(new Row());
		addSubview(new Row());
		addSubview(new Row());
		addSubview(new Row());
		addSubview(new Row());
	}

	@Override
	protected void layoutView() {
		int rowHeight = getHeight() / count();
		for (int i = 0; i < count(); i++) {
			getSubview(i).setFrame(0, rowHeight * i, getWidth(), rowHeight);
		}
	}
	
	@Override
	protected void repaintView(ViewGraphics g) {
		// draw the grid line
		g.setColor(Color.black);
		
		int rowHeight = getHeight() / count() - 1;
		for (int i = 0; i <= count(); i++) {
			g.drawLine(0, rowHeight * i, getWidth(), rowHeight * i);
		}
		
		int columnWidth = (getWidth() - 1) / 4;
		for (int i = 0; i <= 4; i++) {
			g.drawLine(i * columnWidth, 0, i * columnWidth, getHeight());
		}
	}
	
}

// a single row with 4 text boxes
class Row extends View {
	
	private EditableTextView column1, column2, column3, column4;
	
	public Row() {
		setBackgroundColor(null);
		
		column1 = new EditableTextView();
		column1.setBackgroundColor(null);
		addSubview(column1);
		
		column2 = new EditableTextView();
		column2.setBackgroundColor(null);
		addSubview(column2);
		
		column3 = new EditableTextView();
		column3.setBackgroundColor(null);
		addSubview(column3);
		
		column4 = new EditableTextView();
		column4.setBackgroundColor(null);
		addSubview(column4);
	}
	
	@Override
	protected void layoutView() {
		// auto-resize every text box
		column1.setPosition(0, 0);
		column2.setPosition(getWidth() / 4, 0);
		column3.setPosition(getWidth() / 2, 0);
		column4.setPosition(getWidth() * 3 / 4, 0);
		column1.setSize(getWidth() / 4, getHeight());
		column2.setSize(getWidth() / 4, getHeight());
		column3.setSize(getWidth() / 4, getHeight());
		column4.setSize(getWidth() / 4, getHeight());
	}
	
}
