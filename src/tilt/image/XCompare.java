package tilt.image;

import java.util.Comparator;
import java.awt.Point;
class XCompare implements Comparator<Point>
{
	@Override
	public int compare(Point o1, Point o2) 
	{
		return (new Integer(o1.x)).compareTo(new Integer(o2.x));
	}
}
