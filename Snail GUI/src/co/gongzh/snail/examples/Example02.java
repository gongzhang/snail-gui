package co.gongzh.snail.examples;

import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import co.gongzh.snail.Animation;
import co.gongzh.snail.Image;
import co.gongzh.snail.KeyEvent;
import co.gongzh.snail.MouseEvent;
import co.gongzh.snail.View;
import co.gongzh.snail.ViewContext;
import co.gongzh.snail.ViewGraphics;
import co.gongzh.snail.util.Vector2D;


public class Example02 {
	
	public static class CircleView extends View {
		private Color fillColor;
		public CircleView(int left, int top, int width, int height) {
			super(left, top, width, height);
			setBackgroundColor(null);
			setFillColor(Color.black);
		}
		public Color getFillColor() {
			return fillColor;
		}
		public CircleView setFillColor(Color fillColor) {
			this.fillColor = fillColor;
			setNeedsRepaint();
			return this;
		}
		@Override
		protected void repaintView(ViewGraphics g) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(fillColor);
			g.fillArc(0, 0, getWidth(), getHeight(), 0, 360);
		}
	}
	
	public static class ImageView extends View {
		private static Image img = null;
		public ImageView(int left, int top, int width, int height) {
			super(left, top, width, height);
			setBackgroundColor(null);
		}
		@Override
		protected void repaintView(ViewGraphics g) {
			if (img == null) {
				img = new Image(g.getDeviceConfiguration(), new Image.ImageSourceLoader() {
					@Override
					public BufferedImage loadImage() {
						try {
							return ImageIO.read(getClass().getResourceAsStream("Icon.png"));
						} catch (IOException e) {
							return null;
						}
					}
					@Override
					public void unloadImage(BufferedImage image) {
						image.flush();
					}
				});
			}
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.drawImage(img, 0, 0, getWidth(), getHeight());
		}
	}

	public static class KeyView extends View {
		private char ch = '?';
		private boolean mouseOn = false;
		public KeyView(int left, int top, int width, int height) {
			super(left, top, width, height);
			setBackgroundColor(Color.blue);
			setAlpha(0.5f);
		}
		@Override
		protected void repaintView(ViewGraphics g) {
			g.setColor(mouseOn ? Color.white : Color.green);
			g.setFont(new Font("Dialog", Font.PLAIN, 20));
			g.drawString(String.valueOf(ch), 16, 26);
		}
		@Override
		protected void gotKeyboardFocus() {
			setBackgroundColor(Color.red);
		}
		@Override
		protected void lostKeyboardFocus() {
			setBackgroundColor(Color.blue);
		}
		@Override
		protected void keyTyped(KeyEvent e) {
			ch = e.getKeyChar();
			setNeedsRepaint();
			e.handle();
		}
		@Override
		protected void mouseEntered() {
			mouseOn = true;
			setNeedsRepaint();
		}
		@Override
		protected void mouseExited() {
			mouseOn = false;
			setNeedsRepaint();
		}
		@Override
		protected void mousePressed(MouseEvent e) {
			requestKeyboardFocus();
			System.out.println(1);
		}
	}
	
	public static class MouseView extends View {
		public MouseView(int left, int top, int width, int height) {
			super(left, top, width, height);
			setBackgroundColor(Color.blue);
		}
		@Override
		protected void mouseEntered() {
			setBackgroundColor(Color.red);
		}
		@Override
		protected void mouseExited() {
			setBackgroundColor(Color.blue);
		}
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame("Snail GUI 5.0 Sample");
		frame.setBounds(200, 200, 640, 480);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// Snail GUI
		final View rootView = new ViewContext(frame.getContentPane()).getRootView();
		
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 3; y++) {
				final View view = new View(10 + 110 * x, 10 + 110 * y, 100, 100);
				view.setBackgroundColor(new Color(0xcccccc));
				if (x == 0 && y == 0) {
					// hierarchical
					view.setBackgroundColor(new Color(0xffcccc));
					int[] colors = new int[] {0xff0000, 0xffff00, 0x00ff00, 0x0000ff};
					for (int i = 0; i < 2; i++) {
						for (int j = 0; j < 2; j++) {
							View smallView = new View(10 + 45 * i, 10 + 45 * j, 35, 35);
							smallView.setBackgroundColor(new Color(colors[i * 2 + j]));
							view.addSubview(smallView);
							if (i == 1 && j == 0) {
								View tinyView = new View(10, 10, 15, 15);
								tinyView.setBackgroundColor(Color.black);
								smallView.addSubview(tinyView);
							}
						}
					}
				} else if (x == 1 && y == 0) {
					// clip
					View v1 = new View(-10, 10, 120, 80);
					v1.setBackgroundColor(new Color(0xffffcc));
					view.addSubview(v1);
					View v2 = new View(20, -10, 80, 100);
					v2.setBackgroundColor(new Color(0xccffcc));
					v2.setClipped(false);
					v1.addSubview(v2);
					View v3 = new View(-5, 5, 10, 10) {
						@Override
						protected void mouseClicked(MouseEvent e) {
							System.out.println("a");
						}
					};
					v3.setBackgroundColor(Color.black);
					v2.addSubview(v3);
					
					view.setClipped(true);
					v1.setClipped(true);
					v2.setClipped(true);
					v3.setClipped(true);
				} else if (x == 2 && y == 0) {
					// hidden, alpha
					View target = view;
					for (int i = 0; i < 20; i++) {
						View v1 = new View(5, 5, 100, 100);
						v1.setBackgroundColor(Color.red);
						if (i == 0) {
							v1.setAlpha(0.1f);
						} else if (i == 10) {
							v1.setHidden(true);
						}
						target.addSubview(v1);
						target = v1;
					}
					view.setClipped(true);
				} else if (x == 3 && y == 0) {
					// view subclassing
					View circle = new CircleView(10, 10, 60, 60).setFillColor(Color.red);
					circle.setAlpha(0.5f);
					view.addSubview(circle);
					circle = new CircleView(40, 20, 60, 60).setFillColor(Color.green);
					circle.setAlpha(0.5f);
					view.addSubview(circle);
					circle = new CircleView(20, 30, 60, 60).setFillColor(Color.blue);
					circle.setAlpha(0.5f);
					view.addSubview(circle);
				} else if (x == 0 && y == 1) {
					// image
					View imgView = new ImageView(10, 10, 70, 70);
					view.addSubview(imgView);
					imgView = new ImageView(60, 60, 30, 30);
					view.addSubview(imgView);
				} else if (x == 1 && y == 1) {
					// basic animation
					final View v = new CircleView(0, 0, 20, 20).setFillColor(Color.orange);
					view.addSubview(v);
					final Animation animation = new Animation(2.0f) {
						@Override
						protected void animate(float lambda) {
							v.setPosition((int) (lambda * 100), (int) (lambda * 100));
						}
						@Override
						protected void completed(boolean cancel) {
							commit();
						}
					};
					animation.commit();
					view.setClipped(true);
				} else if (x == 2 && y == 1) {
					// key event
					KeyView v1 = new KeyView(10, 10, 40, 40) {
						protected void keyTyped(KeyEvent e) {
							super.keyTyped(e);
							if (e.getKeyChar() == '2') {
								getSuperView().getSubview(2).requestKeyboardFocus();
							}
						}
					};
					view.addSubview(v1);
					KeyView v2 = new KeyView(10, 50, 40, 40);
					view.addSubview(v2);
					KeyView v3 = new KeyView(50, 10, 40, 40);
					view.addSubview(v3);
					KeyView v4 = new KeyView(50, 50, 40, 40);
					view.addSubview(v4);
				} else if (x == 3 && y == 1) {
					// mouse event
					view.setAlpha(0.1f);
					for (int i = 0; i < 10; i++) {
						View v1 = new MouseView(5 + i * 5, 5 + i * 5, 100, 100);
						view.addSubview(v1);
					}
					view.setClipped(true);
				} else if (x == 0 && y == 2) {
					// dragging
					View v = new CircleView(0, 0, 20, 20) {
						int dx, dy;
						@Override
						protected void mousePressed(MouseEvent e) {
							Vector2D point = e.getPosition(this); // magic code
							dx = point.x;
							dy = point.y;
							e.handle();
						}
						@Override
						protected void mouseDragged(MouseEvent e) {
							Vector2D point = e.getPosition(view);
							setPosition(point.x - dx, point.y - dy);
							e.handle();
						}
					};
					view.addSubview(v);
					view.setClipped(true);
				} else if (x == 1 && y == 2) {
					// rendering animation
					ImageView v = new ImageView(10, 10, 70, 70) {
					};
					view.addSubview(v);
				} else if (x == 2 && y == 2) {
					View view2 = new View(0, 0, 100, 100) {
						@Override
						protected void mouseReleased(MouseEvent e) {
							System.out.println(e.getVelocity(this));
						}
					};
					view2.setBackgroundColor(Color.green);
					view.addSubview(view2);
				} else if (x == 3 && y == 2) {
					View view2 = new View(0, 0, 100, 100) {
						@Override
						protected void mouseMoved(MouseEvent e) {
							setAlpha(e.getPosition(this).y / 100.0f);
						}
						@Override
						protected void mouseDragged(MouseEvent e) {
							setAlpha(e.getPosition(this).y / 100.0f);
						}
					};
					view2.setBackgroundColor(Color.red);
					view.addSubview(view2);
				}
				rootView.addSubview(view);
			}
		}
		rootView.getViewContext().setDebugModeEnable(true);
//		rootView.getViewContext().prepareContent();
		frame.setVisible(true);
	}
	
}
