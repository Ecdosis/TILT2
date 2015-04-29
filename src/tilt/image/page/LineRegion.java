/*
 * This file is part of TILT.
 *
 *  TILT is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  TILT is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TILT.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2015
 */

package tilt.image.page;
import tilt.image.geometry.Point;
import tilt.image.geometry.Polygon;
import tilt.image.geometry.Segment;
import java.awt.image.BufferedImage;

/**
 * Create a region belonging to a line by examining the previous and next line.
 * @author desmond
 */
public class LineRegion 
{
    Line next,prev,current;
    int lineHeight;
    BufferedImage src;
    Polygon poly;
    /**
     * Create a region around a line
     * @param src the source image we lie on top of
     * @param current the current line
     * @param prev the previous line (maybe null)
     * @param next the next line (maybe null)
     * @param lineHeight estimated average line-height
     */
    public LineRegion( BufferedImage src, Line current, Line prev, 
        Line next, int lineHeight )
    {
        this.prev = prev;
        this.next = next;
        this.current = current;
        this.lineHeight = lineHeight;
        this.src = src;
    }
    /**
     * Compute the region of the line within which all word-shapes must lie.
     * @return a non-convex polygon
     */
    private void calculate()
    {
        Polygon poly = new Polygon();
        Point[] pts = current.getPoints();
        if ( pts.length > 0 )
        {
            if ( prev == null )
            {
                // first line
                for ( int i=pts.length-1;i>=0;i-- )
                {
                    float tentativeY = pts[i].y-lineHeight/2;
                    int y = (tentativeY<0)?0:Math.round(tentativeY);
                    poly.addPoint( Math.round(pts[i].x), y );
                }
            }
            else
            {
                // go through all the point-pairs from right to left
                Point[] topPts = prev.getPoints();
                int i = pts.length-1;
                int j = topPts.length-1;
                float dist = Integer.MAX_VALUE;
                int best = 0;
                // NB check first and last points for perpendicularity
                while ( i >= 0 )
                {
                    Point p = pts[i];
                    Point q = topPts[j];
                    float newDist = p.distance(q);
                    if ( newDist < dist )
                    {
                        best = j;
                        if ( j > 0 )
                            j--;
                        dist = newDist;
                    }
                    else
                    {
                        float x = (topPts[best].x+pts[i].x)/2;
                        // straight sides for polygon at ends
                        if (i==0 || i==pts.length-1)
                            x = pts[i].x;
                        poly.addPoint(Math.round(x),
                            Math.round(topPts[best].y+pts[i].y)/2);
                        i--;
                        j = best;
                    }
                }
            }
            if ( next == null )
            {
                // last line
                int maxY = src.getHeight()-1;
                for ( int i=0;i<pts.length;i++ )
                {
                    float tentativeY = pts[i].y+lineHeight/2;
                    float y = (tentativeY>maxY)?maxY:tentativeY;
                    poly.addPoint( Math.round(pts[i].x), Math.round(y) );
                }
            }
            else
            {
                // go through all the point-pairs from left to right
                Point[] botPts = next.getPoints();
                int i = 0;
                int j = 0;
                float dist = Integer.MAX_VALUE;
                int best = 0;
                // NB check first and last points for perpendicularity
                while ( i < pts.length )
                {
                    Point p = pts[i];
                    Point q = botPts[j];
                    float newDist = p.distance(q);
                    if ( newDist < dist )
                    {
                        best = j;
                        if ( j < botPts.length-1 )
                            j++;
                        dist = newDist;
                    }
                    else
                    {
                        float x = (botPts[best].x+pts[i].x)/2;
                        // straight sides for polygon at ends
                        if ( i==0||i==pts.length-1 )
                            x = pts[i].x;
                        poly.addPoint(Math.round(x),
                            Math.round((botPts[best].y+pts[i].y)/2));
                        i++;
                        j = best;
                    }
                }
            }
            // close the loop
            poly.addPoint(poly.xpoints[0], poly.ypoints[0]);
        }
        // remember for next time
        this.poly = poly;
    }
    /**
     * Find the Y-intercept of a line extended from a point and the polygon
     * @param p0 the point to be extended vertically as a line
     * @param above if true return the y-coordinate of the top intercept
     * @return the above or below intercept point's y-value
     */
    int getYIntercept( Point p0, boolean above )
    {
        Point bot = new Point(p0.x,src.getHeight()-1);
        Point top = new Point(p0.x,0);
        Segment S = new Segment(top,bot);
        Segment IS = new Segment(top,bot);
        if ( poly == null )
            this.getPoly();
        //Polygon.printPolygonPoints( poly );
        System.out.println("line:["+S.p0.x+','+S.p0.y+"]["+S.p1.x+","+S.p1.y+"]");
        if ( this.poly.intersectsLine( S, IS ) )
        {
            System.out.println("intersects: ["+IS.p0.x+','+IS.p0.y+"]["+IS.p1.x+","+IS.p1.y+"]");
            if ( above )
            {
                Point p = (IS.p0.y>IS.p1.y)?IS.p1:IS.p0;
                float y = p.y+(p0.y-p.y)/2;
                return Math.round(y);
            }
            else
            {
                Point p = (IS.p0.y>IS.p1.y)?IS.p0:IS.p1;
                float y = p0.y+(p.y-p0.y)/2;
                return Math.round(y);
            }
        }
        else
            return Math.round(p0.y);
    }
    /**
     * Get the sub-region of the LineRegion closest to the line
     * @param p0 the lhs of the baseline
     * @param p1 the rhs of the baseline
     * @return a polygon
     */
    public Polygon getLineBase( Point p0, Point p1 )
    {
        // clockwise
        Polygon pg = new Polygon();
        int y = getYIntercept( p0, true );
        pg.addPoint( Math.round(p0.x),y );
        Point start = new Point( p0.x, y );
        y = getYIntercept( p1, true );
        pg.addPoint( Math.round(p1.x),y );
        y = getYIntercept( p1, false );
        pg.addPoint( Math.round(p1.x), y );
        y = getYIntercept( p0, false );
        pg.addPoint( Math.round(p0.x), y );
        pg.addPoint( Math.round(start.x), Math.round(start.y) );
        return pg;
    }
    /**
     * Get the polygonal region of this line
     * @return a polygon, possibly modified by external agents
     */
    public Polygon getPoly()
    {
        if ( this.poly == null )
            calculate();
        if ( this.poly.points == null )
            this.poly.toPoints();
        return this.poly;
    }
    /**
     * Update the line's polygon
     * @param poly the new polygon
    */
    public void setPoly( Polygon poly )
    {
        this.poly = poly;
    }
    public static void main( String[] args )
    {
        LineRegion lr = new LineRegion(null, null, null, 
            null, 0 );
        lr.poly = new Polygon();
        lr.poly.addPoint(1,5);
        lr.poly.addPoint(4,1);
        lr.poly.addPoint(7,4);
        lr.poly.addPoint(5,7);
        lr.poly.addPoint(1,5);
        lr.poly.toPoints();
        int yAbove = lr.getYIntercept(new Point(4,4), true);
        int yBelow = lr.getYIntercept(new Point(4,4), false);
        System.out.println("y-intercept above: "+yAbove
            +" below:"+yBelow);
    }
}
