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
function bindJsonToImage( json )
{
    var img = $("#left img");
    if ( img == undefined )
        alert("img is undefined");
    var ht = jqImg.height();
    var wt = jqImg.width();
    var cstr = '<canvas width="'+wt+'" height="'+ht+'"></canvas>';
    var src = jqImg.attr("src");
    jqImg.replaceWith(cstr);
    var canvas = $("#left canvas");
    var ctx = canvas.get(0).getContext('2d');
    var img = new Image;
    img.src = src;
    ctx.drawImage( img, 0, 0, wt, ht );
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
