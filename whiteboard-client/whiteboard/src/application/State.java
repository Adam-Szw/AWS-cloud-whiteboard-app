package application;

import java.util.ArrayList;
import java.util.List;

import application.drawings.Shape;


public class State {
	
	Comms comms;
	
	public State(Comms comms) {
		this.comms = comms;
	}
	
	public List<Shape> shapes = new ArrayList<Shape>();
	
	public void append(Shape shape) {
		shapes.add(shape);
		uploadState();
	}
	
	public String toString() {
		String str = "";
		for(Shape shape : shapes) {
			str += shape.toString() + ";";
		}
		return str;
	}
	
	void uploadState() {
		comms.message = this.toString();
		clear();
	}
	
	void clear() {
		shapes = new ArrayList<Shape>();
	}
}