package co.gongzh.snail.examples;

import java.awt.Color;
import java.awt.Font;
import java.text.BreakIterator;

import javax.swing.JFrame;

import co.gongzh.snail.MouseEvent;
import co.gongzh.snail.View;
import co.gongzh.snail.ViewContext;
import co.gongzh.snail.text.EditableTextView;
import co.gongzh.snail.util.Alignment;
import co.gongzh.snail.util.Insets;
import co.gongzh.snail.util.Range;

public class Example03 extends View {
	
	EditableTextView textView;
	
	Example03() {
		setBackgroundColor(Color.CYAN);
		
		textView = new EditableTextView();
		textView.setDefaultFont(new Font(Font.DIALOG, Font.BOLD, 24));
		textView.setDefaultTextColor(Color.BLACK);
		textView.setText("This is a TextBox.\nWelcome to great China.");
//		textView.setText("1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ");
		textView.setBreakIterator(BreakIterator.getLineInstance());
		textView.setTextAlignment(Alignment.LEFT_TOP);
//		textView.setInsets(Insets.make(20, 20, 20, 20));
		textView.setTextColor(Range.make(16, 5), Color.RED);
		addSubview(textView);
		
	}
	
	@Override
	protected void mouseClicked(MouseEvent e) {
		if (textView.isKeyboardFocus() && e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
			textView.resignKeyboardFocus();
		}
	}
	
	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);
		if (textView != null) {
			textView.setPosition(170, 170);
			textView.setSize(width - 340, height - 340);
		}
	}

	public static void main(String[] args) {
		
		// standard Swing
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 400);
		frame.setVisible(true);
		
		// bridge to Snail GUI
		ViewContext context = new ViewContext(frame.getContentPane());
		context.setRootView(new Example03());
		
	}

}
