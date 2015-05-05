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
import java.awt.geom.Rectangle2D;

public class Rect extends Rectangle2D.Float
{
    /**
    * A simple rectangle class
    * @param x the x-position of top-left
    * @param y the y-position of top-left
    * @param width the width of the rect
    * @param height the height of the rect
    */
    public Rect( float x, float y, float width, float height )
    {
        super(x,y,width,height);
    }
    /**
     * Is a point inside the rectangle?
     * @param p the point to test
     * @return true if it is else false
     */
    public boolean contains( Point p ) 
    {
        if ( p.x==540.0f && p.y==1048.0f && this.x == 540.0 && this.y==1048.0f )
            System.out.println("why?");
        return p.x >= this.x && p.x < this.x+this.width 
            && p.y >= this.y && p.y < this.y+this.height;
    }
    /**
     * Convert this rectangle to a string for debugging
     * @return a string 
     */
    public String toString()
    {
        return"x="+this.x+",y="+this.y+",width="
            +this.width+",height="+this.height;
    }
    /**
     * Send a representation of ourself to the console
     */
    public void print()
    {
        System.out.println(toString());
    }
    /**
     * Outset/inset this rectangle
     * @param by the amount to inset (negative) or outset (positive)
     */
    public void expand( int by ) 
    {
        this.x -= by;
        this.y -= by;
        this.width += by*2;
        this.height += by*2;
    }
    /**
     * Does this rectangle intersect with another?
     * @param b the other rectangle
     * @return true if it does else false
     */
    public boolean intersects( Rect b ) 
    {
        boolean res = ((this.x <= b.x && this.x+this.width >= b.x)
            || (b.x <= this.x && b.x+b.width >= this.x))
            && ((this.y <= b.y && this.y+this.height >= b.y)
            || (b.y <= this.y && b.y+b.height >= this.y));
        return res;
    }
    /**
     * Get an array of points of the 4 corners
     * @return an array of four points being the corners
     */
    Point[] getPoints() 
    {
        Point[] points = new Point[4];
        points[0] = new Point(this.x,this.y);
        points[1] = new Point(this.x+this.width,this.y);
        points[2] = new Point(this.x+this.width,this.y+this.height);
        points[3] = new Point(this.x,this.y+this.height);
        return points;
    }
    /**
     * Is this point wholly within on the edge of the rect?
     * @param pt the point to test
     * @return true if it is true else false
     */
    boolean containsPt( Point pt ) 
    {
        return pt.x >= this.x && pt.y >= this.y 
            && pt.x <= this.x+this.width && pt.y<= this.y+this.height;
    }
    /**
     * Is this rectangle entire inside another?
     * @param r the other rectangle
     * @return true if it is
     */
    public boolean inside( Rect r ) 
    {
        return this.x >= r.x && this.x+this.width<=r.x+r.width 
            && this.y>=r.y&&this.y+this.height<=r.y+r.height;
    }
}
