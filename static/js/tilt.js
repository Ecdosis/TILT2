/**
 * Represent a HTML attribute
 */
function Attribute( name,value ) 
{
    /** the attribute name */
    this.name = name;
    /** the attribute value */
    this.value = value;
    /**
     * Stringify the attribute, preceding it with a space
     * @return an attribute compatible with HTML5
     */
    this.toString = function() {
        var attr = " "+name;
        if ( value.length>0 )
            attr += "=\""+value+"\"";
        return attr;
    };
}
/**
 * Represent a generic HTML5 element
 */
function Element( name )
{
    /** The contents turned into text as we go */
    this.contents = "";
    /** a list of the element's attributes */
    this.attrs = new Array();
    /** the tag name */
    this.name = name;
    /**
     * Add an attribute to the script element
     * @param name name of the attribute
     * @param value its value
     */
    this.addAttribute = function( name, value ) {
        this.attrs.push( new Attribute(name,value) );
    };
    /**
     * Add some text to the HTML
     * @param text
     */
    this.addText = function( text ) {
        this.contents += text;
    };
    /**
     * Add an element to the body
     * @param elem the element
     */
    this.addElement = function( elem ) {
        this.contents += elem.toString();
    };
    /**
     * Compose a HTML element for output
     * @return a String
     */
    this.toString = function() {
        sb = "";
        sb += "<";
        sb += this.name;
        for ( var i=0;i<this.attrs.length;i++ )
            sb += this.attrs[i].toString();
        sb += ">";
        sb += this.contents;
        sb += "</";
        sb += this.name;
        sb += ">\n";
        return sb;
    };
}
/**
 * The HTML container of the three panels
 */
function Container() {
    /**
     * Add a button to the toolbox list of buttons
     * @param list the unordered list element
     * @param title the titleofthe button (tooltip)
     * @param id the id of the button
     * @param button the button's class that defines its look
     */
    this.addButton = function( list, title, id, button )
    {
        var li = new Element("li");
        li.addAttribute("title",title);
        li.addAttribute("id",id);
        var span = new Element( "span" );
        span.addAttribute("class",button);
        li.addElement( span );
        list.addElement( li );
    }
    /**
     * Convert the entire container to a string of HTML
     * @return an HTMLstring being the page contents
     */
    this.toString = function() {
        return this.table.toString();
    };
    /**
     * Create an outer container for the editor, and fill it
     */
    this.table = new Element("table");
    this.table.addAttribute("id", "container");
    var row = new Element("tr");
    var toolbox = new Element("td");
    toolbox.addAttribute("id","toolbox");
    var list = new Element("ul");
    this.addButton(list,"Open...","open-button","fa fa-2x fa-folder-open-o");
    this.addButton(list,"save","save-button","fa fa-2x fa-save");
    this.addButton(list,"previous page","prev-button","fa fa-2x fa-chevron-left");
    this.addButton(list,"next page","next-button","fa fa-2x fa-chevron-right");
    this.addButton(list,"merge shapes","merge-button","fa fa-2x fa-merge");
    this.addButton(list,"split shape","split-button","fa fa-2x fa-split");
    this.addButton(list,"recognise","recognise-button","fa fa-2x fa-recognise");
    this.addButton(list,"re-align","align-button","fa fa-2x fa-align");
    this.addButton(list,"magnify","magnify-button","fa fa-2x fa-magnify");
    this.addButton(list,"justify","justify-button","fa fa-2x fa-justify");
    var image = new Element("td");
    image.addAttribute("id","image");
    var img = new Element("img");
    image.addElement( img );
    var canvas = new Element( "canvas" );
    canvas.addAttribute( "id", "tilt" );
    image.addElement( canvas );
    toolbox.addElement( list );
    row.addElement( toolbox );
    row.addElement( image );
    var text = new Element( "td" );
    text.addAttribute( "id","flow" );
    row.addElement( text );
    this.table.addElement( row );
}
/**
 * Generates an "overlay", which is a modal dialog for file open
 */
