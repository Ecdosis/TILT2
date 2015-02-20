/**
 * A 2-D binary tree
 * @param x the top-left x-position
 * @param y the top-left y position
 * @param width the width of the area covered
 * @param height the height of the area covered
 */
function QuadTree( x, y, width, height )
{
    this.boundary = new Rect(x,y,width,height);
    this.points = new Array();
    this.distance = function(pt1,pt2) {
        var xDiff = Math.abs(pt1.x-pt2.x);
        var yDiff= Math.abs(pt1.y-pt2.y);
        var ysq = yDiff*yDiff;
        var xsq = xDiff*xDiff;
        // good old pythagoras
        return Math.round(Math.sqrt(ysq+xsq));
    };
    /**
     * Add a point to the quadtree
     * @param pt the point to add
     */
    this.addPt = function( pt )
    {
        if ( !this.contains(pt) )
        {
            return false;
        }
        else 
        {
            if ( this.points != undefined )
            {
                if ( this.points.length < 4 )
                {
                    this.points[this.points.length] = pt;
                    return true;
                }
                else
                    this.subdivide();
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
                console.log("add failed for point "+pt.x+","+pt.y);
                return false;
            }
        }
    };
    /**
     * Find the quadrant to which this point belongs
     * @param pt the point to find
     * @return the quadrant in which it is stored or undefined
     */
    this.getQuad = function(pt) {
        if ( this.points != undefined )
        {
            for( var i=0;i<this.points.length;i++ )
            {
                if ( this.points[i] == pt )
                {
                    return this;
                }
            }
            console.log("failed to find quad for point "+pt.x+","+pt.y);
            return undefined;
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
            console.log("failed to find quad for point "+pt.x+","+pt.y);
            return undefined;
        }
    };
    /**
     * Remove a point from the quadtree
     * @param pt the point to remove
     */
    this.removePt = function(pt) {
        if ( this.points != undefined )
        {
            for( var i=0;i<this.points.length;i++ )
            {
                if ( this.points[i] == pt )
                {
                    this.points.splice(i,1);
                    return;
                }
            }
            console.log("failed to remove point "+pt.x+","+pt.y);
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
            console.log("failed to remove point "+pt.x+","+pt.y);
    };
    /**
     * Does this quadrant contain the given point?
     * @param pt the point to test for
     * @return true if it is contained in our boundary
     */
    this.contains = function(pt) {
        return this.boundary.contains(pt);
    };
    /**
     * We are full. Split into four equal quadrants, and reassign points
     */
    this.subdivide = function() {
        // precompute x,y of se quadrant
        var xSplit = Math.floor(this.boundary.width/2)+this.boundary.x;
        var ySplit = Math.floor(this.boundary.height/2)+this.boundary.y;
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
        for ( var i=0;i<this.points.length;i++ )
        {
            var pt = this.points[i];
            var res;
            if ( this.nw.contains(pt) )
                res = this.nw.addPt(pt);
            else if ( this.sw.contains(pt) )
                res = this.sw.addPt(pt);
            else if ( this.ne.contains(pt) )
                res= this.ne.addPt(pt);
            else
                res = this.se.addPt(pt);
            if ( !res )
            {
                console.log("failed to add point "+pt.x+","+pt.y);
                break;
            }
        }
        // clear residual points
        this.points = undefined;
        // send polygons also to their respective quadrants
        if ( this.polygons != undefined )
        {
            for ( var i=0;i<polygons.length;i++ )
            {
                var pg = this.polygons[i];
                this.nw.addPolygonToQuadrant(pg);
                this.sw.addPolygonToQuadrant(pg);
                this.se.addPolygonToQuadrant(pg);
                this.ne.addPolygonToQuadrant(pg);
            }
            polygons = undefined;
        }
    };
    /**
     * Does this quadrant actually contain the given point?
     * @param pt the point to test
     * @param closest nearest point found so far or undefined
     * @return the closest point found or undefined
     */
    this.hasPoint = function(pt,closest) {
        if ( this.points != undefined )
        {
            for( var i=0;i<this.points.length;i++ )
            {
                if ( this.points[i].x==pt.x&&this.points[i].y==pt.y )
                {
                    closest = this.points[i];
                    break;
                }
                else if ( closest == undefined )
                    closest = this.points[i];
                else if ( this.distance(closest,pt)>this.distance(this.points[i],pt) )
                    closest = this.points[i];
            }
        }
        else if ( this.nw.contains(pt) )
            closest = this.nw.hasPoint(pt,closest);
        else if ( this.sw.contains(pt) )
            closest = this.sw.hasPoint(pt,closest);
        else if ( this.ne.contains(pt) )
            closest = this.ne.hasPoint(pt,closest);
        else if( this.se.contains(pt) )
            closest = this.se.hasPoint(pt,closest);
        return closest;
    };
    /**
     * Print this tree's boundary and points to the console
     */
    this.print = function() {
        this.boundary.print();
        if ( this.points != undefined )
        {
            console.log(this.points.length+" points");
            for ( var i=0;i<this.points.length;i++ )
            {
                this.points[i].print();
            }
        }
        else
        {
            this.nw.print();
            this.sw.print();
            this.ne.print();
            this.se.print();
        }
    };
    /**
     * Add a polygon to this quadrant
     * @param pg the polygon to add
     */
    this.addPolygonToQuadrant = function( pg ) {
        // only add if this quadrant has no sub-quadrants
        if ( this.points != undefined )
        {
            if ( pg.intersectsWithRect(this.boundary) )
            {
                if ( this.polygons == undefined )
                    this.polygons = new Array();
                this.polygons.push(pg);
            }
        }
        else    // recurse
        {
            this.nw.addPolygonToQuadrant(pg);
            this.sw.addPolygonToQuadrant(pg);
            this.ne.addPolygonToQuadrant(pg);
            this.se.addPolygonToQuadrant(pg);
        }
    };
    /**
     * Add a polygon to this quadtree (or quadrant)
     * @param pg the polygon
     */
    this.addPolygon = function(pg) {
        // add the points
        var pts = pg.points;
        for ( var i=0;i<pts.length;i++ )
        {
            if ( !qt.addPt(pts[i]) )
                console.log("failed to add point "+pt.x+","+pt.y);
        }
        // now add the polygon to the quadrant's list
        this.addPolygonToQuadrant(pg);
    };
    /**
     * Remove a polygon from the quadrant (not the points)
     * @param pg the polygon to remove
     */
    this.removePolygonFromQuadrant = function( pg) {
        if ( this.polygons != undefined )
        {
            for ( var i=0;i<this.polygons.length;i++ )
            {
                if ( this.polygons[i]== pg )
                {
                    this.polygons.splice(i,1);
                    break;
                }
            }
        }
        // the polygon can be registered multiple times
        if ( this.nw != undefined )
            this.nw.removePolygonFromQuadrant(pg);
        if ( this.ne != undefined )
            this.ne.removePolygonFromQuadrant(pg);
        if ( this.sw != undefined )
            this.sw.removePolygonFromQuadrant(pg);
        if ( this.se != undefined )
            this.se.removePolygonFromQuadrant(pg);
    };
    /**
     * Remove a polygon and all its points
     * @param pg the polygon to remove
     */
    this.removePolygon = function( pg ) {
        for ( var i=0;i<pg.points.length;i++ )
            this.removePt( pg.points[i] );
        this.removePolygonFromQuadrant( pg );
    };
    /**
     * Is the point inside a particular polygon?
     * @param pt the point to test
     * @return the polygon it is inside or false
     */
    this.pointInPolygon = function( pt ) {
        if ( this.points != undefined )
        {
            // then there must also be polygons
            for ( var i=0;i<this.polygons.length;i++ )
            {
                if ( this.polygons[i].pointInPoly(pt) )
                    return this.polygons[i];
            }
            return false;
        }
        else  // recurse
        {
            var pg = this.nw.pointInPolygon(pt);
            if ( pg ) 
                return pg;
            pg = this.sw.pointInPolygon(pt);
            if ( pg ) 
                return pg;
            pg = this.ne.pointInPolygon(pt);
            if ( pg ) 
                return pg;
            return this.se.pointInPolygon(pt);
        }
    };
    /**
     * Update the record of this polygon (must be called at top-level)
     * @param pg the polygon to update
     */
    this.updatePolygon = function(pg) {
        this.removePolygonFromQuadrant( pg );
        this.addPolygonToQuadrant( pg );
    };
    /**
     * Update the point which has moved
     * @param pt the point that moved
     */
     this.updatePt = function( pt ){
        this.removePt(pt);
        this.addPt( pt );
    };      
}
