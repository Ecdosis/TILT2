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
    this.distance= function(pt1,pt2) {
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
    this.addPoint = function( pt )
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
            if ( this.nw.addPoint(pt) )
                return true;
            else if ( this.sw.addPoint(pt) )
                return true;
            else if ( this.ne.addPoint(pt) )
                return true;
            else if ( this.se.addPoint(pt) )
                return true;
            else
            {
                console.log("add failed for point "+pt.x+","+pt.y);
                return false;
            }
        }
    };
    /**
     * Remove a point from the quadtree
     * @param pt the point to remove
     */
    this.removePoint = function(pt) {
        if ( this.points != undefined )
        {
            for( var i=0;i<this.points.length;i++ )
            {
                if ( this.points[i].equals(pt) )
                {
                    this.points.splice(i,1);
                    break;
                }
            }
        }
        else if ( this.nw.contains(pt) )
            this.nw.removePoint(pt);
        else if ( this.sw.contains(pt) )
            this.sw.removePoint(pt);
        else if ( this.ne.contains(pt) )
            this.ne.removePoint(pt);
        else if ( this.se.contains(pt) )
            this.se.removePoint(pt);
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
                res = this.nw.addPoint(pt);
            else if ( this.sw.contains(pt) )
                res = this.sw.addPoint(pt);
            else if ( this.ne.contains(pt) )
                res= this.ne.addPoint(pt);
            else
                res = this.se.addPoint(pt);
            if ( !res )
            {
                console.log("failed to add point "+pt.x+","+pt.y);
                break;
            }
        }
        // clear residual points
        this.points = undefined;
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
}

