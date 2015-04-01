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
        return new Point(this.x*factor,this.y*factor);
    };
    this.crossProduct = function( b ){
        return this.x*b.y-b.x*this.y;
    };
    this.inRect = function( r ) {
        return this.x >= r.x && this.x <=r.x+r.width 
        && this.y >= r.y && this.y <= r.y+r.height;
    };
    this.distance = function(pt2) {
        var xDiff = Math.abs(this.x-pt2.x);
        var yDiff= Math.abs(this.y-pt2.y);
        var ysq = yDiff*yDiff;
        var xsq = xDiff*xDiff;
        // good old pythagoras
        return Math.round(Math.sqrt(ysq+xsq));
    };
    /**
     * Convert local to absolute coordinates
     * @param ctx the current graphical context
     * @return the point transformed
     */
    this.toAbsolute = function( ctx ) {
        return new Point(this.x /= ctx.canvas.width,this.y /= ctx.canvas.height);
    };
    /**
     * Convert absolute to local coordinates
     * @param ctx the current graphical context
     * @return the point transformed
     */
    this.toLocal = function(ctx) {
        return new Point(this.x*ctx.canvas.width,this.y*ctx.canvas.height);
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
     * Convert this rectangle to a string for debugging
     */
    this.toString = function(){
        return"x="+this.x+",y="+this.y+",width="
        +this.width+",height="+this.height;
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
    /**
     * Scale a rectangle from absolute to local coordinates
     * @param ctx the drawing context
     */
    this.toLocal = function(ctx) {
        var r = new Rect(this.x*ctx.canvas.width,
        this.y*ctx.canvas.height,this.width*ctx.canvas.width,
        this.height*ctx.canvas.height);
        return r;
    };
    /**
     * Is this rectangle entire inside another?
     * @param r the other rectangle
     * @return true if it is
     */
    this.inside = function( r ) {
        return this.x >= r.x && this.x+this.width<=r.x+r.width 
            && this.y>=r.y&&this.y+this.height<=r.y+r.height;
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
    this.EPSILON = 0.00000001;
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
    this.x_inc_left = function(ctx) { 
        var gap = this.gap/ctx.canvas.width;     
        return Math.round((gap*(this.p0.x-this.p1.x))/this.hypoteneuse());
    };
    this.y_inc_left = function(ctx) {
        var gap = this.gap/ctx.canvas.width;     
        return Math.round((gap*(this.p0.y-this.p1.y))/this.hypoteneuse());
    };
    this.x_inc_right = function(ctx) {
        var gap = this.gap/ctx.canvas.width;     
        return Math.round((gap*(this.p1.x-this.p0.x))/this.hypoteneuse());
    };
    this.y_inc_right = function(ctx) {
        var gap = this.gap/ctx.canvas.width;     
        return Math.round((gap*(this.p1.y-this.p0.y))/this.hypoteneuse());
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
    // local radius of control points
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
    // distance in local pixels to new points from edge
    this.tolerance = 1.5;
    // index into points of edge end-point
    this.pos = undefined;
    // array of points
    this.points = pts;
    // small number
    this.SMALL_NUM = 0.0000000001;
    /**
     * Get the points of the polygon
     * @return an array of Point
     */
    this.getPoints = function() {
        return this.points;
    };
    /**
     * Is the given point inside this polygon?
     * http://stackoverflow.com/questions/11716268/point-in-polygon-algorithm
     * @param pt the point to test for
     * @return true if it was else false 
     */
    this.pointInPoly = function(pt) {
        var i,j,nvert = this.points.length;
        var c = false;
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
            var pt0 = this.points[0].toLocal(ctx);
            console.log("x="+pt0.x+" y="+pt0.y);
            if ( this.points.length>0 )
                ctx.moveTo(pt0.x,pt0.y);
            for ( var i=1;i<this.points.length;i++ )
            {
                var pti = this.points[i].toLocal(ctx);
                ctx.lineTo(pti.x,pti.y);
            }
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
            var orig = this.points[i];
            var pt = orig.toLocal(ctx);
            ctx.beginPath();
            if ( orig === this.highlightPt || orig === this.dragPt )
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
     * Recompute the bounds
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
            if ( pt.y > maxY )
                maxY = pt.y;
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
        var r = this.bounds.toLocal(ctx);
        var extra = this.radius+1;
        ctx.globalAlpha = 1.0; 
        ctx.clearRect(r.x-extra, r.y-extra,
            r.width+extra*2, r.height+extra*2);
        this.drawn = false;
    };
    /**
     * We just started dragging
     * @param pt the point that is being dragged in absolute coordinates
     */
    this.startDrag = function(pt) {
        var ctx = $("#"+this.canvas)[0].getContext("2d");
        var localPt = pt.toLocal(ctx);
        localPt.x = Math.round(localPt.x);
        localPt.y = Math.round(localPt.y);
        for ( var i=0;i<this.points.length;i++)
        {
            var ptix = Math.round(this.points[i].x*ctx.canvas.width);
            var ptiy = Math.round(this.points[i].y*ctx.canvas.height);
            if ( localPt.x==ptix && localPt.y==ptiy )
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
     * @param pt the new position of dragPt in absolute coordinates
     */
    this.dragTo = function(pt) {
        if ( this.dragPt.x != pt.x || this.dragPt.y != pt.y )
        {
            var ctx = $("#"+this.canvas)[0].getContext("2d");
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
        this.pos = undefined;
        var ctx= $("#"+this.canvas)[0].getContext("2d");
        var limit = this.tolerance/ctx.canvas.width;
        for ( var i=0;i<this.points.length-1;i++ )
        {
            var seg = new Segment(this.points[i],this.points[i+1]);
            var dist = seg.distFromLine(pt);
            if ( dist < limit )
            {
                this.pos = i;
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
     * Add a point to the polygon after test for ptOnEdge or otherwise
     * @param pt the point to add
     */
    this.addPt = function(pt) {
        // have we just called ptOnEdge?
        if ( this.pos != undefined )
        {
            var newPt = this.closestPt(this.points[this.pos],this.points[this.pos+1],pt);
            this.points.splice(this.pos+1,0,newPt);
            var ctx = $("#"+this.canvas)[0].getContext("2d");
            this.redraw(ctx);
            this.pos = undefined;
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
        var intersects = this.intersectsLine(S,SI);
        if ( intersects )
        {
            var state = 0;
            var left = new Polygon(new Array(),this.canvas);
            var right = new Polygon(new Array(),this.canvas);
            var ctx = $("#"+this.canvas)[0].getContext("2d");
            // now compose the two separate polygons
            for ( var i=0;i<this.points.length-1;i++ )
            {
                var current = this.points[i];
                var next = this.points[i+1];
                var seg = new Segment(current,next);
                switch ( state )
                {
                    case 0: // looking for first break
                        left.addPt( current );
                        if ( seg.intersects(S) )
                        {
                            var xdl = seg.x_inc_left(ctx);
                            var ydl = seg.y_inc_left(ctx);
                            var xdr = seg.x_inc_right(ctx);
                            var ydr = seg.y_inc_right(ctx);
                            left.addPt( new Point(SI.p0.x+xdl,SI.p0.y+ydl) );
                            right.addPt( new Point(SI.p0.x+xdr,SI.p0.y+ydr) );
                            state = 1;
                        } 
                        break;
                    case 1: // looking for second break
                        right.addPt( current );
                        if ( seg.intersects(S) )
                        {
                            var xdl = seg.x_inc_left(ctx);
                            var ydl = seg.y_inc_left(ctx);
                            var xdr = seg.x_inc_right(ctx);
                            var ydr = seg.y_inc_right(ctx);
                            right.addPt( new Point(SI.p1.x+xdl,SI.p1.y+ydl) );
                            left.addPt( new Point(SI.p1.x+xdr,SI.p1.y+ydr) );
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
        if ( this.bounds.intersects(r) )
        {
            // 1. r is completely inside us
            if ( r.inside(this.bounds) )
                return true;
            // 2. we are entirely inside r
            else if ( this.bounds.inside(r) )
                return true;
            else
            {
                // 3. at least one polygon point is inside r
                for ( var i=0;i<this.points.length;i++ )
                {
                    if ( r.containsPt(this.points[i]) )
                        return true;
                }
                // 4. at least one edge intersects with r
                for ( var i=0;i<this.points.length-1;i++ )
                {
                    var edge = new Segment(this.points[i],this.points[i+1]);
                    if ( edge.intersectsWithRect(r) ) 
                        return true;
                }
            }
        }
        // else: most cases will fall through to here
        return false;
    };
    this.print = function() {
        var str = "polygon: ";
        for ( var i=0;i<this.points.length;i++ )
        {
            var pt = this.points[i];
            str +=  pt.x+","+pt.y;
            if ( i < this.points.length-1 )
                str += ":";
        }
        console.log(str);
    };
}
