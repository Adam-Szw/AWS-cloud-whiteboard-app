package application;

import application.drawings.Circle;
import application.drawings.Line;
import application.drawings.Shape;

/**
 * Actual drawing in on the board happens here
 * 
 * @author aks60
 *
 */
public class GraphicsImplementer {
	
	DrawingPanel panel;
	
	enum ShapeType {
		line, circle
	}
	
	public GraphicsImplementer(DrawingPanel panel) {
		this.panel = panel;
	}
	
	public void draw(Line line) {
		panel.graphics.drawLine(line.start[0], line.start[1], line.end[0], line.end[1]);
		panel.repaint();
	}
	
	public void draw(Circle circle) {
		int width = Math.abs(circle.end[0] - circle.start[0]);
		int height = Math.abs(circle.end[1] - circle.start[1]);
		panel.graphics.drawOval(circle.start[0], circle.start[1], width, height);
		panel.repaint();
	}

	public static ShapeType decode(String str) {
		ShapeType type = null;
		
		if(strBegins(str, Line.strStart)) return ShapeType.line;
		if(strBegins(str, Circle.strStart)) return ShapeType.circle;
		return type;
	}
	
	public void implement(UpdateGroup group) {
		for(int i = 0; i < group.shapes.size(); i++) {
			Shape shape = group.shapes.get(i);
			if(shape instanceof Line) {
				draw((Line) shape);
			}
			if(shape instanceof Circle) {
				draw((Circle) shape);
			}
		}
	}
	
	public void clear() {
		panel.clear();
	}
	
	static boolean strBegins(String str, String compare) {
		int length = compare.length();
		if(length > str.length()) return false;
		String sub = str.substring(0, length);
		if(sub.equals(compare)) return true;
		return false;
	}
	
}
