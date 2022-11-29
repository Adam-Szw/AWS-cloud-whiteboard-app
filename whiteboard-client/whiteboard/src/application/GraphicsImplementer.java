package application;

import application.drawings.Line;
import application.drawings.Shape;

public class GraphicsImplementer {
	
	DrawingPanel panel;
	
	enum ShapeType {
		line
	}
	
	public GraphicsImplementer(DrawingPanel panel) {
		this.panel = panel;
	}
	
	public void draw(Line line) {
		panel.graphics.drawLine(line.start[0], line.start[1], line.end[0], line.end[1]);
		panel.repaint();
	}

	public static ShapeType decode(String str) {
		ShapeType type = null;
		
		if(str.length() >= 4 && str.substring(0, 4).equals("Line")) {
			return ShapeType.line;
		}
		return type;
	}
	
	public void implement(UpdateGroup group) {
		for(int i = 0; i < group.shapes.size(); i++) {
			Shape shape = group.shapes.get(i);
			if(shape instanceof Line) {
				System.out.print("adding missing line");
				draw((Line) shape);
			}
		}
	}
	
}
