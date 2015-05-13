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
function getImageUrl( pictype )
{
	var docid = $("#docid").val();
    var pageid = $("#pageid").val();
	return "/tilt/image/?docid="+docid+"&pageid="+pageid+"&pictype="+pictype;
}
function getGeoJsonUrl()
{
	var docid = $("#docid").val();
    var pageid = $("#pageid").val();
	return "/tilt/geojson?docid="+docid+"&pageid="+pageid;
}
function getTextUrl()
{
	var docid = $("#docid").val();
    var pageid = $("#pageid").val();
	return "/pages/html?docid="+docid+"&pageid="+pageid;
}
function enableAll()
{
	$("#preflight").prop('disabled',false);
	$("#greyscale").prop('disabled', false);
	$("#twotone").prop('disabled', false);
	$("#cleaned").prop('disabled', false);
	$("#reconstructed").prop('disabled', false);
    $("#baselines").prop('disabled', false);
	$("#words").prop('disabled', false); 
    $("#link").prop('disabled', false); 
}
function disableAll()
{
	$("#preflight").prop('disabled',true);
	$("#greyscale").prop('disabled', true);
	$("#twotone").prop('disabled', true);
	$("#cleaned").prop('disabled', true);
	$("#reconstructed").prop('disabled', true);
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
        else
            this.offset = -1;
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
    this.reverseIndex = new Array();
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
    var polyList = new Array();
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
                    var indices = this.getIndices(p);
                    for ( var k=0;k<indices.length;k++ )
                    {
                       var index = indices[k];
                       if ( this.buckets[index] == undefined )
                           this.buckets[index] = new Bucket(p);
                       else
                           this.buckets[index].append(p);
                    }
                    polyList[polyList.length] = p;
                    if ( p.offset != undefined )
                    {
                        p.id = "O_"+p.offset;
                        this.reverseIndex[p.id] = p;
                    }
                }
            }
        }
    }
    this.shellsort = function(a)
    {
	    for (var h = a.length; h = parseInt(h/2);) {
            for (var i = h; i < a.length; i++) {
                var k = a[i];
                for (var j=i;j>=h && k.offset<a[j-h].offset; j-=h)
                    a[j] = a[j-h];
                a[j] = k;
            }
        }
        return a;
    }
    /**
     * Process the html text into spans with IDs corresponding 
     * to the offsets int eh polygons
     */
    this.processPolys = function( parray )
    {
        this.shellsort(parray);
        var text = $("#text").val();
        var lfPos = text.indexOf("\n");
        console.log(lfPos);
        for ( var i=parray.length-1;i>=0;i-- )
        {
            var poly = parray[i];
            if ( poly.offset != undefined )
            {
                var spacePos = text.indexOf(" ",poly.offset);
                var brPos = text.indexOf("<",poly.offset);
                var end = Math.min(spacePos,brPos);
                if ( brPos == -1 && spacePos == -1 )
                     end = text.length;
                else if ( brPos == -1 )
                    end = spacePos;
                else if ( spacePos == -1 )
                    end = brPos;
                var prev = text.slice(0,poly.offset);
                var word = text.slice(poly.offset,end);
                var tail = text.slice(end);
                text = prev.concat('<span class="word" id="'+poly.id+'">',word,"</span>",tail);
            }
        }
        $("#content").html(text);
    }
    this.processPolys( polyList );
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
function appendCanvas()
{
    var container = $("#container");
    var jqImg = $("#left img");
    if ( jqImg != undefined )
    {
        var ht = jqImg.height();
        var wt = jqImg.width();
        var cstr = '<canvas width="'+wt+'" height="'+ht+'">No HTML5!</canvas>';
        if ( $("#container").has("canvas"))
            $("#container canvas").remove();
        container.append(cstr);
    }
}
function bindJsonToImage( json )
{
    appendCanvas();
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
                    {
                        currentPoly.restore(ctx);
                        $("#"+currentPoly.id).unhighlight();
                    }
                    polys[i].fill(ctx);
                    currentPoly = polys[i];
                    var target = $("#"+currentPoly.id);
                    target.highlight(target.text());
                    break;
                }
            }
        }
    });
    var ctx = canvas.get(0).getContext('2d');
    tree = new Hash2DTree(json);
    $(".word").mousemove(function(e) {
        var id = $(this).attr("id");
        if ( id != undefined )
        {
            if ( currentPoly != undefined )
            {
                currentPoly.restore(ctx);
                $("#"+currentPoly.id).unhighlight();
            }
            var poly = tree.reverseIndex[id];
            poly.fill(ctx);
            currentPoly = poly;
            $(this).highlight($(this).text());
        }
    });
}
function loadSelection()
{
    var value = $("#selections").val();
    var parts = value.split('#');
    if ( parts.length == 2 )
    {
        $("#docid").val(parts[0]);
        $("#pageid").val(parts[1]);
        var imageUrl = getImageUrl( "load" );
        $("#container").empty();
        $("#container").html('<img src="'+imageUrl+'" width="500">'); 
        appendCanvas();
        enableAll();
    }
    var textUrl = getTextUrl();
    $.get( textUrl, function( thtml ) {
        $("#text").val(thtml);
        $("#content").html(thtml);
    });
}
$(document).ready(function() {
disableAll();
$("#original").click(function(){
	$("#container").empty();
	$("#container").html('<img width="500" src="'+getImageUrl("original")+'">');
    appendCanvas()
});
$("#preflight").click(function(){
	$("#container").empty();
	$("#container").html('<img width="500" src="'+getImageUrl("preflight")+'">');
    appendCanvas();
});
$("#greyscale").click(function(){
	$("#container").empty();
	$("#container").html('<img width="500" src="'+getImageUrl("greyscale")+'">');
    appendCanvas();
});
$("#twotone").click(function(){
	var url = escapeUrl(getImageUrl("twotone"));
	$("#container").empty();
	$("#container").html('<img width="500" src="'+getImageUrl("twotone")+'">');
    appendCanvas();
});
$("#cleaned").click(function(){
	var url = escapeUrl(getImageUrl("cleaned"));
	$("#container").empty();
	$("#container").html('<img width="500" src="'+getImageUrl("cleaned")+'">');
    appendCanvas();
});
$("#reconstructed").click(function(){
	var url = escapeUrl(getImageUrl("reconstructed"));
	$("#container").empty();
	$("#container").html('<img width="500" src="'+getImageUrl("reconstructed")+'">');
    appendCanvas();
});
$("#baselines").click(function(){
	var url = escapeUrl(getImageUrl("baselines"));
	$("#container").empty();
	$("#container").html('<img width="500" src="'+getImageUrl("baselines")+'">');
    appendCanvas();
});
$("#words").click(function(){
	var url = escapeUrl(getImageUrl("words"));
	$("#container").empty();
	$("#container").html('<img width="500" src="'+getImageUrl("words")+'">');
    appendCanvas();
});
$("#link").click(function(){
	var url = escapeUrl(getImageUrl("link"));
	$("#container").empty();
	$("#container").html('<img id="linked-image" width="500" src="'+getImageUrl("link")+'">');
    $("#linked-image").load(function(){
        var gjurl = getGeoJsonUrl();
        $.get( gjurl, function( data ) {
            bindJsonToImage(data);
        });
    });
});
$("#selections").change(function(e){
	loadSelection();
});
loadSelection();
});
