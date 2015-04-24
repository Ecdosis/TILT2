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
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TILT.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2015
 */
package tilt.image.geometry;

/**
 * Segment class. A portion of a line
 * @param p0 the first end-point
 * @param p1 the second end-point
 */
public class Segment
{
    int gap;
    public Point p0;
    public Point p1;
    static float EPSILON = 0.00000001f;
    public Segment( Point p0, Point p1 )
    {
        this.gap = 5;
        this.p0 = p0;
        this.p1 = p1;
    }
    /**
     * Do two line segments intersect?
     * @param b the other line segment
     * @return true if they do
     */
    public boolean intersects( Segment b ) 
    {
        Rect box1 = this.getBoundingBox();
        Rect box2 = b.getBoundingBox();
        return box1.intersects(box2)
                && this.crosses(b)
                && b.crosses(this);
    };
    /**
     * Get the bounding box of this segment
     * @return a Rect
     */
    public Rect getBoundingBox() 
    {
        return new Rect(Math.min(this.p0.x,this.p1.x),
            Math.min(this.p0.y,this.p1.y),
            Math.abs(this.p0.x-this.p1.x),
            Math.abs(this.p0.y-this.p1.y) );
    };
    /**
     * XOR two values (not in Javascript)
     * @param a the first boolean
     * @param b the second boolean
     * @return true if a ^ b
     */
    boolean xor( boolean a, boolean b ) 
    {
        return (a&&!b)||(!a&&b);
    }
    /**
     * Does this segment cross or touch another?
     * @param b the other segment
     * @return true if it does
     */
    public boolean crosses( Segment b ) 
    {
        boolean r1 = isPointRightOfLine(b.p0);
        boolean r2 = isPointRightOfLine(b.p1);
        boolean res = this.isPointOnLine(b.p0)
            || this.isPointOnLine(b.p1)
            || this.xor(r1,r2);
       return res;
    }
    /**
     * Does this segment intersect with a rectangle?
     * @param r the Rect to test intersection with
     * @return true if it is else false
     */
    public boolean intersectsWithRect( Rect r ) 
    {
        Point[] points = r.getPoints();
        for ( int i=0;i<points.length-1;i++ )
        {
            Segment edge = new Segment(points[i],points[i+1]);
            if ( this.crosses(edge) ) 
                return true;
        }
        return r.containsPt(this.p0)&& r.containsPt(this.p1);
    }
    /**
     * Is a point on the line within a certain tolerance?
     * http://martin-thoma.com/how-to-check-if-two-line-segments-intersect/
     * @param p the point to test
     * @return true if it is
     */
    public boolean isPointOnLine( Point p ) 
    {
        Segment aTmp = new Segment(new Point(0, 0), new Point(
            this.p1.x - this.p0.x, this.p1.y - this.p0.y));
        Point pTmp = new Point(p.x - this.p0.x, p.y - this.p0.y);
        int r = aTmp.p1.crossProduct(pTmp);
        return Math.abs(r) < this.EPSILON;
    };
    /**
     * Is a point to the right of the line but not touching it?
     * http://martin-thoma.com/how-to-check-if-two-line-segments-intersect/
     * @param p the point to test
     * @return true if it is to the right else false
     */
    private boolean isPointRightOfLine( Point p ) 
    {
        Segment aTmp = new Segment(new Point(0,0), new Point(
            this.p1.x-this.p0.x, this.p1.y-this.p0.y));
        Point pTmp = new Point(p.x-this.p0.x, p.y-this.p0.y);
        boolean res = aTmp.p1.crossProduct(pTmp) < 0;
        return res;
    }
    /**
     * Compute the hypoteneuse length
     * @return the diagonal length of the segment
     */
    public int hypoteneuse() 
    {
        int dy = this.p1.y-this.p0.y;
        int dx = this.p1.x-this.p0.x;
        return (int)Math.round(Math.abs(Math.sqrt(dy*dy+dx*dx)));
    }
    /**
     * Compute the distance a point is from a line segment
     * http://forums.codeguru.com/printthread.php?t=194400
     * @param c the point off the line
     * @return the distance in fractional pixels
     */
    public int distFromLine( Point c ) 
    {
        Point a = this.p0;
        Point b = this.p1;
        float r_numerator = (c.x-a.x)*(b.x-a.x) + (c.y-a.y)*(b.y-a.y);
        float r_denomenator = (b.x-a.x)*(b.x-a.x) + (b.y-a.y)*(b.y-a.y);
        float r = r_numerator / r_denomenator;
        float px = a.x + r*(b.x-a.x);
        float py = a.y + r*(b.y-a.y);  
        float s =  ((a.y-c.y)*(b.x-a.x)-(a.x-c.x)*(b.y-a.y) ) / r_denomenator;
        double distanceLine = Math.abs(s)*Math.sqrt(r_denomenator);
        double distanceSegment;
        float xx = px;
        float yy = py;
        if ( (r >= 0) && (r <= 1) )
            distanceSegment = distanceLine;
        else
        {
            float dist1 = (c.x-a.x)*(c.x-a.x) + (c.y-a.y)*(c.y-a.y);
            float dist2 = (c.x-b.x)*(c.x-b.x) + (c.y-b.y)*(c.y-b.y);
            if (dist1 < dist2)
            {
                xx = a.x;
                yy = a.y;
                distanceSegment = Math.sqrt(dist1);
            }
            else
            {
                xx = b.x;
                yy = b.y;
                distanceSegment = Math.sqrt(dist2);
            }
        }
        return (int)Math.round(distanceSegment);
    };
}