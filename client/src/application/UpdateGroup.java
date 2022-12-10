package application;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import application.drawings.Circle;
import application.drawings.Line;
import application.drawings.Shape;

/**
 * Class for grouping updates together. Updates are the shapes that need to be
 * drawn in order to replicate the state of the board. It has ID for synchronisation purposes.
 * 
 * @author aks60
 *
 */
public class UpdateGroup implements Comparable<UpdateGroup> {
	
	public List<Shape> shapes;
	public long id;
	public boolean empty = true;
	
	public UpdateGroup() {
		this.shapes = new ArrayList<Shape>();
		Random rd = new Random();
		this.id = Math.abs(rd.nextLong());
	}
	
	public UpdateGroup(long id) {
		this.shapes = new ArrayList<Shape>();
		this.id = id;
	}
	
	public void append(Shape shape) {
		shapes.add(shape);
		empty = false;
	}
	
	public void append(String str) {
		GraphicsImplementer.ShapeType shape = GraphicsImplementer.decode(str);
		if(shape == GraphicsImplementer.ShapeType.line) shapes.add(new Line(str));
		if(shape == GraphicsImplementer.ShapeType.circle) shapes.add(new Circle(str));
	}
	
	public String toString() {
		String str = "";
		str += "ID:" + id + ";";
		for(Shape shape : shapes) {
			str += shape.toString() + ";";
		}
		return str;
	}
	
	void clear() {
		shapes = new ArrayList<Shape>();
		empty = true;
	}

	@Override
	public int compareTo(UpdateGroup o) {
		return ((Long)id).compareTo((Long)o.id);
	}
}
