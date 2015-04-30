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
public class Point extends java.awt.geom.Point2D.Float
{
    static int MOD_ADLER = 65521;
    PointType type;
    /**
     * @param x the-coordinate
     * @param y the y-coordinate
     */
    public Point( float x, float y )
    {
        this.x = x;
        this.y = y;
    }
    public void print() 
    {
        System.out.println(this.toString());
    }
    public void setType ( PointType type )
    {
        this.type = type;
    }
    public PointType getType()
    {
        return this.type;
    }
    public void classify( Polygon other )
    {
        if ( other.contains(this) )
            this.type = PointType.inner;
        else
            this.type = PointType.outer;
    }
    public String toString() 
    {
        return "["+this.x+","+this.y+"]";
    }
    public java.awt.Point toAwt()
    {
        return new java.awt.Point(Math.round(this.x),Math.round(this.y));
    }
    /**
     * Is this polygon equal to another (used to assess membership of hashmap)
     * @param obj the other Polygon to compare to
     * @return true if this is equal to obj
     */
    @Override
    public boolean equals( Object obj ) 
    {
        if ( obj instanceof Point )
        {
            Point pt = (Point)obj;
            return pt!= null &&this.x==pt.x&&this.y==pt.y;
        }
        else
            return false;
    }
    public Point minus( Point other )
    {
        return new Point(this.x-other.x,this.y-other.y);
    }
    public Point plus( Point other ) 
    {
        return new Point(this.x+other.x,this.y+other.y);
    }
    public float perp( Point v )
    {
        return this.x*v.y-this.y*v.x;
    }
    public float dot( Point v ) 
    {
       return this.x*v.x+this.y*v.y;
    }
    public Point times( float factor ) 
    {
        return new Point(this.x*factor,this.y*factor );
    }
    public float crossProduct( Point b )
    {
        return this.x*b.y-b.x*this.y;
    }
    public boolean inRect( Rect r ) 
    {
        return this.x >= r.x && this.x <=r.x+r.width 
         && this.y >= r.y && this.y <= r.y+r.height;
    }
    public float distance( Point pt2 ) 
    {
        float xDiff = Math.abs(this.x-pt2.x);
        float yDiff= Math.abs(this.y-pt2.y);
        float ysq = yDiff*yDiff;
        float xsq = xDiff*xDiff;
        // good old pythagoras
        return (int)Math.round(Math.sqrt(ysq+xsq));
    }
    /**
     * Compare one point with another based on its polar coordinates
     * @param other the other point
     * @param centroid the origin of the set of points we all belong to
     * @return 1 if we are greater than other, else -1 if less else 0
     */
    public int compareWith( Point other, Point centroid )
    {
        // use the anti-tan of the angle with centroid as discrimiator
        double angle1 = Math.atan2(this.y-centroid.y, this.x-centroid.x);
        double angle2 = Math.atan2(other.y-centroid.y, other.x-centroid.x);
        return (angle1-angle2)>0?1:(angle1==angle2)?0:-1;
    }
    /**
     * Return a (nearly) unique hash for this object
     * @return an int
     */
    @Override
    public int hashCode()
    {
        String s = this.toString();
        byte[] data = s.getBytes();
        int a = 1, b = 0;
        int index;

        /* Process each byte of the data in order */
        for (index = 0; index < data.length; ++index)
        {
            a = (a + data[index]) % MOD_ADLER;
            b = (b + a) % MOD_ADLER;
        }
        return (b << 16) | a;
    }
}
