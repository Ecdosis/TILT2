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
 *  (c) copyright Desmond Schmidt 2014
 */   

package tilt.image.page;
import tilt.image.geometry.Polygon;
import tilt.image.geometry.Point;
import java.util.ArrayList;
import java.util.Iterator;
import tilt.exception.SplitException;
import tilt.image.convexhull.*;
import java.awt.Rectangle;
import tilt.Utils;
/**
 * Manage a shape we are creating by splitting an existing one
 * @author desmond
 */
public class SplitShape 
{
    int left;
    int right;
    static final double SMALL_NUM = 0.00000001;
    ArrayList<Point2D> points;
    /**
     * Create a SplitShape object
     * @param left the left limit
     * @param right the right limit (not included)
     */
    SplitShape( int left, int right )
    {
        this.left = left;
        this.right = right;
        points = new ArrayList<>();
    }
    /**
     * Do we want this point?
     * @param x the x-position of the point
     * @return true if it falls within our purview
     */
    boolean wants( int x )
    {
        return x >= left && x < right;
    }
    /**
     * Add a point to the nascent polygon
     * @param p the point to add
     */
    void add( Point2D p )
    {
        points.add( p );
    }
    double dot( Point2D u, Point2D v )
    {
        return u.x() * v.x() + u.y() * v.y();
    }
    double perp( Point2D u, Point2D v )  
    {
        return u.x() * v.y() - u.y() * v.x();
    }
    int cn_PnPoly( Point2D P, Point2D[] V )
    {
        int cn = 0;
        for ( int i=0; i<V.length-1; i++ ) 
        {
            if (((V[i].y <= P.y) && (V[i+1].y > P.y))
            || ((V[i].y > P.y) && (V[i+1].y <=  P.y))) 
            {
                double vt = (P.y  - V[i].y) / (V[i+1].y - V[i].y);
                if (P.x <  V[i].x + vt * (V[i+1].x - V[i].x))
                     ++cn;
            }
        }
        return cn&1;
    }
    boolean intersectsLine( Point2D[] pts, Point2D p0, Point2D p1, Point2D in, Point2D out )
    {
        if ( p0 == p1 )    
        {
            return cn_PnPoly(p0,pts)==1;
        }
        double tE = 0;
        double tL = 1;
        double t, N, D;
        Point2D dS = p1.minus(p0);
        Point2D e; 
        for ( int i=0; i<this.points.size()-1; i++)
        {
            e = this.points.get(i+1).minus(this.points.get(i));
            N = this.perp(e, p0.minus(this.points.get(i)));
            D = -this.perp(e, dS);
            if (Math.abs(D) < this.SMALL_NUM) 
            {
                if (N < 0)
                     return false;
                else
                     continue;
            }
            t = N / D;
            if (D < 0) 
            {
                if (t > tE) 
                {
                     tE = t;
                     if (tE > tL)
                         return false;
                }
            }
            else 
            {
                if (t < tL) 
                {
                     tL = t;
                     if (tL < tE)
                         return false;
                }
            }
        }
        in.x = p0.x + tE * dS.x; 
        in.y = p0.y + tE * dS.y;
        out.x = p0.x + tL * dS.x; 
        out.y = p0.y + tL * dS.y; 
        return true;
    }
    private Point2D[] toPoint2D( ArrayList<Point> pts )
    {
        Point2D[] array = new Point2D[pts.size()];
        int i = 0;
        for ( Point p : pts )
        {
            array[i++] = new Point2D(p.x,p.y);
        }
        return array;
    }
    void addLeftIntersectPoints( Polygon shape )
    {
        Rectangle sb = shape.getBounds();
        ArrayList<Point> pts = shape.toPoints();
        Point2D[] array = toPoint2D( pts );
        Point2D p0 = new Point2D(left,sb.y);
        Point2D p1 = new Point2D(left,sb.y+sb.height);
        Point2D in = new Point2D(0,0);
        Point2D out = new Point2D(0,0);
        intersectsLine( array, p0, p1, in, out );
        if ( in.x !=0 && in.y != 0 )
            this.points.add( in );
        if ( out.x != 0 && out.y != 0 )
            this.points.add( out );
    }
    void addRightIntersectPoints( Polygon shape )
    {
        Rectangle sb = shape.getBounds();
        ArrayList<Point> pts = shape.toPoints();
        Point2D[] array = toPoint2D( pts );
        Point2D p0 = new Point2D(right-1,sb.y);
        Point2D p1 = new Point2D(right-1,sb.y+sb.height);
        Point2D in = new Point2D(0,0);
        Point2D out = new Point2D(0,0);
        intersectsLine( array, p0, p1, in, out );
        if ( in.x !=0 && in.y != 0 )
            this.points.add( in );
        if ( out.x != 0 && out.y != 0 )
            this.points.add( out );
    }
    /**
     * Get the number of points
     * @return an int
     */
    int numPoints()
    {
        return points.size();
    }
    /**
     * Get the bounds of this shape
     * @return an awt Rectangle
     */
    Rectangle getBounds()
    {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        for ( int i=0;i<points.size();i++ )
        {
            Point2D p = points.get(i);
            int pX = (int)Math.round(p.x());
            int pY = (int)Math.round(p.y());
            if ( pX < minX )
                minX = pX;
            if ( pY < minY )
                minY = pY;
            if ( pX > maxX )
                maxX = pX;
            if ( pY >maxY )
                maxY = pY;
        }
        return new Rectangle( minX, minY, maxX-minX, maxY-minY);
    }
    /**
     * Convert the points list to a polygon
     * @return an ordinary awt Polygon
     */
    Polygon getPoly() throws SplitException
    {
        // close the generated shapes
        if ( points.size() > 1 )
        {
            Point2D p = points.get(0);
            Point2D q = points.get(points.size()-1);
            if ( !p.equals(q) )
                points.add( new Point2D(p.x(),p.y()) );
            // convert to polygons
            Point2D[] newPolyArray = new Point2D[points.size()];
            points.toArray(newPolyArray);
            GrahamScan gs = new GrahamScan( newPolyArray );
            Iterable<Point2D> trimmedPts = gs.hull();
            Iterator<Point2D> iter = trimmedPts.iterator();
            Polygon pg = new Polygon();
            while ( iter.hasNext() )
            {
                Point2D pt = iter.next();
                pg.addPoint( (int)Math.round(pt.x()), 
                    (int)Math.round(pt.y()) );
            }
            return pg;
        }
        else
            throw new SplitException("Polygon only has "+points.size()+" points");
    }
}
