function Tilt() {
    var self = this;
    /**
     * Scale image
     * @param factor the mag factor
     */
    this.setImageScale = function( factor ) {
        var dataMagnify = $("#image").attr("data-magnify");
        var scales = dataMagnify.split(" ");
        if ( scales.length == 10 )
        {
            //console.log("old width="+$("#image").width());
            var img = $("#image > img");
            var awt = parseInt(scales[factor*2]);
            var aht = parseInt(scales[factor*2+1]);
            img.width(awt);
            img.height(aht);
            //console.log("new width="+$("#image").width());
            //console.log("overflow="+$("#image").css("overflow"));
        }
    };
    /**
     * Centre image around location
     * @param globalX the global (window relative) X click location
     * @param globalY the global Y click location
     * @param factor the target scale factor between 1 and 4
     * @param oldFactor the current scale factor between 1 and 4
     */ 
    this.centreAround = function( globalX, globalY, factor, oldFactor ) {
        if ( factor >= 1 && factor <= 4 )
        {
            var dataMagnify = $("#image").attr("data-magnify");
            var scales = dataMagnify.split(" ");
            var currW = parseInt(scales[oldFactor*2]);
            var currH = parseInt(scales[oldFactor*2+1]);
            var nextW = parseInt(scales[factor*2]);
            var nextH = parseInt(scales[factor*2+1]);
            var pic = $("#image img");
            var top = parseInt(pic.css("top").split("px")[0]);
            var left = parseInt(pic.css("left").split("px")[0]);
            // localise global coordinates
            var localX = globalX-left;
            var localY = globalY-top;
            // scale localX and localY to new mag
            localX = (localX*nextW)/currW; 
            localY = (localY*nextH)/currH; 
            // actual window height and width
            var aWt = scales[2];
            var aHt = scales[3];
            // centre click 
            top = Math.round((aHt/2)-localY);
            left = Math.round((aWt/2)-localX);
            // sanity checks
            var minTop = aHt-nextH;
            var minLeft = aWt-nextW;
            if ( top < minTop )
                top = minTop;
            if ( left < minLeft )
                left = minLeft;
            if ( left > 0 )
                left = 0;
            if ( top > 0 )
                top = 0;
            pic.css("top", top+"px");
            pic.css("left",left+"px");
        }
    };
    /**
     * Safely decrement the level number
     * @param level the current level
     * @return the new level
     */
    this.decLevel = function( level ) {
        if ( level == -1 )
            return 0;
        else
            return level-1;
    };
    /**
     * Turn this level name into a level number
     * @param str
     * @return the elvel number
     */
    this.levelToNumber = function( str ) {
        if ( str == "standard" )
            return -1;
        else
            return parseInt(str.split("-")[1]);
    };
    /**
     * Workout the current zoom level name from its numerical level
     * @param number
     * @return the level name
     */
    this.numberToLevel = function( number ) {
        if ( number == -1 )
            return "standard";
        else
            return "level-"+number;
    };
    /**
     * Refresh the pages list based on the current document selected
     */
    this.refreshPages = function() {
        var doc = $("#documents").val();
        $.get($("#host-record").val()+"/list?docid="+doc,function(data){
            var pages = $("#pages");
            pages.empty();
            for ( var i=0;i<data.length;i++ )
            {
                var pageNo = i+1;
                pages.append("<option value=\""+data[i]+"\">"+pageNo+"</option>");
            }
        }).fail(function(){
            console.log("failed to create pages list");
        });
    };
    /**
     * Remove trailing "/pages" service name from URL if present
     * @param doc the document identifier
     */
    this.realDocid = function( doc ) {
        var lastSlashIndex = doc.lastIndexOf("/pages");
        if ( lastSlashIndex != -1 )
            doc = doc.substring(0,lastSlashIndex);
        return doc;
    };
    /**
     * Update the document and pages lists based on current url
     */
    this.refreshList = function() {
        $.get($("#host-record").val()+"/documents",function(data){
            var docs = $("#documents");
            docs.empty();
            for ( var i=0;i<data.length;i++ )
            {
                var option ="<option";
                if ( i == 0 )
                    option += " selected=\"selected\"";
                option += " value=\""+self.realDocid(data[i].docid)
                    +"\">"+data[i].title+"</option>";
                docs.append(option);
            }
            self.refreshPages();
        }).fail(function(){
            console.log("failed to refresh list");
        });
    };
    /**
     * Load the text and the image
     * @param docid the document identifier
     * @param pageid theid of the page-image
     */
    this.loadPage = function( docid, pageid ) {
        $.get($("#host-record").val()+"/html?docid="
            +docid+"&pageid="+pageid,function(data){
            var height = $("#image img").height();
            $("#flow").empty();
            $("#flow").append(data);
            var diff=$("#image").height()-$("#image img").height();
            $("#flow").height(aht-diff);
        }).fail(function(){
            console.log("failed to get text");
        });
        // get image
        $.get($("#host-record").val()+"/image?docid="+docid+"&pageid="+pageid,function(data){
            $("#image img").attr("src",data.trim());
        }).fail(function(){
            console.log("failed to get image");
        });
    };
    /**
     * Create an empty GeoJSON document
     */
    this.emptyDocument = function() {
        return {
          type: "Feature",
          geometry: {
              type: "Polygon",
                coordinates: [
                [ [0.0, 0.0], [100.0, 0.0], [100.0, 100.0], [0.0, 100.0] ]
              ]
          },
          properties: {
            url: "http://setis.library.usyd.edu.au/ozedits/harpur/A87-1/00000005.jpg"
          }
        };
    };
    /**
     * Load image and scale text blockto match
     */
    this.loadImage = function(){
        var iht = $("#image > img").height();
        var iwt = $("#image > img").width();
        var wht = $(window).height();
        // adjust image width and height to window height
        var awt = Math.round((iwt*wht)/iht);
        var aht = wht;
        // save magnifications as data-magnify attr on #image
        var mag1wt = Math.round(awt*1.5);
        var mag1ht = Math.round(aht*1.5);
        var mag2wt = Math.round(mag1wt*1.5);
        var mag2ht = Math.round(mag1ht*1.5);
        var mag3wt = Math.round(mag2wt*1.5);
        var mag3ht = Math.round(mag2ht*1.5);
        $("#image").attr("data-magnify",iwt+" "+iht+" "+awt+" "+aht
            +" "+mag1wt+" "+mag1ht+" "+mag2wt+" "+mag2ht+" "+mag3wt+" "+mag3ht);
        // set image height and width
        $("#image > img").height(aht);
        $("#image > img").width(awt);
        // scale wrapper divs to same values
        $("#image").height(aht);
        $("#image").width(awt);
        $("#tilt").height(aht);
        $("#tilt").width(awt);
        $("#tilt").css("left","-"+awt+"px");
        $("#text").width(awt);
        $("#text").height(aht);
        var diff=$("#image").height()-$("#image img").height();
        $("#flow").height(aht-diff);
    };
    /**
     * Toggle between honouring line-breaks and ignoring them
     */
    $("#justify-button").click(function()
    {
        var classtr = $("#justify-button").attr("class");
        var classNames = classtr.split(" ");
        if ( classNames[classNames.length-1]=="fa-justify" )
        {
            $("br").css("display","none");
            $(this).attr("title","line-breaks");
            $(this).attr("class","fa fa-2x fa-linebreaks");
            $("span.hard").attr("class","soft");
        }
        else
        {
            $("br").css("display","inline");
            $(this).attr("title","justify");
            $(this).attr("class","fa fa-2x fa-justify");
            $("span.soft").attr("class","hard");
        }
    });
    /**
     * Change the cursor and react to clicks when magnifying or minifying
     */
    $("#magnify-button").click(function()
    {
        var container = $("#container");
        var icon = $("#magnify-button");
        var cursor = container.attr("class");
        var level = self.decLevel(self.levelToNumber(cursor));
        if ( level < 0 )
        {
            container.attr("class","standard");
            icon.attr("class","fa fa-2x fa-magnify");
        }
        else if ( level >= 0 )
        {
            icon.attr("class","fa fa-2x fa-minify");
            container.attr("class","level-"+level);
            self.setImageScale( level+1 );
            if ( level > 0 )
                self.centreAround( container.width()/2+container.offset().left,
                    container.height()/2+container.offset().top, level+1, 
                    level+2 );
            else
                $("#image img").css({left:"0px",top:"0px"});
        }
    });
    $("#refresh").click(function(){
        self.refreshList();
    });
    $("#documents").change(function(){
        self.refreshPages();
    });
    $("#open-button").click(function()
    {
        var overlay = $("#overlay");
        self.refreshList();
        var visibility = overlay.css("visibility","visible");
    });
    $("#dismiss-button").click(function()
    {
        var overlay = $("#overlay");
        var doc = $("#documents").val();
        self.loadPage(doc,$("#pages").val());
        var visibility = overlay.css("visibility","hidden");
    });
    $("#recognise-button").click(function() {
        var geo = self.emptyDocument();
        var json = JSON.stringify(geo);
        var text = $("#flow").text();
        var obj = {geojson: json,pictype:"link",text:text};
        $.post('http://localhost/tilt/',obj,
            function(data){
                console.log("posted json data");
            }
        );
    });
    $("#image").click(function(event){
        var container = $("#container");
        var cursor = container.attr("class");
        var img = $("#image");
        var icon = $("#magnify-button");
        var level = self.levelToNumber(cursor);
        if ( level == 3 )
        {
            container.attr("class",self.numberToLevel(level-1));
            self.setImageScale( 3 );
            self.centreAround( event.pageX-img.offset().left,
                event.pageY-img.offset().top, 3, 4 );
        }
        else if ( level >= 0 )
        {
            container.attr("class",self.numberToLevel(level+1));
            self.setImageScale( level+2 );
            self.centreAround( event.pageX-img.offset().left,
                event.pageY-img.offset().top, level+2, level+1 );
        }
    });
    $("#host-record").val("http://localhost/pages");
    this.loadImage();
}
$(document).ready(function(){
    var tilt = new Tilt();
});

