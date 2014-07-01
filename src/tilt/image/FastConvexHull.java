package tilt.image;
/**
 * License: GNU GPL v2
 * @author adam.larkeryd
 * Lifted from: http://code.google.com/p/convex-hull/source/browse/
 * Convex+Hull/src/algorithms/FastConvexHull.java?r=4
 */
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;

public class FastConvexHull
{
    /**
     * Find the convex hull of a set of points. 
     * @param points an array of at least 3 points
     * @return a list of points representing the convex hull.
     */
	public static ArrayList<Point> execute(ArrayList<Point> points) 
	{
		if ( points.size()> 2 )
        {
            ArrayList<Point> xSorted = new ArrayList<Point>(points);
            Collections.sort(xSorted, new XCompare());
            int n = xSorted.size();
            Point[] lUpper = new Point[n];
            lUpper[0] = xSorted.get(0);
            lUpper[1] = xSorted.get(1);
            int lUpperSize = 2;
            for (int i = 2; i < n; i++)
            {
                lUpper[lUpperSize] = xSorted.get(i);
                lUpperSize++;
                while (lUpperSize > 2 && !rightTurn(lUpper[lUpperSize - 3], 
                    lUpper[lUpperSize - 2], lUpper[lUpperSize - 1]))
                {
                    // Remove the middle point of the three last
                    lUpper[lUpperSize - 2] = lUpper[lUpperSize - 1];
                    lUpperSize--;
                }
            }
            Point[] lLower = new Point[n];
            lLower[0] = xSorted.get(n - 1);
            lLower[1] = xSorted.get(n - 2);
            int lLowerSize = 2;
            for (int i = n - 3; i >= 0; i--)
            {
                lLower[lLowerSize] = xSorted.get(i);
                lLowerSize++;
                while (lLowerSize > 2 && !rightTurn(lLower[lLowerSize - 3], 
                    lLower[lLowerSize - 2], lLower[lLowerSize - 1]))
                {
                    // Remove the middle point of the three last
                    lLower[lLowerSize - 2] = lLower[lLowerSize - 1];
                    lLowerSize--;
                }
            }
            ArrayList<Point> result = new ArrayList<Point>();
            for (int i = 0; i < lUpperSize; i++)
            {
                result.add(lUpper[i]);
            }
            for (int i = 1; i < lLowerSize - 1; i++)
            {
                result.add(lLower[i]);
            }
            return result;
        }
        else
            return points;
	}
	private static boolean rightTurn(Point a, Point b, Point c)
	{
		return (b.x - a.x)*(c.y - a.y) - (b.y - a.y)*(c.x - a.x) > 0;
	}
}

