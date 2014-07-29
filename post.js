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
    this.right = function()
    {
        return this.x+this.width;
    };
    this.bottom = function()
    {
        return this.y+this.height;
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
    this.containsPoint = function( P )
    {
        var cn = 0;
        var V = this.points;
        for (var i=0; i<V.length-1; i++) 
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
    this.fill = function( ctx )
    {
         var b = this.bounds;
         this.savedImageData = ctx.getImageData( b.x, b.y, b.width, b.height ); 
         ctx.save();
         ctx.beginPath();
         ctx.moveTo(this.points[0].x,this.points[0].y);
         for (var i=1;i<this.points.length; i++)
         {
             ctx.lineTo(this.points[i].x,this.points[i].y);
         }
         ctx.fillStyle = "rgba(255, 0, 0, 0.25)";
         ctx.closePath();
         ctx.fill();
         ctx.restore();
    };
    this.restore = function(ctx)
    {
        var b = this.bounds;
        if ( this.savedImageData != undefined )
        {
            ctx.putImageData( this.savedImageData, b.x, b.y );
        }
    }
}
function Bucket( p )
{
    this.poly = p;
    this.append = function( p )
    {
        var temp = this;
        while ( temp.next != undefined )
            temp = temp.next;
        temp.next = new Bucket(p);
    };
    this.getAll = function()
    {
        var all = new Array();
        var temp = this;
        while ( temp != undefined )
        {
            all[all.length] = temp.poly;
            temp = temp.next;
        }
        return all;
    };
}
function Hash2DTree( json )
{
    var geoJson = JSON.parse( json );
    var canvas = $("#left canvas");
    var ht = canvas.height();
    var wt = canvas.width();
    var x1 = Math.round(geoJson.bbox[0]*wt);
    var y1 = Math.round(geoJson.bbox[1]*ht);
    var x2 = Math.round(geoJson.bbox[2]*wt);
    var y2 = Math.round(geoJson.bbox[3]*ht);
    var width = x2-x1;
    var height = y2-y1;
    this.hBuckets = Math.round((width<height)?50:75);
    this.hUnit = Math.floor(width/this.hBuckets);
    this.vBuckets = Math.round((width<height)?75:50);
    this.vUnit = Math.floor(height/this.vBuckets);
    this.buckets = new Array();
    this.getIndices = function( p )
    {
        var b = p.bounds;
        var firstX = Math.floor(b.x/this.hUnit);
        var lastX = Math.floor(b.right()/this.hUnit);
        var firstY = Math.floor(b.y/this.vUnit);
        var lastY = Math.floor(b.bottom()/this.vUnit);
        var indices = new Array();
        var i = 0;
        for ( var x=firstX;x<=lastX;x++ )
        {
            for ( var y=firstY;y<=lastY;y++ )
                indices[i++] = x*128 + y;
        }
        return indices;
    };
    for ( var i=0;i<geoJson.features.length;i++ )
    {
        var line = geoJson.features[i];
        polygon = 0;
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
                    var indices = this.getIndices(p);
                    for ( var k=0;k<indices.length;k++ )
                    {
                       var index = indices[k];
                       if ( this.buckets[index] == undefined )
                           this.buckets[index] = new Bucket(p);
                       else
                           this.buckets[index].append(p);
                    }
                }
            }
        }
    }
    this.find = function( x, y )
    {
        var indX = Math.floor(x/this.hUnit);
        var indY = Math.floor(y/this.vUnit);
        var index = indX*128 + indY;
        var pindex = index;
        var b = this.buckets[index];
        if ( b == undefined )
            return undefined;
        else 
            return b.getAll();
    };
}
function bindJsonToImage( json )
{
    var jqImg = $("#left img");
    var ht = jqImg.height();
    var wt = jqImg.width();
    var cstr = '<canvas width="'+wt+'" height="'+ht+'">No HTML5!</canvas>';
    var src = jqImg.attr("src");
    jqImg.replaceWith(cstr);
    var canvas = $("#left canvas");
    currentPoly = undefined;
    canvas.mousemove(function(e){
        var c = $("#left canvas");
        var r = c.get(0).getBoundingClientRect();
        var x = e.clientX - r.left;
        var y = e.clientY - r.top;
        var polys = tree.find(x,y);
        if ( polys != undefined )
        {
            var p = new Point(x,y);
            for ( var i=0;i<polys.length;i++ )
            {
                if ( polys[i].containsPoint(p) )
                {
                    if ( currentPoly != undefined )
                        currentPoly.restore(ctx);
                    polys[i].fill(ctx);
                    currentPoly = polys[i];
                    break;
                }
            }
        }
    });
    var ctx = canvas.get(0).getContext('2d');
    tree = new Hash2DTree(json);
    tree.img = new Image;
    tree.img.src = src;
    ctx.drawImage( tree.img, 0, 0, wt, ht );
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
