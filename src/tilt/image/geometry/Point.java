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
 * A simple point class
 */
public class Point extends java.awt.Point
{
    /**
     * @param x the-coordinate
     * @param y the y-coordinate
     */
    public Point( int x, int y )
    {
        this.x = x;
        this.y = y;
    }
    public void print() 
    {
        System.out.println(this.toString());
    }
    public String toString() 
    {
        return "x="+this.x+",y="+this.y;
    }
    public boolean equals( Point pt ) 
    {
        return pt!= null &&this.x==pt.x&&this.y==pt.y;
    }
    public Point minus( Point other )
    {
        return new Point(this.x-other.x,this.y-other.y);
    }
    public Point plus( Point other ) 
    {
        return new Point(this.x+other.x,this.y+other.y);
    }
    public int perp( Point v )
    {
        return this.x*v.y-this.y*v.x;
    }
    public int dot( Point v ) 
    {
       return this.x*v.x+this.y*v.y;
    }
    public Point times( float factor ) 
    {
        return new Point(Math.round(this.x*factor),
            Math.round(this.y*factor) );
    }
    public int crossProduct( Point b )
    {
        return this.x*b.y-b.x*this.y;
    }
    public boolean inRect( Rect r ) 
    {
        return this.x >= r.x && this.x <=r.x+r.width 
         && this.y >= r.y && this.y <= r.y+r.height;
    }
    public int distance( Point pt2 ) 
    {
        int xDiff = Math.abs(this.x-pt2.x);
        int yDiff= Math.abs(this.y-pt2.y);
        int ysq = yDiff*yDiff;
        int xsq = xDiff*xDiff;
        // good old pythagoras
        return (int)Math.round(Math.sqrt(ysq+xsq));
    };
}
