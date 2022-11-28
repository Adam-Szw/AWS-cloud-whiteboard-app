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



public class DrawingPanel extends JComponent {

	private Image image;
	Graphics2D graphics;
	
	private int currentX;
	private int currentY;
	private int oldX;
	private int oldY;
	
	GraphicsImplementer implementer;
	
	public DrawingPanel(State state) {
		setDoubleBuffered(false);
		implementer = new GraphicsImplementer(this);
		
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
		        	state.append(line);

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