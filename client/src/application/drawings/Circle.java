package application.drawings;

/**
 * 
 * @author aks60
 *
 */
public class Circle extends Shape {
	
	public int[] start;
	public int[] end;
	
	public static String strStart = "C:";
	
	public Circle(int[] start, int[] end) {
		 int[] cStart = new int[] {
				 Math.min(start[0], end[0]),
				 Math.min(start[1], end[1])
		 };
		 int[] cEnd = new int[] {
				 Math.max(start[0], end[0]),
				 Math.max(start[1], end[1])
		 };
		this.start = cStart;
		this.end = cEnd;
	}
	
	public Circle(String str) {
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
