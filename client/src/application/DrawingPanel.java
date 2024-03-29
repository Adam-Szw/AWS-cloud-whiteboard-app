package application;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JComponent;

import application.drawings.Circle;
import application.drawings.Line;


/**
 * Drawing panel object that takes in user input and reacts to actions
 * by sending messages to the state systems
 * 
 * @author aks60
 *
 */
@SuppressWarnings("serial")
public class DrawingPanel extends JComponent {

	public static int DRAW_TICKRATE = 20;
	
	public Image image;
	Graphics2D graphics;
	
	private int oldX = 0;
	private int oldY = 0;
	private long timer = 0;
	
	private State state;
	
	GraphicsImplementer implementer;
	Synchroniser synchroniser;
	
	public mode drawingMode = mode.brush;
	
	public enum mode {
		none, brush, line, rectangle, circle, text
	}
	
	private enum action {
		press, release, drag
	};
	
	public DrawingPanel(Connector connector) {
		state = new State();
		setDoubleBuffered(false);
		implementer = new GraphicsImplementer(this);
		this.synchroniser = new Synchroniser(state, connector, implementer);
		synchroniser.start();
		
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				updateFromAction(action.press, x, y);
			}
			
			public void mouseReleased(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				updateFromAction(action.release, x, y);
			}
		});
		
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				updateFromAction(action.drag, x, y);
			}
		});
	
	}
	
	private void updateFromAction(action action, int x, int y) {
		switch(action) {
		case press:
			oldX = x;
			oldY = y;
			break;
		case drag:
			if(drawingMode == mode.brush) {
		        if (graphics != null) {
		        	long time = System.currentTimeMillis() - timer;
		        	if(time >= DRAW_TICKRATE) {
		        		timer = System.currentTimeMillis();
			        	Line line = new Line(new int[] {oldX, oldY}, new int[] {x, y});
			        	drawShape(line);
						oldX = x;
						oldY = y;
		        	}
		        }
			}
			if(drawingMode == mode.line) {
				 if (graphics != null) {
					 refresh();
					 Line line = new Line(new int[] {oldX, oldY}, new int[] {x, y});
					 implementer.draw(line);
				 }
			}
			if(drawingMode == mode.rectangle) {
				if (graphics != null) {
					refresh();
					for(Line l : getRectLines(new int[] {oldX, oldY}, new int[] {x, y})) {
						implementer.draw(l);
					}
				}
			}
			if(drawingMode == mode.circle) {
				 if (graphics != null) {
					 refresh();
					 Circle circle = new Circle(new int[] {oldX, oldY}, new int[] {x, y});
					 implementer.draw(circle);
				 }
			}
			break;
		case release:
			if(drawingMode == mode.line) {
		        if (graphics != null) {
		        	Line line = new Line(new int[] {oldX, oldY}, new int[] {x, y});
		        	drawShape(line);
			        refresh();
		        }
			}
			if(drawingMode == mode.rectangle) {
		        if (graphics != null) {
					for(Line l : getRectLines(new int[] {oldX, oldY}, new int[] {x, y})) {
						drawShape(l);
					}
			        refresh();
		        }
			}
			if(drawingMode == mode.circle) {
		        if (graphics != null) {
		        	Circle circle = new Circle(new int[] {oldX, oldY}, new int[] {x, y});
		        	drawShape(circle);
			        refresh();
		        }
			}
			break;
		default:
			break;
		}
	}
	
	protected void paintComponent(Graphics g) {
		if (image == null) {
			image = createImage(getSize().width, getSize().height);
			graphics = (Graphics2D) image.getGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			clear();
		}
		g.drawImage(image, 0, 0, null);
	}
	
	public void clear() {
		graphics.setPaint(Color.white);
		graphics.fillRect(0, 0, getSize().width, getSize().height);
		graphics.setPaint(Color.black);
		repaint();
	}
	
	public void refresh() {
    	state.stateLock.lock();
    	state.updateLock.lock();
    	implementer.clear();
    	for(UpdateGroup update : state.totalState) {
    		implementer.implement(update);
    	}
    	implementer.implement(state.currentUpdate);
    	state.updateLock.unlock();
    	state.stateLock.unlock();
	}
	
	private void drawShape(Line line) {
    	implementer.draw(line);
    	state.updateLock.lock();
    	state.currentUpdate.append(line);
    	state.updateLock.unlock();
	}
	
	private void drawShape(Circle circle) {
    	implementer.draw(circle);
    	state.updateLock.lock();
    	state.currentUpdate.append(circle);
    	state.updateLock.unlock();
	}
	
	private Line[] getRectLines(int[] start, int[] end) {
		Line[] lines = new Line[4];
		lines[0] = new Line(new int[] {start[0], start[1]}, new int[] {end[0], start[1]});
		lines[1] = new Line(new int[] {end[0], start[1]}, new int[] {end[0], end[1]});
		lines[2] = new Line(new int[] {end[0], end[1]}, new int[] {start[0], end[1]});
		lines[3] = new Line(new int[] {start[0], end[1]}, new int[] {start[0], start[1]});
		return lines;
	}
	
	void clearState() {
		Thread clrThread = new Thread(new Runnable() {
			@Override
			public void run() {
				clear();
				synchroniser.sendClearUpdate();
			}
		});
		clrThread.start();
	}
	
}
