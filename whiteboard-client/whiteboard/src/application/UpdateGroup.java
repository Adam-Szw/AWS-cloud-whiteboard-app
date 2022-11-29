package application;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import application.drawings.Line;
import application.drawings.Shape;


public class UpdateGroup implements Comparable<UpdateGroup> {
	
	public List<Shape> shapes;
	public long id;
	public boolean empty = true;
	
	public UpdateGroup() {
		this.shapes = new ArrayList<Shape>();
		this.id = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
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
