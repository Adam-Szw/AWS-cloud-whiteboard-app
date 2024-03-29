package application.drawings;

/**
 * 
 * @author aks60
 *
 */
public class Line extends Shape {
	
	public int[] start;
	public int[] end;
	
	public static String strStart = "L:";
	
	public Line(int[] start, int[] end) {
		this.start = start;
		this.end = end;
	}
	
	public Line(String str) {
		str = str.substring(strStart.length());
		String[] coords = str.split("->");
		String[] start = coords[0].split(",");
		String[] end = coords[1].split(",");
		this.start = new int[] {Integer.valueOf(start[0]), Integer.valueOf(start[1])};
		this.end = new int[] {Integer.valueOf(end[0]), Integer.valueOf(end[1])};
	}

	@Override
	public String toString() {
		String str = "";
		str += strStart;
		str += start[0] + "," + start[1] + "->" + end[0] + "," + end[1];
		return str;
	}

}
