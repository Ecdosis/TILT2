/**
 * Handle mouse-events and redrawing canvas-wide
 * @param id the id ofthe HTML canvas element to bind to
 * @param wt initial width of the canvas/image
 * @param ht initial height of the canvas/image
 */
function Canvas( id, wt, ht )
{
    // the id of the html5 canvas object
    this.id = id;
    // the polygon with focus
    this.polygon = undefined;
    // where the user moused down last
    this.downPt = undefined;
    // the quadtree for finding points and polygons on the page
    this.qt = new QuadTree(0,0,wt,ht);
    //where the user last performed mouse-up
    this.upPt = undefined;
    // reference to the Canvas object
    var self = this;
    /**
     * Handle all mouse-moved events inside the canvas 
     */
    $("#"+self.id).mousemove(function( event ) {
        var canvas = "#"+self.id;
        var offset = $(canvas).offset();
        var pt = new Point(event.pageX-offset.left,event.pageY-offset.top);
        if ( !self.polygon.anchored )
        {
            var ctx = $(canvas)[0].getContext("2d");
            if ( self.polygon.pointInPoly(pt) )
                self.polygon.drawPolygon(ctx);
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
            var ctx = $(canvas)[0].getContext("2d");
            if ( self.upPt != undefined )
            {
                var r = self.clipRect();
                ctx.save();
                ctx.rect(r.x,r.y,r.width,r.height);
                ctx.clip();
                self.polygon.redraw(ctx);
                ctx.restore();
            }
            ctx.save();
            ctx.beginPath();
            ctx.moveTo(self.downPt.x,self.downPt.y);
            ctx.strokeStyle = '#000000';
            ctx.lineWidth = 2;
            ctx.setLineDash([2,3]);
            ctx.lineTo(pt.x,pt.y);
            ctx.stroke();
            ctx.restore();
            self.upPt = pt;
        }
    });
    /**
     * Handle all mouse-down events inside the canvas 
     */
    $("#"+this.id).mousedown(function(event) {
        var canvas = "#"+self.id;
        var offset = $(canvas).offset();
        var pt = new Point(event.pageX-offset.left,event.pageY-offset.top);
        if ( self.polygon.anchored )
        {
            var inside = self.polygon.pointInPoly(pt);
            var closest = self.qt.hasPoint(pt,undefined);
            var dist = (closest!=undefined)?self.qt.distance(closest,pt):Number.MAX_VALUE;
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
                var ctx = $(canvas)[0].getContext("2d");
                self.polygon.drawControlPoints(ctx);
            }
        }
    });
    $("#"+this.id).mouseup(function(event) {
        var canvas = "#"+self.id;
        var offset = $(canvas).offset();
        var pt = new Point(event.pageX-offset.left,event.pageY-offset.top);
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
                     var ctx = $(canvas)[0].getContext("2d");
                     self.polygon.erase(ctx);
                     self.polygon.anchored = false;
                 }
             }
        }
        if ( self.downPt != undefined && self.upPt != undefined )
        {
            var polygons = self.polygon.split(self.downPt,self.upPt);
            self.qt.removePolygon( self.polygon );
            var ctx = $(canvas)[0].getContext("2d");
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
    this.clipRect = function()
    {
        var left = (this.downPt.x<this.upPt.x)?this.downPt.x:this.upPt.x;
        var right = (this.downPt.x>this.upPt.x)?this.downPt.x:this.upPt.x;
        var top = (this.upPt.y<this.downPt.y)?this.upPt.y:this.downPt.y;
        var bot = (this.upPt.y>this.downPt.y)?this.upPt.y:this.downPt.y;
        var r = new Rect(left,top,right-left,bot-top);
        r.expand( 2 );
        return r;
    };
    this.addPolygon = function( pg ) {
        this.polygon = pg;
        this.qt.addPolygon( pg );
    };
    /**
     * Scale the geojson relative points (fractions of width, height)
     * @param pts the geo json points
     * @return an aray of Point objects in absolute coordinates
     */
    this.scalePoints = function( pts ) {
        var array = new Array();
        for ( var i=0;i<pts.length;i++ )
        {
            var xy = pts[i];
            var pt = new Point(xy[0]*this.qt.width(),xy[1]*this.qt.height());
            array.push(pt);
        }
        return array;
    };
    /**
     * Resize this canvas by saling everything
     * @param wt the new canvas/image width
     * @param ht the new canvas/image height
     */
    this.resize = function( wt, ht ) {
    };
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
                            var pts = this.scalePoints(polys[j].geometry.coordinates);
                            var pg = new Polygon(pts,"tilt");
                            this.qt.addPolygon(pg);
                        }
                    }
                }
            }
        }
    };
}
