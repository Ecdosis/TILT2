/**
 * A simple point class
 * @param x the-coordinate
 * @param y the y-coordinate
 */
function Point(x,y)
{
    this.x = x;
    this.y = y;
    this.print = function() {
        console.log(this.toString());
    };
    this.toString = function() {
        return "x="+this.x+",y="+this.y;
    };
    this.equals = function(pt) {
        return pt!=undefined &&this.x==pt.x&&this.y==pt.y;
    };
    this.minus = function( other ){
        return new Point(this.x-other.x,this.y-other.y);
    };
    this.plus = function( other ) {
        return new Point(this.x+other.x,this.y+other.y);
    };
    this.perp = function(v){
        return this.x*v.y-this.y*v.x;
    };
    this.dot = function(v){
       return this.x*v.x+this.y*v.y;
    };
    this.times = function(factor) {
        return new Point(Math.round(this.x*factor),Math.round(this.y*factor));
    };
    this.crossProduct = function( b ){
        return this.x*b.y-b.x*this.y;
    };
}
/**
 * A simple rectangle class
 * @param x the x-position of top-left
 * @param y the y-position of top-left
 * @param width the width of the rect
 * @param height the height of the rect
 */
function Rect( x, y, width, height )
{
    this.width = width;
    this.height = height;
    this.x = x;
    this.y = y;
    /**
     * Is a point inside the rectangle?
     * @param p the point to test
     * @return true if it is else false
     */
    this.contains = function( p ) {
        if ( p.x >= this.x && p.x < this.x+this.width )
        {
            if ( p.y >= this.y  && p.y < this.y+this.height )
                return true;
        }
        return false;
    };
    /**
     * Dump this rectangle to the console
     */
    this.print = function(){
        console.log("x="+this.x+",y="+this.y+",width="
        +this.width+",height="+this.height);
    };
    /**
     * Outset/inset this rectangle
     * @param by the amount to inset (negative) or outset (positive)
     */
    this.expand = function( by ) {
        this.x -=by;
        this.y -= by;
        this.width += by*2;
        this.height += by*2;
    };
    /**
     * Does this rectangle intersect with another?
     * @param b the other rectangle
     * @return true if it does else false
     */
    this.intersects = function( b ) {
        var res = ((this.x <= b.x && this.x+this.width >= b.x)
            || (b.x <= this.x && b.x+b.width >= this.x))
            && ((this.y <= b.y && this.y+this.height >= b.y)
            || (b.y <= this.y && b.y+b.height >= this.y));
        /*if ( res )
            console.log("bounding boxes intersect");
        else
            console.log("Bounding boxes do NOT intersect");*/
        return res;
    };
    /**
     * Get an array of points of the 4 corners
     */
    this.getPoints = function() {
        var points= new Array();
        points.push( new Point(this.x,this.y));
        points.push( new Point(this.x+this.width,this.y) );
        points.push( new Point(this.x+this.width,this.y+this.height) );
        points.push( new Point(this.x,this.y+this.height) );
        return points;
    };
    /**
     * Is this point wholly within on the edge of the rect?
     * @param pt the point to test
     * @return true if it is true else false
     */
    this.containsPt = function( pt ) {
        return pt.x >= this.x && pt.y >= this.y 
        && pt.x <= this.x+this.width && pt.y<= this.y+this.height;
    };
}
/**
 * Segment class. A portion of a line
 * @param p0 the first end-point
 * @param p1 the second end-point
 */
