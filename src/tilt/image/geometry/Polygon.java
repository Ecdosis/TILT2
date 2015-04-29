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
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.HashSet;
import tilt.image.page.Line;
import java.util.Iterator;

/**
 * Override the awt Polygon class so we can store them as keys in a HashMap
 * These methods only work with CONVEX polygons
 * @author desmond
 */
public class Polygon extends java.awt.Polygon
{
    static int MOD_ADLER = 65521;
    static float SMALL_NUM = 0.0000000001f;
    public Point[] points;
    /** line we are attached to */
    Line line;
    Rect bounds;
    /**
     * Convert a polygon to its constituent points
     * @return a list of points
     */
    public ArrayList<Point> toPoints()
    {
        ArrayList<Point> list = new ArrayList<Point>();
        if ( this.points != null )
        {
            for ( int i=0;i<points.length;i++ )
                list.add( points[i]);
            return list;
        }
        else
        {
            float[] coords = new float[6];
            PathIterator iter = this.getPathIterator(null);
            while ( !iter.isDone() )
            {
                Point pt;
                int step = iter.currentSegment(coords);
                switch ( step )
                {
                    case PathIterator.SEG_LINETO:
                    case PathIterator.SEG_MOVETO:
                        pt = new Point(Math.round(coords[0]),Math.round(coords[1]));
                        list.add(pt);
                    break;
                default:
                    break;
                }
                iter.next();
            }
            this.points = new Point[list.size()];
            list.toArray(this.points);
            computeBounds();
            return list;
        }
    }
    /**
     * Recompute the bounds
     */
    void computeBounds()
    {
        float minX = Integer.MAX_VALUE;
        float minY = Integer.MAX_VALUE;
        float maxX = Integer.MIN_VALUE;
        float maxY = Integer.MIN_VALUE;
        for ( int i=0;i<this.points.length;i++ )
        {
            Point pt = this.points[i];
            if ( pt.x < minX )
                minX = pt.x;
            if ( pt.y < minY )
                minY = pt.y;
            if ( pt.x > maxX )
                maxX = pt.x;
            if ( pt.y > maxY )
                maxY = pt.y;
        }
        this.bounds = new Rect( Math.round(minX), 
            Math.round(minY), Math.round(maxX-minX), 
            Math.round(maxY-minY) );
    }
    /**
     * Convert our list of points to a string
     * @param list the list of points in the polygon
     * @return a string
     */
    private String listToString( )
    {
        ArrayList<Point> points = toPoints();
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<points.size();i++ )
        {
            Point p = points.get(i);
            sb.append( p.x );
            sb.append( "," );
            sb.append( p.y );
            if ( i<points.size()-1 )
                sb.append(":");
        }
        return sb.toString();
    }
    /**
     * Return a (nearly) unique hash for this object
     * @return an int
     */
    @Override
    public int hashCode()
    {
        String s = listToString();
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
    /**
     * Is this polygon equal to another (used to assess membership of hashmap)
     * @param obj the other Polygon to compare to
     * @return true if this is equal to obj
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( obj instanceof Polygon )
        {
            Polygon pg = (Polygon) obj;
            String s1 = this.listToString();
            String s2 = pg.listToString();
            return s1.equals(s2);
        }
        else
            return false;
    }
    /**
     * Compute the centroid of this polygon
     * @param pts the points to get the centroid of
     * @return the centre point
     */
    public Point getCentroid( Point[] pts )
    {
        float totalX = 0;
        float totalY = 0;
        for ( int i=0;i<pts.length;i++ )
        {
            totalX += pts[i].x;
            totalY += pts[i].y;
        }
        return new Point( totalX/pts.length, totalY/pts.length );
    }
    /**
     * Does a line intersect with this <em>convex</em> polygon and if so where
     * @param S the segment intersecting with the Polygon
     * @param IS VAR param: set this to intersecting points
     * @return true if segment S intersects with us
     */
    public boolean intersectsLine( Segment S, Segment IS ) 
    {
        if ( S.p0.equals(S.p1) )
        {
            IS.p0 = S.p0;
            IS.p1 = S.p1;
            return pointInPoly(S.p0);
        }
        else
        {
            float tE = 0;
            float tL = 1;
            float t, N, D;
            Point dS = S.p1.minus(S.p0);
            Point e;
            for (int i=0;i<this.points.length-1;i++) 
            {
                e = this.points[i+1].minus(this.points[i]);
                N = e.perp(S.p0.minus(this.points[i])); 
                D = -e.perp(dS);
                if (Math.abs(D) < SMALL_NUM) 
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
            // tE <= tL implies that there is a valid intersection subsegment
            IS.p0 = S.p0.plus(dS.times(tE));   // = P(tE) = point where S enters polygon
            IS.p1 = S.p0.plus(dS.times(tL));   // = P(tL) = point where S leaves polygon
            return true;
        }
    }
    /**
     * Is the given point inside this possibly non-convex polygon?
     * http://stackoverflow.com/questions/11716268/point-in-polygon-algorithm
     * @param pt the point to test for
     * @return true if it was else false 
     */
    public boolean pointInPoly( Point pt ) 
    {
        int i,j,nvert = points.length;
        boolean c = false;
        for ( i=0,j=nvert-1;i<nvert;j=i++ ) 
        {
            if ( ((this.points[i].y >= pt.y) != (this.points[j].y >= pt.y)) 
                && (pt.x <= (this.points[j].x-this.points[i].x)
                *(pt.y-this.points[i].y)/(this.points[j].y-this.points[i].y)
                +this.points[i].x) )
                c = !c;
        }
        return c;
    };
    public Line getLine()
    {
        return line;
    }
    public void setLine( Line line )
    {
        this.line = line;
    }
    /**
     * Compute the area of a polygon
     * @return the area of the irregular polygon
     */
    public int area()
    {
        ArrayList<Point> points = toPoints();
        Point start = points.get(0);
        Point curr = null;
        Point last = null;
        int area = 0;
        for ( int i=0;i<points.size();i++ )
        {
            last = curr;
            curr = points.get(i);
            if ( last != null && curr != start )
            {
                if ( last.x < curr.x )
                    area += ((last.y+curr.y)/2) * (curr.x-last.x);
                else
                    area -= ((last.y+curr.y)/2) * (last.x-curr.x);
            }
        }
        return area;
    }
    /**
     * Does one polygon entirely contain another polygon?
     * @param pg the polygon that we contain?
     * @return true if it does else false
     */
    public boolean contains( Polygon pg )
    {
        ArrayList<Point> pgPoints = pg.toPoints();
        boolean pgInside = true;
        for ( int i=0;i<pgPoints.size();i++ )
        {
            if ( !this.contains(pgPoints.get(i)) )
            {
                pgInside = false;
                break;
            }
        }
        return pgInside;
    }
    /**
     * Add the points of intersection between a Segment and a polygon
     * @param last the last point
     * @param p the current point
     * @param set the set to add the intersection points to
     */
    private void addIntersectingPoints( Point last, Point p, 
        HashSet<Point> set )
    {
        Segment S = new Segment( last, p );
        ArrayList<Point> pts = this.toPoints();
        for ( int i=1;i<pts.size();i++ )
        {
            Point prev = pts.get(i-1);
            Point q = pts.get(i);
            Segment seg = new Segment(prev,q);
            if ( S.intersects(seg) )
            {
                Segment intersection = S.getIntersection(seg);
                set.add( intersection.p0 );
                if ( !intersection.p0.equals(intersection.p1) )
                    set.add( intersection.p1 );
            }
        }
    }
    /**
     * Ensure the points are ordered counter-clockwise
     * @param array an array of all the points of a polygon
     * @param centroid of the polygon whose points are to be sorted
     */
    void sortCC( Point[] array, Point centroid )
    {
        // shell sort points counter-clockwise about centroid
        int h = array.length / 2;
	while (h > 0) 
        {
            for (int i = h; i < array.length; i++) 
            {
                int j = i;
                Point temp = array[i];
                while (j >= h && array[j-h].compareWith(temp,centroid) > 0 ) 
                {
                    array[j] = array[j-h];
                    j = j - h;
                }
                array[j] = temp;
            }
            if (h == 2) 
                h = 1;
            else
                h *= (5.0 / 11);
	}
    }
    /**
     * Make a list of a polygon's points, including joins with another polygon
     * @param pg1 the other polygon
     * @param pg2 this polygon
     * @param outside if true gather outer points, else inner
     * @param set the set of points to add it to
     * @return the points of pg2 outside of pg1 plus join points
     */
    private void findPoints( Polygon pg1, Polygon pg2, boolean outer, 
        HashSet<Point> set )
    {
        ArrayList<Point> pgList = pg2.toPoints();
        Point last;
        Point p = null;
        boolean pIsInside = false;
        boolean lastIsInside;
        for ( int i=0;i<pgList.size();i++ )
        {
            last = p;
            lastIsInside = pIsInside;
            p = pgList.get(i);
            pIsInside = pg1.contains(p);
            if ( !pIsInside && lastIsInside )
            {
                // coming out
                pg1.addIntersectingPoints( last, p, set );
                if ( outer )
                    set.add(p);
            }
            else if ( pIsInside && !lastIsInside )
            {
                // going in
                if ( last != null )
                    pg1.addIntersectingPoints( last, p, set );
                if ( !outer )
                    set.add(p);
            }
            else if ( !pIsInside )
            {
                if ( last != null )
                    pg1.addIntersectingPoints( last, p, set );
                if ( outer )
                    set.add(p);
            }
            else if ( !outer )
            {
                set.add(p);
            }
        }
    }
    /**
     * Get the last point of a polygon
     * @return the current last point
     */
    Point lastPoint()
    {
        if ( this.npoints > 0 )
        {
            int x = this.xpoints[this.npoints-1];
            int y = this.ypoints[this.npoints-1];
            return new Point(x,y);
        }
        else
            return null;
    }
    /**
     * Get the last point of a polygon
     * @return the current last point
     */
    Point firstPoint()
    {
        if ( this.npoints > 0 )
        {
            int x = this.xpoints[0];
            int y = this.ypoints[0];
            return new Point(x,y);
        }
        else
            return null;
    }
    /**
     * Make sure that the first and last points of this polygon are the same
     * @param pg the polygon to check
     */
    private void fixPolygon( Polygon pg )
    {
        Point q = pg.lastPoint();
        Point p = pg.firstPoint();
        if ( !q.equals(p) )
            pg.addPoint(Math.round(p.x),Math.round(p.y));
    }
    /**
     * Get a new polygon that is the intersection with this one
     * @param pg the polygon that should be intersected with this
     * @return the new polygon 
     */
    public Polygon getIntersection( Polygon pg )
    {
        return getIntersectionOrUnion( pg, false );
    }
    /**
     * Get a new polygon that is the union with this one
     * @param pg the polygon that should be unioned with this
     * @return the new polygon 
     */
    public Polygon getUnion( Polygon pg )
    {
        return getIntersectionOrUnion( pg, true );
    }
    /**
     * Get a new polygon that is the intersection or union with this one
     * @param pg the polygon that intersects with this
     * @param union true if a union is wanted else intersection
     * @return the new polygon being the intersection (empty == no intersection)
     */
    private Polygon getIntersectionOrUnion( Polygon pg, boolean union )
    {
        HashSet<Point> pts = new HashSet<>();
        findPoints( this, pg, union, pts );
        findPoints( pg, this, union, pts );
        Polygon combined = new Polygon();
        Point[] array = new Point[pts.size()];
        pts.toArray( array );
        Point centroid = getCentroid( array );
        sortCC( array, centroid );
        for ( Point pt : array )
        {
            combined.addPoint(Math.round(pt.x),Math.round(pt.y));
        }
        fixPolygon(combined);
        return combined;
    }
    /**
     * Does one polygon intersects with another?
     * @param pg the other polygon
     * @return true if they do else false
     */
    public boolean intersects( Polygon pg )
    {
        ArrayList<Point> pts1 = this.toPoints();
        // two polys intersect if at least one point is
        // "inside" the other
        ArrayList<Point> pts2 = pg.toPoints();
        for ( int i=0;i<pts1.size();i++ )
            if ( pg.contains(pts1.get(i)) )
                return true;
        for ( int i=0;i<pts2.size();i++ )
            if ( this.contains(pts2.get(i)) )
                return true;
        return false;
    }
    /**
     * Does this polygon intersect with a rectangle?
     * @param r the rectangle to test
     * @return true if it does else false
     */
    public boolean intersectsWithRect( Rect r ) 
    {
        if ( bounds.intersects(r) )
        {
            // 1. r is completely inside us
            if ( r.inside(bounds) )
                return true;
            // 2. we are entirely inside r
            else if ( bounds.inside(r) )
                return true;
            else
            {
                // 3. at least one polygon point is inside r
                for ( int i=0;i<this.points.length;i++ )
                {
                    if ( r.containsPt(this.points[i]) )
                        return true;
                }
                // 4. at least one edge intersects with r
                for ( int i=0;i<this.points.length-1;i++ )
                {
                    Segment edge = new Segment(this.points[i],this.points[i+1]);
                    if ( edge.intersectsWithRect(r) ) 
                        return true;
                }
            }
        }
        // else: most cases will fall through to here
        return false;
    }
    public static void main(String[] args)
    {
        Test.runTests();
    }
}
