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

package tilt.image.page;
import tilt.image.geometry.Rect;
import tilt.image.geometry.Point;
import tilt.image.geometry.Polygon;
import java.util.ArrayList;

/**
 * A quadrtree is a 2D binary tree. We use it to store points and 
 * shapes for quick lookup.
 * @author desmond
 */
class QuadTree
{
    Rect boundary;
    /** points inside the quadrant */
    ArrayList<Point> points;
    /** polygons overlapping this quadrant */
    ArrayList<Polygon> polygons;
    /** max points in a quadrant */
    static final int NUM_POINTS = 6;
    /** the four quadrants */
    QuadTree nw;
    QuadTree ne;
    QuadTree se;
    QuadTree sw;
    /** dimensions */
    int width;
    int height;
    int x;
    int y;
    /**
     * A 2-D binary tree
     * @param x the top-left x-position
     * @param y the top-left y position
     * @param width the <em>fraction</em> of width of area covered
     * @param height the <em>fraction</em> of height of area covered
     */
    QuadTree( int x, int y, int width, int height )
    {
        this.boundary = new Rect(x,y,width,height);
        this.width = width;
        this.height = height;
        this.boundary = new Rect(x,y,width,height);
        this.points = new ArrayList<>();
    }
    /**
     * Add a point to the quadtree
     * @param pt the point to add
     * @return true if it was added else false
     */
    boolean addPt( Point pt )
    {
        if ( !this.contains(pt) )
        {
            return false;
        }
        else 
        {
            if ( points != null )
            {
                if ( points.size() < NUM_POINTS )
                {
                    points.add( pt );
                    return true;
                }
                else
                {
                    subdivide();
                }
            }
            if ( this.nw.addPt(pt) )
                return true;
            else if ( this.sw.addPt(pt) )
                return true;
            else if ( this.ne.addPt(pt) )
                return true;
            else if ( this.se.addPt(pt) )
                return true;
            else
            {
                System.out.println("add failed for point "+pt.x+","+pt.y);
                return false;
            }
        }
    }
    /**
     * Find the quadrant to which this point belongs
     * @param pt the point to find
     * @return the quadrant in which it is stored or undefined
     */
    QuadTree getQuad( Point pt ) 
    {
        if ( this.points != null )
        {
            for ( int i=0;i<this.points.size();i++ )
            {
                if ( this.points.get(i) == pt )
                {
                    return this;
                }
            }
            System.out.println("failed to find quad for point "+pt.x+","+pt.y);
            return null;
        }
        else if ( this.nw.contains(pt) )
            return this.nw.getQuad(pt);
        else if ( this.sw.contains(pt) )
            return this.sw.getQuad(pt);
        else if ( this.ne.contains(pt) )
            return this.ne.getQuad(pt);
        else if ( this.se.contains(pt) )
            return this.se.getQuad(pt);
        else
        {
            System.out.println("failed to find quad for point "+pt.x+","+pt.y);
            return null;
        }
    };
    /**
     * Remove a point from the quadtree
     * @param pt the point to remove
     */
    void removePt( Point pt ) 
    {
        if ( this.points != null )
        {
            for ( int i=0;i<this.points.size();i++ )
            {
                if ( this.points.get(i) == pt )
                {
                    this.points.remove(pt);
                    return;
                }
            }
            System.out.println("failed to remove point "+pt.x+","+pt.y);
        }
        else if ( this.nw.contains(pt) )
            this.nw.removePt(pt);
        else if ( this.sw.contains(pt) )
            this.sw.removePt(pt);
        else if ( this.ne.contains(pt) )
            this.ne.removePt(pt);
        else if ( this.se.contains(pt) )
            this.se.removePt(pt);
        else
            System.out.println("failed to remove point "+pt.x+","+pt.y);
    }
    /**
     * Does this quadrant contain the given point?
     * @param pt the point to test for
     * @return true if it is contained in our boundary
     */
    boolean contains( Point pt ) 
    {
        return this.boundary.contains(pt);
    }
    /**
     * Split into four equal quadrants, and reassign points, polygons
     */
    void subdivide() 
    {
        // precompute x,y of se quadrant
        int xSplit = this.boundary.width/2+this.boundary.x;
        int ySplit = this.boundary.height/2+this.boundary.y;
        // create the four sub-trees
        this.nw = new QuadTree(this.boundary.x,this.boundary.y,
            xSplit-this.boundary.x,
            ySplit-this.boundary.y);
        this.sw = new QuadTree(this.boundary.x,ySplit,
            xSplit-this.boundary.x,
            (this.boundary.y+this.boundary.height)-ySplit);
        this.ne = new QuadTree(xSplit,this.boundary.y,
            (this.boundary.x+this.boundary.width)-xSplit,
            ySplit-this.boundary.y);
        this.se = new QuadTree(xSplit,ySplit,
            (this.boundary.x+this.boundary.width)-xSplit,
            (this.boundary.y+this.boundary.height)-ySplit);
        // now send the points to their respective quadrants
        for ( int i=0;i<points.size();i++ )
        {
            Point pt = points.get(i);
            if ( this.nw.contains(pt) )
                this.nw.points.add(pt);
            else if ( this.sw.contains(pt) )
                this.sw.points.add(pt);
            else if ( this.ne.contains(pt) )
                this.ne.points.add(pt);
            else
                this.se.points.add(pt);
        }
        // clear residual points
        points = null;
        // send polygons also to their respective quadrants
        if ( polygons != null )
        {
            for ( int i=0;i<polygons.size();i++ )
            {
                Polygon pg = polygons.get(i);
                nw.addPolygonToQuadrant(pg);
                sw.addPolygonToQuadrant(pg);
                se.addPolygonToQuadrant(pg);
                ne.addPolygonToQuadrant(pg);
            }
            this.polygons = null;
        }
    }
    /**
     * Does this quadrant actually contain the given point?
     * @param pt the point to test
     * @param closest nearest point found so far or undefined
     * @return the closest point found or undefined
     */
    Point hasPoint( Point pt,  Point closest ) 
    {
        if ( this.points != null )
        {
            for( int i=0;i<this.points.size();i++ )
            {
                if ( points.get(i).x==pt.x&&points.get(i).y==pt.y )
                {
                    closest = points.get(i);
                    break;
                }
                else if ( closest == null )
                    closest = points.get(i);
                else if ( closest.distance(pt)>points.get(i).distance(pt) )
                    closest = points.get(i);
            }
        }
        else if ( this.nw.contains(pt) )
            closest = nw.hasPoint(pt,closest);
        else if ( sw.contains(pt) )
            closest = sw.hasPoint(pt,closest);
        else if ( ne.contains(pt) )
            closest = ne.hasPoint(pt,closest);
        else if ( se.contains(pt) )
            closest = se.hasPoint(pt,closest);
        return closest;
    }
    /**
     * Print this tree's boundary and points to the console
     */
    void print() 
    {
        boundary.print();
        if ( this.points != null )
        {
            System.out.println(points.size()+" points");
            for ( int i=0;i<this.points.size();i++ )
            {
                points.get(i).print();
            }
        }
        else
        {
            nw.print();
            sw.print();
            ne.print();
            se.print();
        }
    };
    /**
     * Add a polygon to this quadrant
     * @param pg the polygon to add
     */
    void addPolygonToQuadrant( Polygon pg ) 
    {
        // only add if this quadrant has no sub-quadrants
        if ( this.points != null )
        {
            if ( pg.intersectsWithRect(this.boundary) )
            {
                if ( this.polygons == null )
                    this.polygons = new ArrayList<Polygon>();
                this.polygons.add(pg);
            }
        }
        else    // recurse
        {
            this.nw.addPolygonToQuadrant(pg);
            this.sw.addPolygonToQuadrant(pg);
            this.ne.addPolygonToQuadrant(pg);
            this.se.addPolygonToQuadrant(pg);
        }
    }
    boolean hasPoly( Polygon pg ) 
    {
        if ( this.polygons != null )
        {
            for ( int i=0;i<this.polygons.size();i++ )
                if ( this.polygons.get(i) == pg )
                    return true;
        }
        return false;        
    }
    /**
     * Add a polygon to this quadtree (or quadrant)
     * @param pg the polygon
     */
    void addPolygon( Polygon pg ) 
    {
        // add the points
        Point[] pts = pg.points;
        for ( int i=0;i<pts.length;i++ )
        {
            if ( !this.addPt(pts[i]) )
                System.out.println("failed to add point "+pts[i].x+","+pts[i].y);
        }
        // now add the polygon to the quadrant's list
        this.addPolygonToQuadrant(pg);
        // check that all the points of the polygon 
        // are in quadrants with that polygon
        //for ( var i=0;i<pts.length;i++ )
        //{
        //    var quad = this.getQuad(pts[i]);
        //    if ( quad == undefined || !quad.hasPoly(pg) )
        //        console.log("quad not found or wrong quad. i="+i);
        //}
    }
    /**
     * Remove a polygon from the quadrant (not its points)
     * @param pg the polygon to remove
     */
    void removePolygonFromQuadrant( Polygon pg ) 
    {
        if ( this.polygons != null )
        {
            for ( int i=0;i<this.polygons.size();i++ )
            {
                if ( this.polygons.get(i)== pg )
                {
                    this.polygons.remove(pg);
                    break;
                }
            }
        }
        // the polygon can be registered multiple times
        if ( this.nw != null )
            this.nw.removePolygonFromQuadrant(pg);
        if ( this.ne !=  null )
            this.ne.removePolygonFromQuadrant(pg);
        if ( this.sw != null )
            this.sw.removePolygonFromQuadrant(pg);
        if ( this.se != null )
            this.se.removePolygonFromQuadrant(pg);
    }
    /**
     * Remove a polygon and all its points
     * @param pg the polygon to remove
     */
    void removePolygon( Polygon pg ) 
    {
        for ( int i=0;i<pg.points.length;i++ )
            this.removePt( pg.points[i] );
        this.removePolygonFromQuadrant( pg );
    }
    /**
     * Is the point inside a particular polygon?
     * @param pt the point to test
     * @return the polygon it is inside or null
     */
    Polygon pointInPolygon( Point pt ) 
    {
        if ( points != null && points.size() > 0 )
        {
            // polygons must be defined also
            if ( this.polygons == null )
                System.out.println("this.polygons is undefined! this.points.length="
                +points.size());
            else
                for ( int i=0;i<polygons.size();i++ )
                {
                    if ( polygons.get(i).pointInPoly(pt) )
                    {
                        return this.polygons.get(i);
                    }
                }
            return null;
        }
        else if ( this.points == null ) // recurse
        {
            Polygon pg = this.nw.pointInPolygon(pt);
            if ( pg != null ) 
                return pg;
            pg = this.sw.pointInPolygon(pt);
            if ( pg != null ) 
                return pg;
            pg = this.ne.pointInPolygon(pt);
            if ( pg != null ) 
                return pg;
            return this.se.pointInPolygon(pt);
        }
        else
            return null;
    }
    /**
     * Update the record of this polygon (must be called at top-level)
     * @param pg the polygon to update
     */
    void updatePolygon( Polygon pg ) 
    {
        removePolygonFromQuadrant( pg );
        addPolygonToQuadrant( pg );
    }
    /**
     * Update the point which has moved
     * @param pt the point that moved
     */
    void updatePt( Point pt )
    {
        this.removePt(pt);
        this.addPt( pt );
    }
    int width() 
    {
        return boundary.width-this.boundary.x;
    }
    int height() 
    {
        return this.boundary.height-this.boundary.y;
    } 
}
