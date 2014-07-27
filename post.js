function getUrl(geojson)
{
    var obj = JSON.parse(geojson);
    if ( obj && obj.properties.url )
	    return obj.properties.url;
    else
	    return "http://setis.library.usyd.edu.au/ozedits/harpur/A87-1/00000005.jpg";
}
function escapeUrl( url )
{
    var newUrl = "";
    for ( var i=0;i<url.length;i++ )
    {
	    var token = url.charAt(i);
	    switch ( token )
	    {
	    case '/':
		    newUrl += "%2F";
		    break;
	    case ' ':
		    newUrl += '%20';
		    break;
	    case ':':
		    newUrl += '%3A';
		    break;
	    default:
		    newUrl += url.charAt(i);
		    break;
	    }
    }
    return newUrl;
}
function getUrlFromLoc( pictype )
{
	var gjurl = getUrl($("#geojson").val());
	var url = "/tilt/image/?docid="+escapeUrl(gjurl)+"&pictype="+pictype;
	return url;
}
function getGeoJsonUrl()
{
	var gjurl = getUrl($("#geojson").val());
	var url = "/tilt/geojson/?docid="+escapeUrl(gjurl);
	return url;
}
function enableAll()
{
	$("#original").prop('disabled', false);
	$("#greyscale").prop('disabled', false);
	$("#twotone").prop('disabled', false);
	$("#cleaned").prop('disabled', false);
	$("#baselines").prop('disabled', false);
	$("#words").prop('disabled', false); 
    $("#link").prop('disabled', false); 
}
function disableAll()
{
	$("#original").prop('disabled', true);
	$("#greyscale").prop('disabled', true);
	$("#twotone").prop('disabled', true);
	$("#cleaned").prop('disabled', true);
	$("#baselines").prop('disabled', true);
	$("#words").prop('disabled', true);
    $("#link").prop('disabled', true); 
}
function Point( x, y )
{
    this.x = x;
    this.y = y;
    this.minus = function(p)
    {
        return new Point(this.x-p.x,this.y-p.y);
    };
}
function scalePoints( coords )
{
    var canvas = $("#left canvas");
    var ht = canvas.height();
    var wt = canvas.width();
    var points = new Array();
    for ( var i=0;i<coords.length;i++ )
    {
        points[points.length] = new Point(
            Math.round(coords[i][0]*wt),
            Math.round(coords[i][1]*ht)
        );
    }
    return points;
}
function Bounds( x, y, width, height )
{
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.within = function( r )
    {
        return this.x>=r.x 
            && this.y>=r.y 
            && this.x+this.width<r.x+r.width 
            && this.y+this.height<r.y+r.height;
    };
    this.outside = function( r )
    {
        return this.bottom()<r.y
        || this.y>=r.bottom()
        || this.right()<r.x
        || this.x >=r.right();
    };
    this.right = function()
    {
        return this.x+this.width;
    };
    this.bottom = function()
    {
        return this.y+this.height;
    };
    this.hasPt = function( x, y )
    {
        return x>=this.x&&y>=this.y&&y<this.bottom&&x<this.right();
    };
}
function Polygon( coords, props )
{
    if ( props != undefined )
    {
        if ( props.offset != undefined )
        this.offset = props.offset;
        if ( props.hyphen != undefined )
            this.hyphen = hyphen;
    }
    this.points = scalePoints(coords);
    this.SMALL_NUM = 0.00000001;
    var str = "";
    var x=Number.MAX_VALUE,y=Number.MAX_VALUE;
    var bot=0,right=0;
    for ( var i=0;i<this.points.length;i++ )
    {
        if ( this.points[i].x < x )
            x = this.points[i].x;
        if ( this.points[i].y < y )
            y = this.points[i].y;
        if ( this.points[i].x > right )
            right = this.points[i].x;
        if ( this.points[i].y > bot )
            bot = this.points[i].y;
    }
    this.bounds = new Bounds( x, y, right-x, bot-y );
    this.dot = function(u,v)
    {
        return ((u).x * (v).x + (u).y * (v).y);
    };
    this.perp = function(u,v)  
    {
        return ((u).x * (v).y - (u).y * (v).x);
    };
    this.cn_PnPoly = function( P, V, n )
    {
        var cn = 0;
        for (var i=0; i<n; i++) 
        {
            if (((V[i].y <= P.y) && (V[i+1].y > P.y))
            || ((V[i].y > P.y) && (V[i+1].y <=  P.y))) 
            {
                var vt = (P.y  - V[i].y) / (V[i+1].y - V[i].y);
                if (P.x <  V[i].x + vt * (V[i+1].x - V[i].x))
                     ++cn;
            }
        }
        return cn&1;
    };
    this.intersectsLine = function( p0, p1 )
    {
        if ( p0 == p1 )      
            return cn_PnPoly( p0, V, n );
        var tE = 0;
        var tL = 1;
        var t, N, D;
        var dS = p1.minus(p0);
        var e; 
        for ( var i=0; i<this.points.length; i++)
        {
            e = this.points[i+1].minus(this.points[i]);
            N = perp(e, p0.minus(this.points[i]));
            D = -perp(e, dS);
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
        return true;
    };
    this.overlaps = function( bounds )
    {
        if (this.bounds.within(bounds) )
           return true;
        else if ( this.bounds.outside(bounds) )
            return false;
        else 
        {
            var p0 = new Point(bounds.x,bounds.y);
            var p1 = new Point(bounds.right(),bounds.y);
            var p2 = new Point(bounds.x,bounds.bottom());
            var p3 = new Point(bounds.right().bounds.bottom());
            return this.intersectsLine(p0,p1)
                || this.intersectsLine(p1,p3)
                || this.intersectsLine(p2,p3)
                || this.intersectsLine(p0,p2);
        }
    };
    this.fill = function( ctx )
    {
         ctx.moveTo(points[0].x,points[0].y);
         for (var i=1;i<points.length; i++) 
         {
             ctx.lineTo(points[i].x,points[i].y);
         }
         ctx.fillStyle = "red";
         ctx.closePath();
         ctx.fill();
         ctx.stroke();
    };
}
function Quadrant( bounds )
{
    this.bounds = bounds;
    this.add = function( poly )
    {
        if ( poly.overlaps(this.bounds) )
        {
            if ( this.children == undefined && this.poly == undefined )
                this.poly = poly;
            else
            {
                setTopLeft(poly);
                if ( this.poly != undefined )
                    setTopLeft(this.poly);
                setTopRight(poly);
                if ( this.poly != undefined )
                    setTopRight(this.poly);
                setBotLeft(poly);
                if ( this.poly != undefined )
                    setBotLeft(this.poly);
                setBotRight(poly);
                if ( this.poly != undefined )
                    setBotRight(this.poly);
                this.poly = undefined;
                this.children = true;
            }
        }
    };
    this.setTopLeft = function( poly )
    {
        if ( this.tl == undefined )
        {
            var b = new Bounds(
                this.bound.x,
                this.bounds.y,
                Math.round(this.bounds.width/2),
                Math.round(this.bounds.height/2)
            );
            if ( poly.overlaps(b) )
            {
                this.tl = new Quadrant(b);
                this.tl.poly = poly;
            }
        }
        else
            this.tl.add( poly );
    };
    this.setTopRight = function( poly )
    {
        if ( this.tl == undefined )
        {
            var newX = this.bounds.x+this.bounds.width/2;
            var b = new Bounds(
                newX, 
                this.bounds.y,
                Math.round(this.bounds.right()-newX),
                Math.round(this.bounds.height/2)
            );
            if ( poly.overlaps(b) )
            {
                this.tr = new Quadrant(b);
                this.tr.poly = poly;
            }
        }
        else
            this.tr.add( poly );
    };
    this.setBotLeft = function( poly )
    {
        if ( this.bl == undefined )
        {
            var newY = this.bounds.y+this.bounds.height/2;
            var b = new Bounds(
                this.bound.x,
                newY,
                Math.round(this.bounds.width/2),
                Math.round(this.bounds.bottom()-newY)
            );
            if ( poly.overlaps(b) )
            {
                this.bl = new Quadrant(b);
                this.bl.poly = poly;
            }
        }
        else
            this.bl.add( poly );
    };
    this.setBotRight = function( poly )
    {
        if ( this.br == undefined )
        {
            var newX = this.bounds.x+this.bounds.width/2;
            var newY = this.bounds.y+this.bounds.height/2;
            var b = new Bounds(
                newX,
                newY,
                Math.round(this.bounds.right()-newX),
                Math.round(this.bounds.bottom()-newY)
            );
            if ( poly.overlaps(b) )
            {
                this.br = new Quadrant(b);
                this.br.poly = poly;
            }
        }
        else
            this.br.add( poly );
    };
    this.find = function( x, y )
    {
        if ( this.bounds.hasPt(x,y) )
        { 
            if ( !this.children && this.poly != undefined )
                return this.poly;
            else
            {
                var poly = undefined;
                if ( this.tl != undefined )
                    poly = this.tl.find(x,y);
                if ( poly==undefined && this.tr != undefined )
                    poly = this.tr.find(x,y);
                if ( poly==undefined && this.bl != undefined )
                    poly = this.bl.find(x,y);
                if ( poly==undefined && this.br != undefined )
                    poly = this.br.find(x,y);
                return poly;
            }
        }
        else
            return undefined;
    };
}
function QuadTree( json )
{
    var geoJson = JSON.parse( json );
    var canvas = $("#left canvas");
    var ht = canvas.height();
    var wt = canvas.width();
    var x1 = Math.round(geoJson.bbox[0]*wt);
    var y1 = Math.round(geoJson.bbox[1]*ht);
    var x2 = Math.round(geoJson.bbox[2]*wt);
    var y2 = Math.round(geoJson.bbox[3]*ht);
    this.root = new Quadrant( new Bounds(x1,y1,x2-x1,y2-y1) );
    for ( var i=0;i<geoJson.features.length;i++ )
    {
        var line = geoJson.features[i];
        for ( var j=0;j<line.features.length;j++ )
        {
            var poly = line.features[j];
            if ( poly.type=="Feature" )
            {
                var geometry = poly.geometry;
                if ( geometry.type=="Polygon" )
                {
                    var p = new Polygon(geometry.coordinates,
                        poly.properties);
                    this.root.add( p );
                }
            }
        }
    }
    this.find = function( x, y )
    {
        return this.root.find( x, y );
    };
}
function bindJsonToImage( json )
{
    var jqImg = $("#left img");
    var ht = jqImg.height();
    var wt = jqImg.width();
    var cstr = '<canvas width="'+wt+'" height="'+ht+'"></canvas>';
    var src = jqImg.attr("src");
    jqImg.replaceWith(cstr);
    var canvas = $("#left canvas");
    canvas.mousemove(function(e){
        var c = $("#left canvas");
        var x = e.clientX - c.offset().x;
        var y = e.clientY - c.offset().y;
        var poly = quadTree.find(x,y);
        if ( poly != undefined )
        {
            ctx.drawImage(quadTree.img, 0, 0, c.width, c.height );
            poly.fill(ctx);
        }
    });
    var ctx = canvas.get(0).getContext('2d');
    quadTree = new QuadTree(json);
    quadTree.img = new Image;
    img.src = src;
    ctx.drawImage( quadTree.img, 0, 0, wt, ht );
}
$(document).ready(function() {
disableAll();
$("#upload").click(function(e){
	var localurl = "http://"+location.hostname+"/tilt/";
	$.post( localurl, $("#main").serialize(), function( data ) {
	$("#left").empty();
	$("#left").html(data);
	enableAll();
	},"text").fail(function(xhr, status, error){
	alert(status);
	disableAll();
	});
});
$("#original").click(function(){
	$("#left").empty();
	$("#left").html('<img width="500" src="'+getUrlFromLoc("original")+'">');
});
$("#greyscale").click(function(){
	$("#left").empty();
	$("#left").html('<img width="500" src="'+getUrlFromLoc("greyscale")+'">');
});
$("#twotone").click(function(){
	var url = escapeUrl(getUrlFromLoc("twotone"));
	$("#left").empty();
	$("#left").html('<img width="500" src="'+getUrlFromLoc("twotone")+'">');
});
$("#cleaned").click(function(){
	var url = escapeUrl(getUrlFromLoc("cleaned"));
	$("#left").empty();
	$("#left").html('<img width="500" src="'+getUrlFromLoc("cleaned")+'">');
});
$("#baselines").click(function(){
	var url = escapeUrl(getUrlFromLoc("baselines"));
	$("#left").empty();
	$("#left").html('<img width="500" src="'+getUrlFromLoc("baselines")+'">');
});
$("#words").click(function(){
	var url = escapeUrl(getUrlFromLoc("words"));
	$("#left").empty();
	$("#left").html('<img width="500" src="'+getUrlFromLoc("words")+'">');
});
$("#link").click(function(){
	var url = escapeUrl(getUrlFromLoc("link"));
	$("#left").empty();
	$("#left").html('<img width="500" src="'+getUrlFromLoc("link")+'">');
    var gjurl = getGeoJsonUrl();
    $.get( gjurl, function( data ) {
    bindJsonToImage(data);
    });
});
$("#selections").change(function(e){
	var base_id = $("#selections").val();
    var thtml = $("#"+base_id+"_TEXT").html();
	$("#geojson").val($("#"+base_id+"_JSON").text());
	$("#content").html(thtml);
	$("#text").val(thtml);
	disableAll();
});
$("#content").width($("#geojson").width());
var htext = $("#HARPUR_JSON").text();
$("#geojson").val(htext);
var hhtml = $("#HARPUR_TEXT").html();
$("#content").html(hhtml);
$("#text").val(hhtml);
$("#selections").val("HARPUR");
});
