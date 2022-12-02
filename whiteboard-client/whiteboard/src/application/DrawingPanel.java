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

import application.drawings.Line;


/**
 * Drawing panel object that takes in user input
 * 
 * @author aks60
 *
 */
@SuppressWarnings("serial")
public class DrawingPanel extends JComponent {

	private Image image;
	Graphics2D graphics;
	
	private int currentX;
	private int currentY;
	private int oldX;
	private int oldY;
	
	private State state;
	
	GraphicsImplementer implementer;
	Synchroniser synchroniser;
	
	public DrawingPanel(Connector connector) {
		state = new State();
		setDoubleBuffered(false);
		implementer = new GraphicsImplementer(this);
		this.synchroniser = new Synchroniser(state, connector, implementer);
		synchroniser.start();
		
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				oldX = e.getX();
				oldY = e.getY();
			}
		});
		
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
		        currentX = e.getX();
		        currentY = e.getY();

		        if (graphics != null) {
		        	Line line = new Line(new int[] {oldX, oldY}, new int[] {currentX, currentY});
		        	implementer.draw(line);
		        	state.updateLock.lock();
		        	state.currentUpdate.append(line);
		        	state.updateLock.unlock();

					oldX = currentX;
					oldY = currentY;
		        }
			}
		});
	
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
	
}
