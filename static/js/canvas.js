/**
 * Handle mouse-events and redrawing canvas-wide
 * @param id the id ofthe HTML canvas element to bind to
 */
function Canvas( id )
{
    // the id of the html5 canvas object
    this.id = id;
    // the polygon with focus
    this.polygon = undefined;
    // where the user moused down last
    this.downPt = undefined;
    // the quadtree for finding points and polygons on the page
    this.qt = new QuadTree(0,0,1.0,1.0);
    //where the user last performed mouse-up
    this.upPt = undefined;
    // reference to the Canvas object
    var self = this;
    // remove any existing event handlers
    $("#"+this.id).off();
    /**
     * Handle all mouse-moved events inside the canvas 
     */
    $("#"+self.id).mousemove(function( event ) {
        if ( self.polygon != undefined )
        {
            var canvas = $("#"+self.id);
            var offset = canvas.offset();
            var ctx = canvas[0].getContext("2d");        
            var orig = new Point(event.pageX-offset.left,event.pageY-offset.top);
            var pt = orig.toAbsolute( ctx );
            if ( !self.polygon.anchored )
            {
                if ( self.polygon.pointInPoly(pt) )
                {
                    self.polygon.drawPolygon(ctx);
                }
                else
                {
                    self.polygon.erase(ctx);
                    var newPoly = self.qt.pointInPolygon(pt);
                    if ( newPoly )
                        self.polygon = newPoly;
                }
            }
            else if ( self.polygon.dragging() )
            {
                var quad = self.qt.getQuad(self.polygon.dragPt);
                self.polygon.dragTo(pt);
                quad.removePt(self.polygon.dragPt);
                self.qt.addPt(self.polygon.dragPt);
            }
            else if ( self.downPt != undefined && self.polygon.anchored 
                && !self.polygon.pointInPoly(self.downPt) )
            {
                if ( self.upPt != undefined )
                {
                    var r = self.clipRect(ctx);
                    ctx.save();
                    ctx.rect(r.x,r.y,r.width,r.height);
                    ctx.clip();
                    self.polygon.redraw(ctx);
                    ctx.restore();
                }
                ctx.save();
                ctx.beginPath();
                var down = self.downPt.toLocal(ctx);
                ctx.moveTo(down.x,down.y);
                ctx.strokeStyle = '#000000';
                ctx.lineWidth = 2;
                ctx.setLineDash([2,3]);
                ctx.lineTo(orig.x,orig.y);
                ctx.stroke();
                ctx.restore();
                self.upPt = pt;
            }
        }
    });
    /**
     * Handle all mouse-down events inside the canvas 
     */
    $("#"+this.id).mousedown(function(event) {
        if ( self.polygon != undefined )
        {
            var canvas = $("#"+self.id);
            var offset = canvas.offset();
            var ctx = canvas[0].getContext("2d");        
            var orig = new Point(event.pageX-offset.left,event.pageY-offset.top);
            var pt = orig.toAbsolute( ctx );
            if ( self.polygon.anchored )
            {
                var inside = self.polygon.pointInPoly(pt);
                var closest = self.qt.hasPoint(pt,undefined);
                var dist = (closest!=undefined)?closest.distance(pt):1.0;
                // imprecise, since vertical scale will be different
                dist *= canvas[0].width;
                if ( dist<=self.polygon.radius )
                {
                    self.polygon.startDrag(closest);
                }
                else if ( self.polygon.ptOnEdge(pt) )
                {
                    var newPt = self.polygon.addPt(pt);
                    self.qt.addPt(newPt);
                    self.qt.updatePolygon(self.polygon);
                }
                else
                {
                    if ( inside )
                    {
                        self.polygon.setAnchorNormal();
                    }
                    self.upPt= undefined;
                }
                self.downPt = pt;
            }
            else
            {
                self.polygon.anchored = self.polygon.pointInPoly(pt);
                if ( self.polygon.anchored )
                {
                    self.polygon.drawControlPoints(ctx);
                }
            }
        }
        else
        {
            self.polygon = self.qt.pointInPolygon(pt);
            if ( self.polygon != undefined )
                self.polygon.anchored = true;
        }
    });
    $("#"+this.id).mouseup(function(event) {
        if ( self.polygon != undefined )
        {
            var canvas = $("#"+self.id);
            var offset = canvas.offset();
            var ctx = canvas[0].getContext("2d");        
            var orig = new Point(event.pageX-offset.left,event.pageY-offset.top);
            var pt = orig.toAbsolute( ctx );
            var closest = self.qt.hasPoint(pt,undefined);
            //this stops endDrag from being called
            if ( closest!=undefined && closest.equals(self.polygon.dragPt) )
                self.polygon.setHighlightPt(closest);
            else if ( self.polygon.dragging() )
            {
                var quad = self.qt.getQuad(self.polygon.dragPt);
                if ( quad != undefined )
                {
                    quad.removePt(self.polygon.dragPt);
                    self.polygon.dragTo(pt);
                    self.qt.addPt(self.polygon.dragPt);
                    self.polygon.endDrag(qt);
                }
                else
                    console.log("quad for "+self.polygon.dragPt.x
                    +":"+self.polygon.y+" is undefined");
            }
            else if ( self.downPt != undefined )
            {
                 var upInPoly = self.polygon.pointInPoly(pt);
                 if ( !upInPoly )
                 {
                     if ( self.downPt.equals(pt) )
                     {
                         self.polygon.erase(ctx);
                         self.polygon.anchored = false;
                     }
                 }
            }
            if ( self.downPt != undefined && self.upPt != undefined )
            {
                var polygons = self.polygon.split(self.downPt,self.upPt);
                self.qt.removePolygon( self.polygon );
                self.polygon.erase(ctx);
                if ( polygons != undefined )
                {
                    for ( var i=0;i<polygons.length;i++ )
                        self.qt.addPolygon(polygons[i]);
                    if( polygons.length>0 )
                        self.polygon = polygons[0];
                }
                self.polygon.anchored = true;
                self.polygon.setAnchorNormal();
            }
            self.downPt = undefined;
            self.upPt = undefined;
        }
    });
    /**
     * Handle key-down events for whole document
     */
    $(document).keydown(function(event){
        if ( self.polygon != undefined && self.polygon.highlightPt != undefined )
            if ( event.which == 8 || event.which == 46 )
            {
                self.qt.removePt(self.polygon.highlightPt);
                self.qt.updatePolygon(self.polygon);
                self.polygon.deleteHighlightPt();
            }
        // otherwise we should remove the whole polygon
        return false;
    });
    /**
     * Compute the cliprect to erase when dragging a point
     */
    this.clipRect = function(ctx)
    {
        var up = this.upPt.toLocal(ctx);
        var down = this.downPt.toLocal(ctx);
        var left = (down.x<up.x)?down.x:up.x;
        var right = (down.x>up.x)?down.x:up.x;
        var top = (up.y<down.y)?up.y:down.y;
        var bot = (up.y>down.y)?up.y:down.y;
        var r = new Rect(left,top,right-left,bot-top);
        r.expand(2);
        return r;
    };
    /**
     * Add a whole new polygon
     * @param pg the polygon to add
     */
    this.addPolygon = function( pg ) {
        this.polygon = pg;
        this.qt.addPolygon( pg );
    };
    /**
     * Convert the geojson relative points (fractions of width, height) 
     * to an array of Point objects
     * @param pts the geojson points
     * @return an aray of Point objects in absolute coordinates
     */
    this.convertPoints = function( pts ) {
        var array = new Array();
        for ( var i=0;i<pts.length;i++ )
        {
            var xy = pts[i];
            var pt = new Point(xy[0],xy[1]);
            array.push(pt);
        }
        return array;
    };
    /**
     * Rebuild the quadtree based on fresh geojson document
     * @param geojson the new geojson data
     */
    this.reload = function( geojson ) {
        var gjDoc = JSON.parse(geojson);
        if ( gjDoc.features != undefined )
        {
            var lines = gjDoc.features;
            for ( var i=0;i<lines.length;i++ )
            {
                if ( lines[i].features != undefined )
                {
                    var polys = lines[i].features;
                    for ( var j=0;j<polys.length;j++ )
                    {
                        if ( polys[j].geometry!=undefined
                            &&polys[j].geometry.coordinates!=undefined)
                        {
                            var pts = this.convertPoints(polys[j].geometry.coordinates);
                            var pg = new Polygon(pts,"tilt");
                            if ( this.polygon == undefined )
                                this.polygon = pg;
                            this.qt.addPolygon(pg);
                        }
                    }
                }
            }
        }
    };
}
