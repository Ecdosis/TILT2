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
import tilt.exception.TiltException;
import tilt.image.page.Line;
import tilt.image.convexhull.GrahamScan;
import tilt.image.convexhull.Point2D;

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
    public int ID;
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
                    //case PathIterator.SEG_CLOSE: 
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
            if ( list.size() != this.npoints )
                System.out.println("Added polygon points during listification");
            return list;
        }
    }
    public void setID( int id )
    {
        this.ID = id;
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
     * Add a join point if it is not already in the set
     * @param res the set of join-points
     * @param p the point to add, maybe
     */
    private void addJoinPoint( ArrayList<Point> res, Point p )
    {
        boolean add = true;
        for ( Point r : res )
            if ( r.equals(p) )
            {
                add = false;
                break;
            }
        if ( add )
        {
            p.setType(PointType.join);
            res.add( p );
        }
    }
    /**
     * Add the points of intersection between a Segment and a polygon
     * @param last the last point
     * @param p the current point
     * @param flip true if the order of crossing segments is to be flipped
     */
    private ArrayList<Point> getIntersectingPoints( Point last, Point p,
        boolean flip )
    {
        Segment S = new Segment( last, p );
        ArrayList<Point> pts = this.toPoints();
        ArrayList<Point> res = new ArrayList<>();
        for ( int i=1;i<pts.size();i++ )
        {
            Point prev = pts.get(i-1);
            Point q = pts.get(i);
            Segment seg = new Segment(prev,q);
            if ( S.intersects(seg) )
            {
                Segment intersection = (flip)?seg.getIntersection(S)
                    :S.getIntersection(seg);
                addJoinPoint(res, intersection.p0 );
                if ( !intersection.p0.equals(intersection.p1) )
                    addJoinPoint( res, intersection.p1 );
            }
        }
        return res;
    }
    Point nearestPoint( ArrayList<Point> pts, Point last )
    {
        float minDist = Float.MAX_VALUE;
        int index = -1;
        for ( int i=0;i<pts.size();i++ )
        {
            Point p = pts.get(i);
            float dist = p.distance( last );
            if ( dist < minDist )
            {
                index = i;
                minDist = dist;
            }
        }
        return (index==-1)?null:pts.remove(index);
    }
    /**
     * Make a counter-clockwise list of all a polygon's points, adding 
     * join-points with another polygon
     * @param other the other polygon
     * @param flip true if we swap around the segment order for joins
     * @return a list of classified points, including joins
     */
    private ArrayList<Point> toCCList( Polygon other, boolean flip )
    {
        ArrayList<Point> pts = this.toPoints();
        ArrayList<Point> list = new ArrayList<>();
        Point last;
        Point curr = pts.get(0);
        curr.classify(other);
        for ( int i=1;i<pts.size();i++ )
        {
            last = curr;
            curr = pts.get(i);
            curr.classify(other);
            // check for crossover
            ArrayList<Point> joins = other.getIntersectingPoints( 
                last, curr, flip );
            while ( joins.size()>0 )
            {
                Point p = nearestPoint(joins,last);
                if ( p.equals(last) )
                    last.setType(PointType.join);
                else if ( p.equals(curr) )
                    curr.setType(PointType.join);
                else
                    list.add( p );
            }
            list.add(curr);
        }
        return list;
    }
    /**
     * Get the last point of a polygon
     * @return the current last point
     */
    public Point lastPoint()
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
    public Point firstPoint()
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
    boolean finished()
    {
        return this.npoints>1
            && this.firstPoint().equals(this.lastPoint());
    }
    /**
     * Weave two lists of polygon-points into an intersection
     * @param list1 the first list
     * @param pos the next read-position in list1
     * @param rea1 number of items of list1 read
     * @param list2 the second list
     * @param read2 number of items of list2 read
     * @param intersection the polygon to build as the intersection
     * @throws Exception 
     */
    private void weaveIntersection( ArrayList<Point> list1, int pos, int read1,
        ArrayList<Point> list2, int read2, Polygon intersection ) 
        throws TiltException
    {
        while ( !intersection.finished() && read1 <= list1.size() )
        {
            Point p = list1.get(pos);
            read1++;
            pos = (pos+1)%list1.size();
            if ( p.getType()==PointType.inner )
                intersection.addPoint( Math.round(p.x), Math.round(p.y) );
            else if ( p.getType()==PointType.join )
            {
                intersection.addPoint( Math.round(p.x), Math.round(p.y) );
                Point q = list1.get(pos);
                if ( q.getType()==PointType.outer )
                {
                    int loc = findLoc(list2,p);
                    if ( loc != -1 )
                    {
                        int next = (loc+1)%list2.size();
                        weaveIntersection( list2, next, read2+1, list1, read1, 
                            intersection );
                        break;
                    }
                    else
                        throw new TiltException("point "+p
                            +" not found in list"+printList(list2));
                }
            }
        }   
    }
    private String printList( ArrayList<Point> list )
    {
        StringBuilder sb = new StringBuilder();
        for ( Point p : list )
        {
            sb.append(p.toString());
        }
        return sb.toString();
    }
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        ArrayList<Point> pts = toPoints();
        for ( Point p : pts )
        {
            sb.append( p.toString() );
        }
        return sb.toString();
    }
    /**
     * Get a new polygon that is the intersection with this one
     * @param pg the polygon that should be intersected with this
     * @return the new polygon 
     */
    public Polygon getIntersection( Polygon pg ) throws TiltException
    {
        if ( this.contains(pg) )
            return pg;
        else if ( pg.contains(this) )
            return this;
        else
        {
            Polygon intersection = new Polygon();
            ArrayList<Point> list1 = this.toCCList( pg, true );
            ArrayList<Point> list2 = pg.toCCList( this, false );
//            System.out.println(printList(list1));
//            System.out.println(printList(list2));
            weaveIntersection( list1, 0, 0, list2, 0, intersection );
            return intersection;
        }
    }
    /**
     * Fins the location of a point in another list of points
     * @param list the list to search
     * @param target the target point
     * @return its index into list or -1 if not found
     */
    int findLoc( ArrayList<Point> list, Point target )
    {
        for ( int i=0;i<list.size();i++ )
        {
            Point p = list.get(i);
            if ( p.type==PointType.join && p.equals(target) )
                return i;
        }
        return -1;
    }
    /**
     * Weave the lists of points form two polygons with join-points into a union
     * @param list1 the first list of polygon points, counter-clockwise
     * @param pos the read position in list1
     * @param read1 number of items of list1 read
     * @param list2 the other list
     * @param read2 number of items in list2 read
     * @param union the polygon we are building
     * @throws TiltException 
     */
    private void weaveUnion( ArrayList<Point> list1, int pos, int read1,
        ArrayList<Point> list2, int read2, Polygon union ) throws TiltException
    {
        while ( !union.finished() && read1<=list1.size() )
        {
            Point p = list1.get(pos);
            pos = (pos+1)%list1.size();
            read1++;
            if ( p.getType()==PointType.outer )
                union.addPoint( Math.round(p.x), Math.round(p.y) );
            else if ( p.getType()==PointType.join )
            {
                union.addPoint( Math.round(p.x), Math.round(p.y) );
                int loc = findLoc(list2,p);
                if ( loc != -1 )
                {
                    int next = (loc+1)%list2.size();
                    weaveUnion( list2, next, read2+1, list1, read1, union );
                    break;
                }
                else
                    throw new TiltException("point "+p+" not found");
            }
        }
    }
    /**
     * Get a new polygon that is the union with this one
     * @param pg the polygon that should be unioned with this
     * @return the new polygon 
     */
    public Polygon getUnion( Polygon pg ) throws TiltException
    {
        if ( this.contains(pg) )
            return this;
        else if ( pg.contains(this) )
            return pg;
        else
        {
            Polygon union = new Polygon();
            ArrayList<Point> list1 = this.toCCList( pg, true );
            ArrayList<Point> list2 = pg.toCCList( this, false );
//            System.out.println(printList(list1));
//            System.out.println(printList(list2));
            weaveUnion( list1, 0, 0, list2, 0, union );
            return union;
        }
    }
    /**
     * Does one polygon intersects with another?
     * @param pg the other polygon
     * @return true if they do else false
     */
    public boolean intersects( Polygon pg )
    {
        if ( this.bounds.intersects(pg.bounds) )
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
            // test each edge of this to see if it crosses pg
            for ( int i=1;i<pts1.size();i++ )
            {
                Segment s1 = new Segment(pts1.get(i-1),pts1.get(i));
                for ( int j=1;j<pts2.size();j++ )
                {
                    Segment s2 = new Segment(pts2.get(j-1),pts2.get(j));
                    if ( s1.intersects(s2) )
                        return true;
                }
            }
        }
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
    /**
     * Get the minimal separation between us and another polygon
     * @param pg the second polygon
     * @return the smallest distance between segments or vertices of us, Q
     */
    public double distanceBetween( Polygon pg )
    {
        double minDist = Double.MAX_VALUE;
        ArrayList<Point> points1 = this.toPoints();
        ArrayList<Point> points2 = pg.toPoints();
        Point last1=null,last2=null;
        for ( int i=0;i<points1.size();i++ )
        {
            Point p1 = points1.get(i);
            for ( int j=0;j<points2.size();j++ )
            {
                Point p2 = points2.get(j);
                double x = Math.abs(p1.x-p2.x);
                double y = Math.abs(p1.y-p2.y);
                // distance between vertices
                float dist = Math.round(Math.hypot(x,y));
                if ( dist < minDist )
                    minDist = dist;
                // distance between p1 and a segment of Q
                if ( last2 != null )
                {
                    Segment seg = new Segment(last2,p2);
                    double fDist2 = seg.distFromLine( p1 );
                    if ( fDist2 < minDist )
                        minDist = fDist2;
                }
                // distance between p2 and a segment of P
                if ( last1 != null )
                {
                    Segment seg = new Segment( last1, p1 );
                    double fDist1 = seg.distFromLine( p2 );
                    if ( fDist1 < minDist )
                        minDist = fDist1;
                }
                last2 = p2;
            }
            last1 = p1;
        } 
        return minDist;
    }
    /**
     * Remove the duplicate end-points
     * @param pts an array of polygon points with possible closure
     */
    private void trimPoints( ArrayList<Point> pts )
    {
        if ( pts.size()>1 
            && pts.get(0).equals(pts.get(pts.size()-1)) )
            pts.remove(pts.size()-1);
    }
    /**
     * Merge two polygons into a minimal convex single polygon
     * @param pg2 the second poly
     * @return one Polygon being the merge of both
     */
    public Polygon merge( Polygon pg2 )
    {
        ArrayList<Point> points1 = this.toPoints();
        ArrayList<Point> points2 = pg2.toPoints();
        trimPoints(points1);
        trimPoints(points2);
        Point2D[] points3 = new Point2D[points1.size()+points2.size()];
        int j = 0;
        for ( int i=0;i<points1.size();i++ )
        {
            Point pt = points1.get(i);
            points3[j++] = new Point2D(pt.x,pt.y);
        }
        for ( int i=0;i<points2.size();i++ )
        {
            Point pt = points2.get(i);
            points3[j++] = new Point2D(pt.x,pt.y);
        }
        GrahamScan gs = new GrahamScan( points3 );
        return gs.toPolygon();
    }
    public static void main(String[] args)
    {
        Test.runTests();
    }
}
