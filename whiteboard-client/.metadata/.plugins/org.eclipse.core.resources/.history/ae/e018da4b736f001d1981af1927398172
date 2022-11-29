package application;

import application.drawings.Line;
import application.drawings.Shape;

public class GraphicsImplementer {
	
	DrawingPanel panel;
	
	public GraphicsImplementer(DrawingPanel panel) {
		this.panel = panel;
	}
	
	public void draw(Line line) {
		panel.graphics.drawLine(line.start[0], line.start[1], line.end[0], line.end[1]);
		panel.repaint();
	}

}
