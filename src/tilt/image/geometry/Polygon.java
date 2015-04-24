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
import tilt.image.page.Line;

/**
 * Override the awt Polygon class so we can store them as keys in a HashMap
 * These methods only work with CONVEX polygons
 * @author desmond
 */
public class Polygon extends java.awt.Polygon
{
    static int MOD_ADLER = 65521;
    static float SMALL_NUM = 0.0000000001f;
    Point centroid;
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
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
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
        this.bounds = new Rect( minX, minY, maxX-minX, maxY-minY );
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
     * @return the centre point
     */
    public Point getCentroid()
    {
        if ( centroid == null )
        {
            int xSum = 0;
            int ySum = 0;
            for ( int i=0;i<points.length;i++ )
            {
                Point p = points[i];
                xSum += p.x;
                ySum = p.x;
            }
            centroid = new Point(xSum/points.length,ySum/points.length);
        }
        return centroid;
    }
    /**
     * Does a line intersect with this polygon and if so where
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
     * Is the given point inside this polygon?
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
    private void addIntersectingPoints( Point last, Point p, 
        ArrayList<Point> list )
    {
        Segment S = new Segment( last, p );
        Segment IS = new Segment( last, p );
        if ( this.intersectsLine(S,IS) )
        {
            if ( !IS.p0.equals(last) )
            {
                IS.p0.x = -IS.p0.x;
                IS.p0.y = -IS.p0.y;
                list.add( IS.p0 );
            }
            if ( !IS.p1.equals(p) )
            {
                IS.p1.x = -IS.p1.x;
                IS.p1.y = -IS.p1.y;
                list.add( IS.p1 );
            }
        }
    }
    /**
     * Make a counter-clockwise list for a polygon's points
     * @param pg1 the other polygon
     * @param pg2 this polygon
     * @param outside if true gather outer points, else inner
     * @return the points of pg2 outside of pg1 plus negated join points
     */
    ArrayList<Point> makeCCList( Polygon pg1, Polygon pg2, boolean outer )
    {
        ArrayList<Point> list = new ArrayList<Point>();
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
                pg1.addIntersectingPoints( last, p, list );
                if ( outer )
                    list.add(p);
            }
            else if ( pIsInside && !lastIsInside )
            {
                if (  last != null )
                    pg1.addIntersectingPoints( last, p, list );
                if ( !outer )
                    list.add(p);
            }
            else if ( !pIsInside )
            {
                if ( last != null )
                    pg1.addIntersectingPoints( last, p, list );
                if ( outer )
                    list.add(p);
            }
            else if ( pIsInside && !outer )
            {
                list.add(p);
            }
        }
        return list;
    }
    /**
     * Get the last point of a polygon
     * @param poly the polygon
     * @return the current last point
     */
    Point lastPoint( Polygon poly )
    {
        if ( poly.npoints > 0 )
        {
            int x = poly.xpoints[poly.npoints-1];
            int y = poly.ypoints[poly.npoints-1];
            return new Point(x,y);
        }
        else
            return null;
    }
    /**
     * Find a connection point in a CC list of points
     * @param list the list of points to search
     * @param p the point to look for
     * @return its index in list or -1 if not found
     */
    int findInCCList(ArrayList<Point> list, Point p )
    {
        for ( int i=0;i<list.size();i++ )
        {
            Point q = list.get(i);
            if ( q.x == p.x && q.y == p.y )
                return i;
        }
        return -1;
    }
    /**
     * Get the next position in the list treating it as a circular buffer
     * @param pos the current position, maybe at list end
     * @param list the list
     * @return the next circular position
     */
    private int nextPos( int pos, ArrayList list )
    {
        return (pos+1)%list.size();
    }
    private boolean isCC( ArrayList<Point> list, int pos, int next )
    {
        return next !=0 || !(list.get(pos).x<0 & list.get(next).x<0);
    }
    /**
     * Read a list and switch to another list when you hit a negative point
     * @param list1 the list to start from
     * @param pos the next read position in list1
     * @param read1 the number of points in list1 read so far
     * @param list2 the list to switch to
     * @param read2 the number of points in list2 read so far
     * @param poly the polygon to add points to
     */
    void readCCList( ArrayList<Point> list1, int pos, int read1, 
        ArrayList<Point> list2, int read2, Polygon poly )
    {
        while ( read1 < list1.size() )
        {
            Point p = list1.get(pos);
            read1++;
            if ( p.x >= 0 )
            {
                if ( !p.equals(lastPoint(poly)) )
                    poly.addPoint(p.x,p.y);
                pos = nextPos(pos,list1);
            }
            else
            {
                poly.addPoint( -p.x, -p.y );
                if ( pos != 0 )
                {
                    int loc = findInCCList(list2,p);
                    if ( loc >= 0 )
                    {
                        int next = nextPos(loc,list2);
                        if ( isCC(list2,loc,next) )
                        {
                            readCCList( list2, next, read2+1, list1, read1, poly );
                            break;
                        }
                        else
                            pos = nextPos(pos,list1);
                    }
                    else
                        break;
                }
                else
                    pos = nextPos(pos,list1);
            }
        }
    }
    /**
     * Debug: print CC list
     * @param list the counter-clockwise list of selected points
     */
    private void printList( ArrayList<Point> list )
    {
        for ( int i=0;i<list.size();i++ )
            System.out.print("["+list.get(i).x+","+list.get(i).y+"]");
        System.out.println("");
    }
    /**
     * Get the union of two polygons
     * @param pg the second polygon
     * @return the union of them and us
     */
    public Polygon getUnion( Polygon pg )
    {
        ArrayList<Point> list1 = makeCCList( this, pg, true );
        printList( list1 );
        ArrayList<Point> list2 = makeCCList( pg, this, true );
        printList(list2);
        Polygon union = new Polygon();
        readCCList( list1, 0, 0, list2, 0, union );
        return union;
    }
    /**
     * Get a new polygon that is the intersection with this one
     * @param pg the polygon that intersects with this
     * @return the new polygon being the intersection (empty == no intersection)
     */
    public Polygon getIntersection( Polygon pg )
    {
        ArrayList<Point> list1 = makeCCList( this, pg, false );
        printList(list1);
        ArrayList<Point> list2 = makeCCList( pg, this, false );
        printList(list2);
        Polygon intersection = new Polygon();
        readCCList( list1, 0, 0, list2, 0, intersection );
        return intersection;
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
    static void printPolygonPoints( Polygon poly )
    {
        ArrayList<Point> list = poly.toPoints();
        for ( int i=0;i<list.size();i++ )
            System.out.print("["+list.get(i).x+","+list.get(i).y+"]");
        System.out.println("");
    }
    public static void main(String[] args)
    {
        Polygon pg1 = new Polygon();
        // points have to be counter-clockwise
        pg1.addPoint(70,40);
        pg1.addPoint(50,70);
        pg1.addPoint(10,50);
        pg1.addPoint(40,10);
        pg1.addPoint(70,40);
        pg1.toPoints();
        Polygon pg2 = new Polygon();
        pg2.addPoint(40,30);
        pg2.addPoint(30,40);
        pg2.addPoint(10,30);
        pg2.addPoint(10,10);
        pg2.addPoint(30,10);
        pg2.addPoint(40,30);
        pg2.toPoints();
        Polygon union = pg1.getUnion(pg2);
        printPolygonPoints(union);
        Polygon intersection = pg1.getIntersection(pg2);
        printPolygonPoints(intersection);
        Polygon pg3 = new Polygon();
        pg3.addPoint(10,20);
        pg3.addPoint(70,20);
        pg3.addPoint(70,60);
        pg3.addPoint(10,60);
        pg3.addPoint(10,20);
        pg3.toPoints();
        Polygon pg4 = new Polygon();
        pg4.addPoint(30,70);
        pg4.addPoint(30,10);
        pg4.addPoint(50,10);
        pg4.addPoint(50,70);
        pg4.addPoint(30,70);
        pg4.toPoints();
        Polygon union2 = pg3.getUnion(pg4);
        printPolygonPoints(union2);
        Polygon intersection2 = pg3.getIntersection(pg4);
        printPolygonPoints(intersection2);
    }
}
