package co.gongzh.snail.layer;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;

import co.gongzh.snail.ViewGraphics;
import co.gongzh.snail.util.Vector2D;

public class ShapeLayer extends Layer {
	
	private Shape shape;
	private Stroke stroke;
	private Paint fillPaint;
	private Paint strokePaint;
	
	public ShapeLayer() {
		shape = null;
		stroke = null;
		fillPaint = null;
		strokePaint = null;
	}
	
	public final Shape getShape() {
		return shape;
	}
	
	public void setShape(Shape shape) {
		this.shape = shape;
		setNeedsRepaint();
	}
	
	public final Stroke getStroke() {
		return stroke;
	}
	
	public void setStroke(Stroke stroke) {
		this.stroke = stroke;
		setNeedsRepaint();
	}
	
	public final Paint getFillPaint() {
		return fillPaint;
	}
	
	public void setFillPaint(Paint paint) {
		this.fillPaint = paint;
		setNeedsRepaint();
	}
	
	public final Paint getStrokePaint() {
		return strokePaint;
	}
	
	public void setStrokePaint(Paint paint) {
		this.strokePaint = paint;
		setNeedsRepaint();
	}

	@Override
	protected void repaintLayer(ViewGraphics g) {
		if (fillPaint != null) {
			g.setPaint(fillPaint);
			g.fill(shape);
		}
		if (stroke != null || strokePaint != null) {
			if (strokePaint != null) g.setPaint(strokePaint);
			if (stroke != null) g.setStroke(stroke);
			g.draw(shape);
		}
	}
	
	@Override
	public boolean isInside(Vector2D point) {
		return shape.contains(point.toPoint());
	}

}
