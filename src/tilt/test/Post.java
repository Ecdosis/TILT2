/*
 * This file is part of TILT.
 *
 *  TILT is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  TILT is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TILT.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2014
 */

package tilt.test;

import tilt.test.html.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tilt.exception.TiltException;
import java.util.HashMap;

/**
 * Test the POST command
 * @author desmond
 */
public class Post extends Test
{
    static String POST_JS = 
    "function getUrl(geojson)\n{\n\tvar obj = JSON.parse(geojson);"
    +"\n\tif ( obj && obj.properties.url )\n\t return obj.properti"
    +"es.url;\n\telse\n\t return \"http://setis.library.usyd.edu.a"
    +"u/ozedits/harpur/A87-1/00000005.jpg\";\n}\nfunction escapeUr"
    +"l( url )\n{\n\tvar newUrl = \"\";\n\tfor ( var i=0;i<url.len"
    +"gth;i++ )\n\t{\n\t var token = url.charAt(i);\n\t switch ( t"
    +"oken )\n\t {\n\t case '/':\n\t\t newUrl += \"%2F\";\n\t\t br"
    +"eak;\n\t case ' ':\n\t\t newUrl += '%20';\n\t\t break;\n\t c"
    +"ase ':':\n\t\t newUrl += '%3A';\n\t\t break;\n\t default:\n\t"
    +"\t newUrl += url.charAt(i);\n\t\t break;\n\t }\n\t}\n\tretu"
    +"rn newUrl;\n}\nfunction getUrlFromLoc( pictype )\n{\n\tvar g"
    +"jurl = getUrl($(\"#geojson\").val());\n\tvar url = \"/tilt/i"
    +"mage/?docid=\"+escapeUrl(gjurl)+\"&pictype=\"+pictype;\n\tre"
    +"turn url;\n}\nfunction getGeoJsonUrl()\n{\n\tvar gjurl = get"
    +"Url($(\"#geojson\").val());\n\tvar url = \"/tilt/geojson/?do"
    +"cid=\"+escapeUrl(gjurl);\n\treturn url;\n}\nfunction enableA"
    +"ll()\n{\n\t$(\"#original\").prop('disabled', false);\n\t$(\""
    +"#greyscale\").prop('disabled', false);\n\t$(\"#twotone\").pr"
    +"op('disabled', false);\n\t$(\"#cleaned\").prop('disabled', f"
    +"alse);\n\t$(\"#baselines\").prop('disabled', false);\n\t$(\""
    +"#words\").prop('disabled', false); \n\t$(\"#link\").prop('di"
    +"sabled', false); \n}\nfunction disableAll()\n{\n\t$(\"#origi"
    +"nal\").prop('disabled', true);\n\t$(\"#greyscale\").prop('di"
    +"sabled', true);\n\t$(\"#twotone\").prop('disabled', true);\n"
    +"\t$(\"#cleaned\").prop('disabled', true);\n\t$(\"#baselines\""
    +").prop('disabled', true);\n\t$(\"#words\").prop('disabled',"
    +" true);\n\t$(\"#link\").prop('disabled', true); \n}\nfunctio"
    +"n Point( x, y )\n{\n\tthis.x = x;\n\tthis.y = y;\n}\nfunctio"
    +"n scalePoints( coords )\n{\n\tvar canvas = $(\"#left canvas\""
    +");\n\tvar ht = canvas.height();\n\tvar wt = canvas.width();"
    +"\n\tvar points = new Array();\n\tfor ( var i=0;i<coords.leng"
    +"th;i++ )\n\t{\n\t\tpoints[points.length] = new Point(\n\t\t\t"
    +"Math.round(coords[i][0]*wt),\n\t\t\tMath.round(coords[i][1]*ht)\n"
    +"\t\t);\n\t}\n\treturn points;\n}\nfunction Bounds( x, y, wid"
    +"th, height )\n{\n\tthis.x = x;\n\tthis.y = y;\n\tthis.width "
    +"= width;\n\tthis.height = height;\n}\nfunction Polygon( coor"
    +"ds, props )\n{\n\tif ( props != undefined )\n\t{\n\t\tif ( p"
    +"rops.offset != undefined )\n\t\tthis.offset = props.offset;\n"
    +"\t\tif ( props.hyphen != undefined )\n\t\t\tthis.hyphen = h"
    +"yphen;\n\t}\n\tthis.points = scalePoints(coords);\n\tvar str"
    +" = \"\";\n\tvar x=Number.MAX_VALUE,y=Number.MAX_VALUE;\n\tva"
    +"r bot=0,right=0;\n\tfor ( var i=0;i<points.length;i++ )\n\t{"
    +"\n\t\tif ( points[i].x < x )\n\t\t\tx = points[i].x;\n\t\tif"
    +" ( points[i].y < y )\n\t\t\ty = points[i].y;\n\t\tif ( point"
    +"s[i].x > right )\n\t\t\tright = points[i].x;\n\t\tif ( point"
    +"s[i].y > bot )\n\t\t\tbot = points[i].y\n\t\tstr += points[i"
    +"].x+\" \"+points[i].y+\" \";\n\t}\n\tthis.bounds = new Bound"
    +"s( x, y, right-x, bot-y );\n\talert(str);\n}\nfunction QuadT"
    +"ree( json )\n{\n\tvar geoJson = JSON.parse( json );\n\tfor ("
    +" var i=0;i<geoJson.features.length;i++ )\n\t{\n\t\tvar line "
    +"= geoJson.features[i];\n\t\tfor ( var j=0;j<line.features.le"
    +"ngth;j++ )\n\t\t{\n\t\t\tvar poly = line.features[j];\n\t\t\t"
    +"if ( poly.type==\"Feature\" )\n\t\t\t{\n\t\t\t\tvar geometr"
    +"y = poly.geometry;\n\t\t\t\tif ( geometry.type=\"Polygon\" )"
    +"\n\t\t\t\t{\n\t\t\t\t\tvar p = Polygon(geometry.coordinates,"
    +"\n\t\t\t\t\t\tpoly.properties);\n\t\t\t\t\t\t\t\t}\n\t\t\t}\n"
    +"\t\t}\n\t}\n}\nfunction bindJsonToImage( json )\n{\n\tvar j"
    +"qImg = $(\"#left img\");\n\tvar ht = jqImg.height();\n\tvar "
    +"wt = jqImg.width();\n\tvar cstr = '<canvas width=\"'+wt+'\" "
    +"height=\"'+ht+'\"></canvas>';\n\tvar src = jqImg.attr(\"src\""
    +");\n\tjqImg.replaceWith(cstr);\n\tvar canvas = $(\"#left ca"
    +"nvas\");\n\tvar ctx = canvas.get(0).getContext('2d');\n\tvar"
    +" img = new Image;\n\timg.src = src;\n\tctx.drawImage( img, 0"
    +", 0, wt, ht );\n\tquadTree = QuadTree(json);\n}\n$(document)"
    +".ready(function() {\ndisableAll();\n$(\"#upload\").click(fun"
    +"ction(e){\n\tvar localurl = \"http://\"+location.hostname+\""
    +"/tilt/\";\n\t$.post( localurl, $(\"#main\").serialize(), fun"
    +"ction( data ) {\n\t$(\"#left\").empty();\n\t$(\"#left\").htm"
    +"l(data);\n\tenableAll();\n\t},\"text\").fail(function(xhr, s"
    +"tatus, error){\n\talert(status);\n\tdisableAll();\n\t});\n})"
    +";\n$(\"#original\").click(function(){\n\t$(\"#left\").empty("
    +");\n\t$(\"#left\").html('<img width=\"500\" src=\"'+getUrlFr"
    +"omLoc(\"original\")+'\">');\n});\n$(\"#greyscale\").click(fu"
    +"nction(){\n\t$(\"#left\").empty();\n\t$(\"#left\").html('<im"
    +"g width=\"500\" src=\"'+getUrlFromLoc(\"greyscale\")+'\">');"
    +"\n});\n$(\"#twotone\").click(function(){\n\tvar url = escape"
    +"Url(getUrlFromLoc(\"twotone\"));\n\t$(\"#left\").empty();\n\t"
    +"$(\"#left\").html('<img width=\"500\" src=\"'+getUrlFromLoc"
    +"(\"twotone\")+'\">');\n});\n$(\"#cleaned\").click(function()"
    +"{\n\tvar url = escapeUrl(getUrlFromLoc(\"cleaned\"));\n\t$(\""
    +"#left\").empty();\n\t$(\"#left\").html('<img width=\"500\" "
    +"src=\"'+getUrlFromLoc(\"cleaned\")+'\">');\n});\n$(\"#baseli"
    +"nes\").click(function(){\n\tvar url = escapeUrl(getUrlFromLo"
    +"c(\"baselines\"));\n\t$(\"#left\").empty();\n\t$(\"#left\")."
    +"html('<img width=\"500\" src=\"'+getUrlFromLoc(\"baselines\""
    +")+'\">');\n});\n$(\"#words\").click(function(){\n\tvar url ="
    +" escapeUrl(getUrlFromLoc(\"words\"));\n\t$(\"#left\").empty("
    +");\n\t$(\"#left\").html('<img width=\"500\" src=\"'+getUrlFr"
    +"omLoc(\"words\")+'\">');\n});\n$(\"#link\").click(function()"
    +"{\n\tvar url = escapeUrl(getUrlFromLoc(\"link\"));\n\t$(\"#l"
    +"eft\").empty();\n\t$(\"#left\").html('<img width=\"500\" src"
    +"=\"'+getUrlFromLoc(\"link\")+'\">');\n\tvar gjurl = getGeoJs"
    +"onUrl();\n\t$.get( gjurl, function( data ) {\n\tbindJsonToIm"
    +"age(data);\n\t});\n});\n$(\"#selections\").change(function(e"
    +"){\n\tvar base_id = $(\"#selections\").val();\n\tvar thtml ="
    +" $(\"#\"+base_id+\"_TEXT\").html();\n\t$(\"#geojson\").val($"
    +"(\"#\"+base_id+\"_JSON\").text());\n\t$(\"#content\").html(t"
    +"html);\n\t$(\"#text\").val(thtml);\n\tdisableAll();\n});\n$("
    +"\"#content\").width($(\"#geojson\").width());\nvar htext = $"
    +"(\"#HARPUR_JSON\").text();\n$(\"#geojson\").val(htext);\nvar"
    +" hhtml = $(\"#HARPUR_TEXT\").html();\n$(\"#content\").html(h"
    +"html);\n$(\"#text\").val(hhtml);\n$(\"#selections\").val(\"H"
    +"ARPUR\");\n});\n";
    static String POST_CSS =
    "#left {float:left;margin:10px}\n"
    +"#right {float:left;margin:10px}\n"
    +"#toolbar {clear:both}\n"
    +"#content { width: 500px }\n"
    +"#content table tr td { font-size:small}\n"
    +"#content p { font-size:small}\n"
    +"#content table tr td ul { margin: 0 }\n"
    +".hidden { display: none}\n"
    + "textarea { display: none }\n"
    + "#title { font-weight: bold}";
    /**
     * Create a hidden div (of class "hidden")
     * @param id the id of the div
     * @param contents its contents
     * @return the hidden element
     */
    Element hiddenDiv( String id, String contents )
    {
        Element div = new Element("div");
        div.addAttribute("class", "hidden" );
        div.addAttribute("id", id );
        div.addText( contents );
        return div;
    }
    /**
     * Display the test GUI
     * @param request the request to read from
     * @param urn the original URN
     */
    @Override
    public void handle( HttpServletRequest request,
        HttpServletResponse response, String urn ) throws TiltException
    {
        try
        {
            // create the doc and install the scripts etc
            doc = new HTML();
            doc.getHead().addEncoding("text/html; charset=UTF-8");
            doc.getHead().addJQuery("1.9.0");
            doc.getHead().addScript( POST_JS );
            doc.getHead().addCss(POST_CSS);
            HashMap<String,String> selections = new HashMap<>();
            for ( int i=0;i<Texts.samples.length;i++ )
            {
                String id = Texts.samples[i][0];
                String name = Texts.samples[i][1];
                String text = Texts.samples[i][2];
                String json = Texts.samples[i][3];
                selections.put(name,id);
                doc.addElement( hiddenDiv(id+"_TEXT",text) );
                doc.addElement( hiddenDiv(id+"_JSON",json) );
            }
            Form f = new Form( "POST", "http://"+request.getServerName()
                +"/tilt/test","multipart/form-data" );
            f.addAttribute("id","main");
            Select s = new Select( selections, "selections", 
                Texts.samples[0][1] );
            Element p = new Element("p");
            Element span = new Element("span");
            span.addAttribute("id","title");
            span.addText("TILT upload test: ");
            p.addElement(span);
            p.addElement(s);
            f.addElement( p );
            Element textarea = new Element("textarea");
            textarea.addAttribute("name","geojson");
            textarea.addAttribute("id","geojson");
            textarea.addAttribute("rows","8");
            textarea.addAttribute("cols","80");
            textarea.addText(Texts.samples[0][2]);
            f.addElement( textarea );
            p = new Element("p");
            p.addElement( new Input("upload","button","upload",true) );
            p.addElement( new Input("original","button","original",false) );
            p.addElement( new Input("greyscale","button","greyscale",false) );
            p.addElement( new Input("twotone","button","two tone",false) );
            p.addElement( new Input("cleaned","button","cleaned",false) );
            p.addElement( new Input("baselines","button","baselines",false) );
            p.addElement( new Input("words","button","words",false) );
            p.addElement( new Input("link","button","link",false) );
            f.addElement( p );
            Element textHidden = new Element("input");
            textHidden.addAttribute("type","hidden");
            textHidden.addAttribute("id","text");
            textHidden.addAttribute("name","text");
            f.addElement( textHidden );
            doc.addElement( f );
            // now add the two sides
            Element lhs = new Element("div");
            lhs.addAttribute("id","left");
            doc.addElement( lhs );
            Element rhs = new Element("div");
            rhs.addAttribute("id","right");
            Element contentDiv = new Element("div");
            contentDiv.addAttribute("id","content");
            rhs.addElement( contentDiv );
            doc.addElement( rhs );
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println(doc);
        }
        catch ( Exception e )
        {
            throw new TiltException(e);
        }
    }
}