function Segment( p0, p1 )
{
    this.gap = 5;
    this.p0 = p0;
    this.p1 = p1;
    this.EPSILON = 0.000001;
    /**
     * Do two line segments intersect?
     * @param b the other line segment
     * @return true if they do
     */
    this.intersects = function(b) {
        var box1 = this.getBoundingBox();
        var box2 = b.getBoundingBox();
        return box1.intersects(box2)
                && this.crosses(b)
                && b.crosses(this);
    };
    /**
     * Get the bounding box of this segment
     * @return a Rect
     */
    this.getBoundingBox = function() {
        return new Rect(Math.min(this.p0.x,this.p1.x),
            Math.min(this.p0.y,this.p1.y),
            Math.abs(this.p0.x-this.p1.x),
            Math.abs(this.p0.y-this.p1.y) );
    };
    /**
     * XOR two values (not in Javascript)
     * @param a the first boolean
     * @param b the second boolean
     * @return true if a ^ b
     */
    this.xor = function( a, b) {
        return (a&&!b)||(!a&&b);
    };
    /**
     * Does this segment cross or touch another?
     * @param b the other segment
     * @return true if it does
     */
    this.crosses = function(b) {
        var r1 = this.isPointRightOfLine(b.p0);
        var r2 = this.isPointRightOfLine(b.p1);
        var res = this.isPointOnLine(b.p0)
            || this.isPointOnLine(b.p1)
            || this.xor(r1,r2);
        /*if ( !res )
            console.log(
                this.p0.x+","+this.p0.y+":"
                +this.p1.x+","+this.p1.y
                +" does not cross "
                +b.p0.x+","+b.p0.y+":"
                +b.p1.x+","+b.p1.y );*/
       return res;
    };
    /**
     * Does this segment intersectwith a rectangle?
     * @param r the rect to test intersection with
     * @return true if it is else false
     */
    this.intersectsWithRect = function( r ) {
        var points = r.getPoints();
        for ( var i=0;i<points.length-1;i++ )
        {
            var edge = new Segment(points[i],points[i+1]);
            if ( this.crosses(edge) ) 
                return true;
        }
        return r.containsPt(this.p0)&& r.containsPt(this.p1);
    };
    /**
     * Is a point on the line within a certain tolerance?
     * http://martin-thoma.com/how-to-check-if-two-line-segments-intersect/
     * @param p the point to test
     * @return true if it is
     */
    this.isPointOnLine = function(p) {
        var aTmp = new Segment(new Point(0, 0), new Point(
            this.p1.x - this.p0.x, this.p1.y - this.p0.y));
        var pTmp = new Point(p.x - this.p0.x, p.y - this.p0.y);
        var r = aTmp.p1.crossProduct(pTmp);
        return Math.abs(r) < this.EPSILON;
    };
    /**
     * Is a point to the right of the line but not touching it?
     * http://martin-thoma.com/how-to-check-if-two-line-segments-intersect/
     * @param p the point to test
     * @return true if it is to the right else false
     */
    this.isPointRightOfLine = function(p) {
        var aTmp = new Segment(new Point(0,0), new Point(
            this.p1.x-this.p0.x, this.p1.y-this.p0.y));
        var pTmp = new Point(p.x-this.p0.x, p.y-this.p0.y);
        var res = aTmp.p1.crossProduct(pTmp) < 0;
        /*if ( !res)
            console.log(
                p.x+","+p.y+" is not to the right of "
                +this.p0.x+","+this.p0.y+":"
                +this.p1.x+","+this.p1.y
        );*/
        return res;
    }
    /**
     * Compute the hypoteneuse length
     * @return the diagonal length of the segment
     */
    this.hypoteneuse = function() {
        var dy = this.p1.y-this.p0.y;
        var dx = this.p1.x-this.p0.x;
        return Math.abs(Math.sqrt(dy*dy+dx*dx));
    };
    /** 
     * Amount to inc/dec the x-value along a split on the 'left'
     */
    this.x_inc_left = function() {      
        return Math.round((this.gap*(this.p0.x-this.p1.x))/this.hypoteneuse());
    };
    this.y_inc_left = function() {
        return Math.round((this.gap*(this.p0.y-this.p1.y))/this.hypoteneuse());
    };
    this.x_inc_right = function() {
        return Math.round((this.gap*(this.p1.x-this.p0.x))/this.hypoteneuse());
    };
    this.y_inc_right = function() {
        return Math.round((this.gap*(this.p1.y-this.p0.y))/this.hypoteneuse());
    };
    /**
     * Compute the distance a point is from a line segment
     * http://forums.codeguru.com/printthread.php?t=194400
     * @param c the point off the line
     * @return the distance in fractional pixels
     */
    this.distFromLine = function( c ) {
        var a = this.p0;
        var b = this.p1;
        var r_numerator = (c.x-a.x)*(b.x-a.x) + (c.y-a.y)*(b.y-a.y);
        var r_denomenator = (b.x-a.x)*(b.x-a.x) + (b.y-a.y)*(b.y-a.y);
        var r = r_numerator / r_denomenator;
        var px = a.x + r*(b.x-a.x);
        var py = a.y + r*(b.y-a.y);  
        var s =  ((a.y-c.y)*(b.x-a.x)-(a.x-c.x)*(b.y-a.y) ) / r_denomenator;
        var distanceLine = Math.abs(s)*Math.sqrt(r_denomenator);
        var xx = px;
        var yy = py;
        if ( (r >= 0) && (r <= 1) )
            distanceSegment = distanceLine;
        else
        {
            var dist1 = (c.x-a.x)*(c.x-a.x) + (c.y-a.y)*(c.y-a.y);
            var dist2 = (c.x-b.x)*(c.x-b.x) + (c.y-b.y)*(c.y-b.y);
            if (dist1 < dist2)
            {
                xx = a.x;
                yy = a.y;
                distanceSegment = Math.sqrt(dist1);
            }
            else
            {
                xx = b.x;
                yy = b.y;
                distanceSegment = Math.sqrt(dist2);
            }
        }
        return distanceSegment;
    };
}
/**
 * Polygon class
 * @param pts a plain javascript array of x,y objects
 * @param id the id of the canvas we are drawn inside
 */