function Overlay()
{
    this.div = new Element("div");
    this.div.addAttribute("id","overlay");
    var inner = new Element("div");
    var table = new Element("table");
    //row 1: pages service url text box and refresh button
    var row1 = new Element( "tr" );
    var cell1 = new Element("td");
    var host =new Element("input");
    host.addAttribute("type","text");
    host.addAttribute("id","host-record");
    cell1.addElement(host);
    var refreshButton = new Element("span");
    refreshButton.addAttribute("id","refresh");
    refreshButton.addAttribute("class","fa fa-refresh");
    cell1.addElement(refreshButton);
    row1.addElement(cell1);
    table.addElement(row1);
    // row 2: dropdown of available documents
    var row2 = new Element("tr");
    cell1 = new Element("td");
    var documents = new Element("select");
    documents.addAttribute("id","documents");
    documents.addAttribute("size","4");
    cell1.addElement(documents);
    row2.addElement(cell1);
    table.addElement(row2);
    // row 3: page numbers dropdown
    var row3= new Element("tr");
    cell1 = new Element("td");
    cell1.addText("Page: ");
    var pages = new Element("select");
    pages.addAttribute("id","pages");
    cell1.addElement(pages);
    row3.addElement(cell1);
    table.addElement(row3);
    // row4: dismiss button
    var row4 = new Element("tr");
    cell1= new Element("td");
    var okButton= new Element("input");
    okButton.addAttribute("type","button");
    okButton.addAttribute("id","dismiss-button");
    okButton.addAttribute("value","open");
    cell1.addElement(okButton);
    row4.addElement(cell1);
    table.addElement(row4);
    inner.addElement(table);
    this.div.addElement(inner);
    /**
     * Convert the entire overlay to a string of HTML
     * @return a HTMLstring being the overlay contents
     */
    this.toString = function() {
        return this.div.toString();
    };
}
function Progress()
{
    this.div = new Element("div");
    this.div.addAttribute("id","progress");
    var message = new Element("p");
    message.addAttribute("id","progress_message");
    var progress_bar = new Element("div");
    progress_bar.addAttribute("id","progress_bar");
    var slider = new Element("div");
    slider.addAttribute("id","szlider");
    var sliderbar = new Element("div");
    sliderbar.addAttribute("id","szliderbar");
    var szazalek = new Element("div");
    szazalek.addAttribute,("id","szazalek");
    slider.addElement(sliderbar);
    slider.addElement(szazalek);
    progress_bar.addElement(message);
    progress_bar.addElement(slider);
    this.div.addElement(progress_bar);
    /**
     * Convert the entire overlay to a string of HTML
     * @return a HTMLstring being the overlay contents
     */
    this.toString = function() {
        return this.div.toString();
    };
}
/**
 * Here we handle the container resizing and toolbar events.
 * @param docid initial doument identifier or "null"
 * @param initial page identifier or "null"
 */
