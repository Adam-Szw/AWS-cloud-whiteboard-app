package application;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * 
 * Class for managing the visual aspects of the white board application client
 * 
 * @author aks60
 *
 */
public class Whiteboard {
	
	private JFrame frame;
	private Container content;
	private Connector connector;
	private DrawingPanel panel;
	
	public Whiteboard(Connector connector) {
		this.connector = connector;
	    frame = new JFrame("Whiteboard App");
	    content = frame.getContentPane();
	    content.setLayout(new BorderLayout());
	    addDrawingPanel();
	    addButtons();
	}
	
	private void addDrawingPanel() {
	    panel = new DrawingPanel(connector);
	    content.add(panel, BorderLayout.CENTER);
	}
	
	private void addButtons() {
	    JPanel buttonsPanel = new JPanel();
	    GridLayout gl = new GridLayout(0, 1);
	    buttonsPanel.setLayout(gl);
	    
	    JButton btnClear = new JButton("Clear");
	    btnClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				panel.clearState();
			}
	    });
	    JButton btnBrush = new JButton("Brush");
	    btnBrush.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				panel.drawingMode = DrawingPanel.mode.brush;
			}
	    });
	    JButton btnLine = new JButton("Line");
	    btnLine.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				panel.drawingMode = DrawingPanel.mode.line;
			}
	    });
	    JButton btnRect = new JButton("Rectangle");
	    btnRect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				panel.drawingMode = DrawingPanel.mode.rectangle;
			}
	    });
	    JButton btnCircle = new JButton("Elipse");
	    btnCircle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				panel.drawingMode = DrawingPanel.mode.circle;
			}
	    });
	    buttonsPanel.add(btnClear);
	    buttonsPanel.add(btnBrush);
	    buttonsPanel.add(btnLine);
	    buttonsPanel.add(btnRect);
	    buttonsPanel.add(btnCircle);
	    
	    content.add(buttonsPanel, BorderLayout.WEST);
	}
	
	public void open(int width, int height) {
	    frame.setSize(width, height);
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setVisible(true);
	}

}