function Polygon( pts, id )
{
    // flag to save redrawing
    this.drawn = false;
    // the id of the canvas
    this.canvas = id;
    // radius of control points
    this.radius = 4;
    // colour of control points
    this.controlColour = '#003300';
    // fill colour of polygons
    this.fillColour = "#FF0000";
    // colour of highlighted point
    this.highlightColour = "#00FF00";
    // true if anchored by a click
    this.anchored = false;
    // currently dragged point
    this.dragPt = undefined;
    // currently highlighted point
    this.highlightPt = undefined;
    // distance to new points from edge
    this.tolerance = 0.07;
    // first end-point of edge nearest to new point
    this.pt1 = undefined;
    // second end-point of edge nearest to new point
    this.pt2 = undefined;
    // index into points of edge end-point
    this.pos = 0;
    // array of points
    this.points = new Array();
    // small number
    this.SMALL_NUM = 0.00000001;
    for ( var i=0;i<pts.length;i++ )
    {
        var pt = new Point(pts[i].x,pts[i].y);
        this.points[this.points.length] = pt;
    }
    /**
     * Get the points of the polygon
     * @return an array of Point
     */
    this.getPoints = function() {
        return this.points;
    };
    /**
     * Is the given point inside this polygon?
     * @param pt the point to test for
     * @return true if it was else false 
     */
    this.pointInPoly = function(pt) {
        var cn = 0;
        var V = this.points;
        for ( var i=0; i<V.length-1; i++ ) 
        {
            if (((V[i].y <= pt.y) && (V[i+1].y > pt.y))
            || ((V[i].y > pt.y) && (V[i+1].y <=  pt.y))) 
            {
                var vt = (pt.y  - V[i].y) / (V[i+1].y - V[i].y);
                if (pt.x <  V[i].x + vt * (V[i+1].x - V[i].x))
                     ++cn;
            }
        }
        return cn&1;
    };
    /**
     * Draw this polygon in the canvas' context
     * @param ctx the current drawing context
     */
    this.drawPolygon = function(ctx) { 
        if ( !this.drawn )
        {
            ctx.globalAlpha = 0.35; 
            ctx.fillStyle = this.fillColour;
            ctx.beginPath();
            if ( this.points.length>0 )
                ctx.moveTo(this.points[0].x,this.points[0].y);
            for ( var i=1;i<this.points.length;i++ )
                ctx.lineTo(this.points[i].x,this.points[i].y);
            ctx.closePath();
            ctx.fill();
            this.drawn = true;
        }
    };
    /**
     * Draw the little circles at each corner
     * @param ctx the current drawing context
     */
    this.drawControlPoints = function(ctx) {
        ctx.globalAlpha = 0.55; 
        for ( var i=0;i<this.points.length;i++ )
        {
            var pt = this.points[i];
            ctx.beginPath();
            if ( pt === this.highlightPt || pt === this.dragPt )
            {
                ctx.strokeStyle = this.highlightColour;
                ctx.lineWidth = 2;
                ctx.strokeRect( pt.x-this.radius, pt.y-this.radius, 
                    this.radius*2, this.radius*2 );
            }
            else
            {
                ctx.arc(pt.x, pt.y, this.radius, 0, 2 * Math.PI, false);
                ctx.lineWidth = 1;
                ctx.strokeStyle = this.controlColour;
                ctx.stroke();
            }
        }
    };
    /**
     * Recomute the bounds
     */
    this.computeBounds = function() {
        var minX = Number.MAX_VALUE;
        var minY = Number.MAX_VALUE;
        var maxX = Number.MIN_VALUE;
        var maxY = Number.MIN_VALUE;
        for ( var i=0;i<this.points.length;i++ )
        {
            var pt = this.points[i];
            if ( pt.x < minX )
                minX = pt.x;
            if ( pt.y < minY )
                minY = pt.y;
            if ( pt.x > maxX )
                maxX = pt.x;
            if ( pt.x >maxY )
                maxY = pt.x;
        }
        this.bounds = new Rect( minX, minY, maxX-minX, maxY-minY );
    };
    this.computeBounds();
    /**
     * Clear the polygon 
     * @param ctx the current drawing context
     */
    this.erase = function(ctx) {
        this.computeBounds();
        var extra = this.radius+1;
        ctx.globalAlpha = 1.0; 
        ctx.clearRect(this.bounds.x-extra,
            this.bounds.y-extra,
            this.bounds.width+extra*2,
            this.bounds.height+extra*2);
        this.drawn = false;
    };
    /**
     * We just started dragging
     * @param pt the point that is being dragged
     */
    this.startDrag = function(pt) {
        for ( var i=0;i<this.points.length;i++)
        {
            if ( pt.x==this.points[i].x && pt.y==this.points[i].y )
            {
                this.dragPt = this.points[i];
                break;
            }
        }
    };
    /**
     * Are we currently dragging a point?
     * @return true if a point is being dragged
     */
    this.dragging = function() {
        return this.dragPt != undefined;
    };
    /**
     * Redraw the polygon and its control points
     * @param ctx the current drawing context
     */
    this.redraw = function(ctx) {
        this.erase(ctx);
        this.drawPolygon(ctx);
        this.drawControlPoints(ctx);
    };
    /**
     * Reset the polygon to the normal anchored state
     * @param qt the top-level quadtree
     */
    this.endDrag = function(qt) {
        qt.updatePt( this.dragPt);
        qt.updatePolygon(this);
        this.dragPt = undefined;
        this.redraw($("#"+this.canvas)[0].getContext("2d"));
    };
    /**
     * Remove the highlighted point
     */
    this.setAnchorNormal = function() {
        this.highlightPt = undefined;
        var ctx= $("#"+this.canvas)[0].getContext("2d");
        this.redraw(ctx);
    };
    /**
     * Drag the currently dragged point
     * @param pt the new position of dragPt
     */
    this.dragTo = function(pt) {
        if ( this.dragPt.x != pt.x || this.dragPt.y != pt.y )
        {
            var ctx= $("#"+this.canvas)[0].getContext("2d");
            this.erase(ctx);
            this.dragPt.x = pt.x;
            this.dragPt.y = pt.y;
            this.drawPolygon(ctx);
            this.drawControlPoints(ctx);
        }
    };
    /**
     * Set the highlight point
     * @param pt the point to highlight for subsquent operations
     */
    this.setHighlightPt = function(pt) {
        for ( var i=0;i<this.points.length;i++ )
        {
            if ( this.points[i].equals(pt) )
            {
                this.highlightPt = this.points[i];
                this.dragPt = undefined;
                this.redraw($("#"+this.canvas)[0].getContext("2d"));
                break;
            }
        }
    };
    /**
     * Delete the current highlight point
     */
    this.deleteHighlightPt = function() {
        for ( var i=0;i<this.points.length;i++ )
        {
            if ( this.points[i].equals(this.highlightPt) )
            {
                var ctx= $("#"+this.canvas)[0].getContext("2d");
                this.erase(ctx);
                this.points.splice(i,1);
                if ( this.points.length>0 )
                    this.highlightPt = this.points[i%this.points.length];
                this.drawPolygon(ctx);
                this.drawControlPoints(ctx);
                break;
            }
        }
    };
    /**
     * Is the point within some tolerance of an edge?
     * http://forums.codeguru.com/printthread.php?t=194400
     * @param pt the point near the edge
     * @return true if it is else false
     */
	this.ptOnEdge = function(pt) {
        this.pt1 = this.pt2 = undefined;
    	for ( var i=0;i<this.points.length-1;i++ )
        {
            var seg = new Segment(this.points[i],this.points[i+1]);
            var dist = seg.distFromLine(pt);
            if ( dist < this.tolerance )
            {
                this.pt1 = this.points[i];
                this.pt2 = this.points[i+1];
                return true;            
            }
        }
        return false;
    };
    /**
     * Find the closest point on the line to the proposed point
     * @param a one existing end-point
     * @param b the other existing end-point
     * @param c the point off the line
     * @return a new point on the line being closest to c
     */
    this.closestPt = function( a, b, c ) {
		var r_num = (c.x-a.x)*(b.x-a.x) + (c.y-a.y)*(b.y-a.y);
		var r_den = (b.x-a.x)*(b.x-a.x) + (b.y-a.y)*(b.y-a.y);
		var r = r_num / r_den;
		return new Point(a.x + r*(b.x-a.x), a.y + r*(b.y-a.y) );
    };
	/**
     * Add a point to the polygon after test for ptOnEdgeor otherwise
     * @param pt the point to add
     */
	this.addPt = function(pt) {
        // have we just called ptOnEdge?
        if ( this.pt1 != undefined && this.pt2 != undefined )
        {
            var newPt = this.closestPt(this.pt1,this.pt2,pt);
			this.points.splice(this.pos,0,newPt);
            var ctx = $("#"+this.canvas)[0].getContext("2d");
            this.redraw(ctx);
            return newPt;
        }
        else    // otherwise
            this.points.push( pt );
        return pt;
	};
    /**
     * Does a line intersect with this polygon and if so where
     * @param S the segment intersecting with us
     * @paramIS VAR param: set this to intersecting points
     * @return true if segment S intersects with us
     */
    this.intersectsLine = function( S, IS ) {
        if ( S.p0.equals(S.p1) )
        {
            IS.p0 = S.p0;
            IS.p1 = S.p1;
            return this.pointInPoly(S.p0);
        }
        else
        {
            var tE = 0;
            var tL = 1;
            var t, N, D;
            var dS = S.p1.minus(S.p0);
            var e;
            for (var i=0;i<this.points.length-1;i++) 
            {
                e = this.points[i+1].minus(this.points[i]);
                N = e.perp(S.p0.minus(this.points[i])); 
                D = -e.perp(dS);
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
            // tE <= tL implies that there is a valid intersection subsegment
            IS.p0 = S.p0.plus(dS.times(tE));   // = P(tE) = point where S enters polygon
            IS.p1 = S.p0.plus(dS.times(tL));   // = P(tL) = point where S leaves polygon
            return true;
        }
    };
    /**
     * Split this ploygon into two around a line
     * @param p0 the first point of the cutting segment
     * @param p1 the second point of the cutting segment
     * @return an array of 1 (same polygon) or two new polygons
     */
    this.split = function(p0,p1) {
        var polygons = new Array();
        var S = new Segment(p0,p1);
        var SI = new Segment(new Point(0,0),new Point(0,0));
        var intersects = polygon.intersectsLine(S,SI);
        if ( intersects )
        {
            var state = 0;
            var left = new Polygon(new Array(),this.canvas+"-left");
            var right = new Polygon(new Array(),this.canvas+"-right");
            //now compose the two separate polygons
            for ( var i=0;i<this.points.length-1;i++ )
            {
                var current = this.points[i];
                var next = this.points[i+1];
                var seg = new Segment(current,next);
                switch ( state )
                {
                    case 0: //looking for first break
                        left.addPt( current );
                        if ( seg.intersects(S) )
                        {
                            var xdl = seg.x_inc_left();
                            var ydl = seg.y_inc_left();
                            var xdr = seg.x_inc_right();
                            var ydr = seg.y_inc_right();
                            left.addPt( new Point(SI.p0.x+xdl,SI.p0.y+ydl) );
                            right.addPt( new Point(SI.p0.x+xdr,SI.p0.y+ydr) );
                            state = 1;
                        } 
                        break;
                    case 1: //looking for second break
                        right.addPt( current );
                        if ( seg.intersects(S) )
                        {
                            var xdl = seg.x_inc_left();
                            var ydl = seg.y_inc_left();
                            var xdr = seg.x_inc_right();
                            var ydr = seg.y_inc_right();
                            right.addPt( new Point(SI.p1.x+xdr,SI.p1.y+ydr) );
                            left.addPt( new Point(SI.p1.x+xdl,SI.p1.y+ydl) );
                            state = 2;
                        }
                        break;
                    case 2: // looking to terminate
                        left.addPt( current );
                        break;
                }
            }
            if ( this.points.length>0 )
                left.addPt( this.points[this.points.length-1]);
            polygons.push( left );
            polygons.push( right );
            return polygons;
        }
        else
        {
            polygons.push( this );
            return polygons;
        }
    };
    /**
     * Does this polygon intersect with a rectangle?
     * @param r the rectangle to test
     * @return true if it does else false
     */
    this.intersectsWithRect = function( r ) {
        // 1. at least one edge of polygon intersects rect
        for ( var i=0;i<this.points.length-1;i++ )
        {
            var edge = new Segment(this.points[i],this.points[i+1]);
            if ( edge.intersectsWithRect(r) ) 
                return true;
        }
        // 2. polygon completely within rect
        if ( this.points.length>0 && this.points[0].inRect(r) )
            return true;
        // 3. rect completely within the polygon
        var p = new Point(r.x,r.y);
        if ( this.pointInPoly(p) )
            return true;
        return false;
    };
}
