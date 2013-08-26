package co.gongzh.snail.examples;

import java.awt.Color;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import co.gongzh.snail.Animation;
import co.gongzh.snail.Image;
import co.gongzh.snail.MouseEvent;
import co.gongzh.snail.PaintMode;
import co.gongzh.snail.View;
import co.gongzh.snail.ViewContext;
import co.gongzh.snail.ViewGraphics;
import co.gongzh.snail.util.Vector2D;

public class Example01 {

	public static void main(String[] args) {
		
		// standard Swing
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		frame.setVisible(true);
		
		// load an example PNG image.
		Image logo = new Image(frame.getGraphicsConfiguration(), new Image.ImageSourceLoader() {
			@Override
			public BufferedImage loadImage() {
				InputStream stream = Example01.class.getResourceAsStream("Icon.png");
				try {
					return ImageIO.read(stream);
				} catch (IOException e) {
					return null;
				}
			}
			@Override
			public void unloadImage(BufferedImage image) {
				image.flush();
			}
		});
		
		// bridge to Snail GUI
		ViewContext context = new ViewContext(frame.getContentPane());
		context.setRootView(new ExampleView(logo));
		
	}

}

class ExampleView extends View {
	
	private View logoView;
	
	public ExampleView(final Image logo) {
		setPaintMode(PaintMode.DIRECTLY); // this view just fills white background. so no need to buffer it.
		
		//// About Paint Mode
		////
		//// when to use PaintMode.BUFFERED (default):
		////      1. the view is not big
		////      2. the rendering work is difficult (such as text, image, shape-clipping...)
		////
		//// when to use PaintMode.DIRECTLY:
		////      1. the view is big
		////      2. simple rendering
		////      3. the view constantly repaints itself
		////
		//// it is okay to buffer everything. (like project JDAT... it takes about 40MB memory to store the buffer)
		//// on windows, that part of memory is actually in V-RAM to fully enable hardware-acceleration.
		//// i'm not sure how it works on mac...
		
		// Example Button 1
		ExampleButton button1 = new ExampleButton() {
			// we don't have listeners to subscribe events. we do that by inline-subclassing.
			@Override
			protected void mouseClicked(MouseEvent e) {
				button1Clicked();
				e.handle();
			}
		};
		button1.setText("play animation");
		button1.setPosition(40, 40);
		button1.setSize(100, 40);
		this.addSubview(button1);
		
		// Example Ball
		Ball ball = new Ball() {
			
			// dragging code:
			
			Vector2D v0;
			
			@Override
			protected void mousePressed(MouseEvent e) {
				v0 = e.getPosition(this); // remember relative location
				e.handle();
			}
			
			@Override
			protected void mouseDragged(MouseEvent e) {
				this.setPosition(Vector2D.subtract(e.getPosition(getSuperView()), v0));
				e.handle();
			}
			
			@Override
			protected void mouseReleased(MouseEvent e) {
				e.handle(); // if you handle "press" event in a view, you'd better handle "release" too, to keep the event consistency of its owner view...
			}
			
		};
		ball.setPosition(200, 200);
		ball.setSize(60, 60);
		this.addSubview(ball);
		
		// Logo
		logoView = new View() {
			@Override
			protected void repaintView(ViewGraphics g) {
				g.drawImage(logo, 0, 0, getWidth(), getHeight());
			}
		};
		logoView.setSize(40, 40);
		logoView.setBackgroundColor(null);
		this.addSubview(logoView);
	}
	
	private void button1Clicked() {
		// create a rectangle
		final View rect = new View();
		rect.setBackgroundColor(Color.cyan); // default bg-color is white.
		rect.setPosition(150, 100);
		rect.setSize(100, 100);
		rect.setPaintMode(PaintMode.DIRECTLY); // simple rectangle. no need to buffer it.
		this.addSubview(rect);
		
		// create animation
		Animation animation = new Animation(5.0f, Animation.EaseInEaseOut) {
			
			@Override
			protected void animate(float progress) {
				AffineTransform transform = new AffineTransform();
				transform.rotate(progress * 2 * Math.PI, 45, 45);
				transform.scale(1 + progress, 1 + progress);
				rect.setTransform(transform);
				rect.setAlpha(1 - progress);
			}
			
			@Override
			protected void completed(boolean canceled) {
				rect.removeFromSuperView();
			}
			
		};
		animation.commit(); // commit to play
	}
	
	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);
		// you can layout subviews here.
		if (logoView != null) {
			View.putViewWithRightAndBottom(logoView, 10, 10);
		}
	}
	
}

/**
 * A simple button.
 */
class ExampleButton extends View {
	
	private String text = "";
	private boolean pressed = false;
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
		setNeedsRepaint();
	}
	
	@Override
	protected void repaintView(ViewGraphics g) {
		g.setColor(pressed ? Color.red : Color.black);
		
		// draw border
		g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		
		// draw label at center of the button, AWT things.
		TextLayout layout = new TextLayout(text, g.getFont(), g.getFontRenderContext());
		float x = (getWidth() - layout.getAdvance()) / 2;
		float y = (getHeight() + layout.getAscent() - layout.getDescent()) / 2;
		layout.draw(g, x, y);
	}
	
	@Override
	protected void mousePressed(MouseEvent e) {
		pressed = true;
		setNeedsRepaint();
		e.handle();
	}
	
	@Override
	protected void mouseReleased(MouseEvent e) {
		pressed = false;
		setNeedsRepaint();
		e.handle();
	}
	
}

/**
 * A simple ball.
 */
class Ball extends View {
	
	private final Ellipse2D.Float shape = new Ellipse2D.Float(); // AWT-based shape model
	
	public Ball() {
		setBackgroundColor(null); // transparent background.
		setPaintMode(PaintMode.DIRECTLY);
	}
	
	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);
		
		// synchronize the model when size changed
		shape.width = width;
		shape.height = height;
		// because super.setSize(...) already called setNeedsRepaint(), so no need to call it again.
	}
	
	@Override
	protected void repaintView(ViewGraphics g) {
		g.setColor(Color.blue);
		g.fill(shape);
	}
	
	@Override
	public boolean isInside(Vector2D point) {
		// this method defines the shape of the view.
		// by default, it's rectangle (checks if the point is in [0,0,width,height]).
		//
		// here, we override it to an ellipse.
		return shape.contains(point.toPoint2D());
	}
	
}