function Tilt(docid,pageid) {
    var self = this;
	/** the canvas object */
	this.canvas = undefined;
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
            // scale canvas also
            $("#tilt")[0].width = awt;
            $("#tilt")[0].height = aht
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
            canvas = $("#tilt");
            canvas.css("top",top+"px");
            canvas.css("left",left+"px");
        }
    };
    /**
     * Safely decrement the zoom level number
     * @param zoomLevel the current level
     * @return the new level
     */
    this.decLevel = function( zoomLevel ) {
        if ( zoomLevel == -1 )
            return 0;
        else
            return zoomLevel-1;
    };
    /**
     * Turn this zoom level name into a zoom level number
     * @param str
     * @return the elvel number
     */
    this.zoomLevelToNumber = function( str ) {
        if ( str == undefined || str == "standard" )
            return -1;
        else
            return parseInt(str.split("-")[1]);
    };
    /**
     * Work out the current zoom level name from its numerical level
     * @param number the numerical zoom level
     * @return the level name
     */
    this.zoomNumberToLevel = function( number ) {
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
            if ( data.length==0 )
                console.log("failed to load pages data");
            else
            {
                var pages = $("#pages");
                pages.empty();
                // temporary hack - nly p18 is usable
                if ( doc == "Journals187119100Brew"&&data.length>17 )
                    pages.append("<option value=\""+data[17].id+"\">"+data[17].n+"</option>");
                else
                {
                    for ( var i=0;i<data.length;i++ )
                    {
                        pages.append("<option value=\""+data[i].id+"\">"+data[i].n+"</option>");
                    }
                }
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
     * @param docId the document identifier
     * @param pageId theid of the page-image
     */
    this.loadPage = function( docId, pageId ) {
        if ( docId != "null" )
        {
            var url = $("#host-record").val()+"/html?docid="
                +docId+"&pageid="+pageId;
            $.get(url,function(data){
                $("#flow").empty();
                $("#flow").append(data);
            }).fail(function(){
                $("#flow").empty();
                $("#flow").append("<p>Failed to load data</p>");
            });
        }
        else
        {
            $("#flow").empty();
            $("#flow").append("<p id=\"warning\">Use <b>Open...</b> to choose a document and page</p>");
        }
        if ( pageId!="null" )
        {
            // get image
            url = $("#host-record").val()+"/image?docid="+docId+"&pageid="+pageId
            $.get(url,function(data){
                var img = $("#image img");
                img.attr("src",data.trim());
                img.load(function(){
                    self.scaleImageAndText();
                });
            }).fail(function(){
                var img = $("#image img");
                img.attr("src","/tilt/static/images/default.jpg");
                img.load(function(){
                    self.scaleImageAndText();
                });
            });
        }
        else
        {
            var img = $("#image img");
            img.attr("src","/tilt/static/images/default.jpg");
            img.load(function(){
                self.scaleImageAndText();
            });
        }
    };
    /**
     * Update the next and previous button's color
     */
    this.checkNextAndPrev = function() {
        var pagesOptions = $("#pages option");
        var nextLimit = pagesOptions.length;
        var next = $("#next-button");
        var prev = $("#prev-button");
        var pages = $("#pages");
        if ( pagesOptions.length<=1 || pages[0].selectedIndex+1 == nextLimit )
            next.css("color","lightgrey");
        else
            next.css("color","black");
        if ( pagesOptions.length<=1 || pages[0].selectedIndex == 0 )
            prev.css("color","lightgrey");
        else
            prev.css("color","black");
    };
    /**
     * Remove trailing "px" from a css value
     * @param value a raw css value with "px" on the end
     * @return a pure number
     */
    this.pixelLen = function( value ) {
        var param = value.replace("px","");
        return parseInt(param);
    };
    /**
     * Scale image and text block to match
     */
    this.scaleImageAndText = function(){
        var iht = $("#image > img").height();
        var iwt = $("#image > img").width();
        var wht = $(window).height();
        //console.log("wht="+wht+" iht="+iht+" iwt="+iwt);
        // adjust image width and height to window height
        var awt = Math.round((iwt*wht)/iht);
        var aht = wht;
        //console.log("aht="+aht);
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
        var marginTop = this.pixelLen($("#flow").css("margin-top"));
        var padTop = this.pixelLen($("#flow").css("padding-top"));
        $("#image").height(aht);
        $("#image").width(awt);
        $("#tilt")[0].height = aht;
        $("#tilt")[0].width = awt;
        $("#flow").width(awt);
        $("#text").height(aht);
        var diff=$("#image").height()-$("#image img").height();
        aht -= (diff+marginTop+padTop);
        $("#flow").height(aht);
    };
    /**
     * Get and set the geojson for this document
     * @param docid the document identifier
     * @param pageid the page identifier
     */
    this.getGeoJson = function( docid, pageid ) {
        var url = "http://"+window.location.hostname
            +"/tilt/geojson?docid="+docid+"&pageid="+pageid;
        $.get(url,function(data){
            $("#geojson").val(data);
			self.canvas = new Canvas("tilt");
            self.canvas.reload($("#geojson").val());
        }).fail(function(){
            console.log("Failed to load geojson");
        });
    };
    /**
     * Generate a segment of a uuid
     */
    this.s4 = function() {
        return Math.floor((1 + Math.random()) * 0x10000)
          .toString(16).substring(1);
    };
    /**
     * Create a uuid separator for a multipart post request
     * @return a suitable boundary separator
     */
    this.createBoundary = function() {
        return this.s4() + this.s4() + '-' + this.s4() + '-' + this.s4() + '-' +
            this.s4() + '-' + this.s4() + this.s4() + this.s4();
    };
    /**
     * Turn a plain javascript object into a postable data block
     * @param obj an object with no recursive structures
     * @param boundary the boundary separator between segments
     */
    this.composePostData = function( obj, boundary ) {
        postData ="";
        for (var key in obj) 
        {
            postData += "--"+boundary+"\r\n";
            postData += "content-type: text/plain; charset=\"utf-8\"\r\n";
            postData += "content-disposition: form-data; name=\""
                +key+"\"\r\n\r\n";
            postData += obj[key];
            postData += "\r\n";
        }
        postData += "--"+boundary+"--\r\n";
        return postData;
    };
    /** 
     * Update the progress bar and its message
     * @param client the XHttpClient 
     * @param readSoFar number of chars read so far
     */
    this.updateProgress = function( client, readSoFar ) {
        var len = client.responseText.length-readSoFar;
        var num = client.responseText.substr(readSoFar,len);
        if (num.length <= 0 )
            num = client.responseText;
        var numbers = num.split("\n");
        for ( var i=0;i<numbers.length;i++ )
        {
            if ( numbers[i].length > 0 )
            {
                var parts = numbers[i].split(" ");
                if ( parts != undefined && parts.length>0 )
                {
                    var val = parseInt(parts[0]);
                    var rest = parts.slice(1);
                    var text_message = rest.join(" ");
                    $("#progress_message").text(text_message);
                    var percent = Math.round((val*100)/100);
                    jQuery("#szliderbar").css("width",percent+"%");
                }
            }
        }
        return client.responseText.length;
    };
    /**
     * Toggle between honouring line-breaks and ignoring them
     */
    $("#justify-button").click(function()
    {
        var span = $("#justify-button span");
        var classtr = span.attr("class");
        var classNames = classtr.split(" ");
        if ( classNames[classNames.length-1]=="fa-justify" )
        {
            $("br").css("display","none");
            $(this).attr("title","line-breaks");
            span.attr("class","fa fa-2x fa-linebreaks");
            $("#flow").css("white-space","normal");
            $("span.soft").css("display","none");
            $("#flow p").css("margin-bottom","1em");
        }
        else
        {
            $("br").css("display","inline");
            $(this).attr("title","justify");
            span.attr("class","fa fa-2x fa-justify");
            $("#flow").css("white-space","pre");
            $("span.soft").css("display","inline");
            $("#flow p").css("margin-bottom","0px");
        }
    });
    /**
     * Change the cursor and react to clicks when magnifying or minifying
     */
    $("#magnify-button").click(function()
    {
        var container = $("#container");
        var icon = $("#magnify-button span");
        var cursor = container.attr("class");
        var level = self.decLevel(self.zoomLevelToNumber(cursor));
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
            {
                $("#image img").css({left:"0px",top:"0px"});
                $("#tilt").css({left:"0px",top:"0px"});
            }
            if ( self.polygon != undefined )
            {
                var ctx = $("#tilt")[0].getContext("2d");
                self.canvas.redraw(ctx);
            }
        }
    });
    $("#refresh").click(function(){
        self.refreshList();
    });
    /**
     * Scrolling list of documents containing pages
     */
    $("#documents").change(function(){
        self.refreshPages();
    });
    /**
     * Make invisible open dialog visible
     */
    $("#open-button").click(function()
    {
        var overlay = $("#overlay");
        //self.refreshList();
        var visibility = overlay.css("visibility","visible");
        overlay.css("z-index","20");
    });
    /**
     * This controls the OK button on the modal dialog
     */
    $("#dismiss-button").click(function()
    {
        var overlay = $("#overlay");
        var doc = $("#documents").val();
        self.loadPage(doc,$("#pages").val());
        self.checkNextAndPrev();
        overlay.css("visibility","hidden");
        overlay.css("z-index","-10");
    });
    /**
     * Send the current geojson and text to server for recognition
     */
    $("#recognise-button").click(function() {
        // json in text form
        var json = $("#geojson").val();
        var text = $("#flow").html();
        var obj = {geojson: json, text:text, docid:$("#documents").val(),
            pageid:$("#pages").val()};
        var readSoFar=0;
        var client = new XMLHttpRequest();
        client.open("POST", "http://"+window.location.hostname+"/tilt/recognise/");
        var boundary = self.createBoundary();
        client.setRequestHeader("Content-type", "multipart/form-data; boundary="+boundary);
        var postData = self.composePostData( obj, boundary );
        client.send(postData);
        var progress = $("#progress");
        //self.refreshList();
        var visibility = progress.css("visibility","visible");
        progress.css("z-index","20");
        client.onreadystatechange = function(){
            // readyState 4 means that the flow has ended
            if( client.readyState == 4 )
            {
                if ( client.status >= 300 )
                {
                    console.log("Error:"+client.status);
                    $("#progress").css("visibility","hidden");
                    $("#progress").css("z-index","-11");    
                }
                else
                {
                    readSoFar = self.updateProgress(client,readSoFar);
                    setTimeout(function(){
                        $("#progress").css("visibility","hidden");
                        $("#progress").css("z-index","-11");
                        self.getGeoJson($("#documents").val(),$("#pages").val());
                    }, 1000);
                }
                
            }
            // readyState 3 means that data is ready
            else if (client.readyState == 3) 
            {
                if (client.status ==200) 
                {
                    readSoFar = self.updateProgress(client,readSoFar);
                }
                else if ( client.status >= 300 )
                {
                   console.log("Error:"+client.status);
                }
            }
        };
    });
    /**
     * Save geojson description to database
     */
    $("#save-button").click(function() {
    });
    $("#next-button").click(function(event){
        var pageNo = $("#pages")[0].selectedIndex+1;
        var limit = $("#pages option").length;
        if ( pageNo < limit )
        {
            $("#pages")[0].selectedIndex++;
            var pageId = $("#pages").val();
            var docId = $("#documents").val();
            self.loadPage( docId, pageId );
            self.checkNextAndPrev();
        }
    });
    $("#prev-button").click(function(event){
        var pageNo = $("#pages")[0].selectedIndex-1;
        var limit = $("#pages option").length;
        if ( pageNo > 1 )
        {
            $("#pages")[0].selectedIndex--;
            var pageId = $("#pages").val();
            var docId = $("#documents").val();
            self.loadPage( docId, pageId );
            self.checkNextAndPrev();
        }
    });
    $("#image").click(function(event){
        var container = $("#container");
        var cursor = container.attr("class");
        var img = $("#image");
        var icon = $("#magnify-button");
        var level = self.zoomLevelToNumber(cursor);
        if ( level == 3 )
        {
            container.attr("class",self.zoomNumberToLevel(level-1));
            self.setImageScale( 3 );
            self.centreAround( event.pageX-img.offset().left,
                event.pageY-img.offset().top, 3, 4 );
        }
        else if ( level >= 0 )
        {
            container.attr("class",self.zoomNumberToLevel(level+1));
            self.setImageScale( level+2 );
            self.centreAround( event.pageX-img.offset().left,
                event.pageY-img.offset().top, level+2, level+1 );
        }
    });
    $("#host-record").val("http://"+window.location.hostname+"/pages");
    this.getGeoJson( docid, pageid );
    this.loadPage( docid, pageid );
    this.refreshList();
    this.checkNextAndPrev();
}
/**
 * This reads the "arguments" to the javascript file
 * @param scrName the name of the script file minus ".js"
 */
function getArgs( scrName )
{
    var scripts = jQuery("script");
    var params = new Object ();
    scripts.each( function(i) {
        var src = jQuery(this).attr("src");
        if ( src != undefined && src.indexOf(scrName) != -1 )
        {
            var qStr = src.replace(/^[^\?]+\??/,'');
            if ( qStr )
            {
                var pairs = qStr.split(/[;&]/);
                for ( var i = 0; i < pairs.length; i++ )
                {
                    var keyVal = pairs[i].split('=');
                    if ( ! keyVal || keyVal.length != 2 )
                        continue;
                    var key = unescape( keyVal[0] );
                    var val = unescape( keyVal[1] );
                    val = val.replace(/\+/g, ' ');
                    params[key] = val;
                }
            }
            return params;
        }
    });
    return params;
}
/**
 * Actually build the page
 */
$(document).ready(function(){
    var params = getArgs('tilt');
    var target = params['target'];
    if ( target == undefined )
        target = "content";
    var content = $("#"+target);
    if ( content != undefined )
    {
        content.append( new Container().toString() );
        content.append( new Overlay().toString() );
        content.append( new Progress().toString() );
        content.append('<textarea id="geojson"></textarea>');
        $("#pages").empty();
        $("#documents").empty();
        var tilt = new Tilt(params['docid'],params['pageid']);
    }
    else
        console.log("tilt.js: couldn't find content element");
});
