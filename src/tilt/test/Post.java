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
    "function getUrl(geojson)\n{\nvar obj = JSON.parse(geojson);\n"
    +"if ( obj && obj.properties.url )\n\treturn obj.properties.ur"
    +"l;\nelse\n\treturn \"http://setis.library.usyd.edu.au/ozedit"
    +"s/harpur/A87-1/00000005.jpg\";\n}\nfunction escapeUrl( url )"
    +"\n{\nvar newUrl = \"\";\nfor ( var i=0;i<url.length;i++ )\n{"
    +"\n\tvar token = url.charAt(i);\n\tswitch ( token )\n\t{\n\tc"
    +"ase '/':\n\t\tnewUrl += \"%2F\";\n\t\tbreak;\n\tcase ' ':\n\t"
    +"\tnewUrl += '%20';\n\t\tbreak;\n\tcase ':':\n\t\tnewUrl += "
    +"'%3A';\n\t\tbreak;\n\tdefault:\n\t\tnewUrl += url.charAt(i);"
    +"\n\t\tbreak;\n\t}\n}\nreturn newUrl;\n}\nfunction getUrlFrom"
    +"Loc( pictype )\n{\n\tvar gjurl = getUrl($(\"#geojson\").val("
    +"));\n\tvar url = \"/tilt/image/?docid=\"+escapeUrl(gjurl)+\""
    +"&pictype=\"+pictype;\n\treturn url;\n}\nfunction getGeoJsonU"
    +"rl()\n{\n\tvar gjurl = getUrl($(\"#geojson\").val());\n\tvar"
    +" url = \"/tilt/geojson/?docid=\"+escapeUrl(gjurl);\n\treturn"
    +" url;\n}\nfunction enableAll()\n{\n\t$(\"#original\").prop('"
    +"disabled', false);\n\t$(\"#greyscale\").prop('disabled', fal"
    +"se);\n\t$(\"#twotone\").prop('disabled', false);\n\t$(\"#cle"
    +"aned\").prop('disabled', false);\n\t$(\"#baselines\").prop('"
    +"disabled', false);\n\t$(\"#words\").prop('disabled', false);"
    +" \n\t$(\"#link\").prop('disabled', false); \n}\nfunction dis"
    +"ableAll()\n{\n\t$(\"#original\").prop('disabled', true);\n\t"
    +"$(\"#greyscale\").prop('disabled', true);\n\t$(\"#twotone\")"
    +".prop('disabled', true);\n\t$(\"#cleaned\").prop('disabled',"
    +" true);\n\t$(\"#baselines\").prop('disabled', true);\n\t$(\""
    +"#words\").prop('disabled', true);\n\t$(\"#link\").prop('disa"
    +"bled', true); \n}\n$(document).ready(function() {\ndisableAl"
    +"l();\n$(\"#upload\").click(function(e){\n\tvar localurl = \""
    +"http://\"+location.hostname+\"/tilt/\";\n\t$.post( localurl,"
    +" $(\"#main\").serialize(), function( data ) {\n\t$(\"#left\""
    +").empty();\n\t$(\"#left\").html(data);\n\tenableAll();\n\t},"
    +"\"text\").fail(function(xhr, status, error){\n\talert(status"
    +");\n\tdisableAll();\n\t});\n});\n$(\"#original\").click(func"
    +"tion(){\n\t$(\"#left\").empty();\n\t$(\"#left\").html('<img "
    +"width=\"500\" src=\"'+getUrlFromLoc(\"original\")+'\">');\n}"
    +");\n$(\"#greyscale\").click(function(){\n\t$(\"#left\").empt"
    +"y();\n\t$(\"#left\").html('<img width=\"500\" src=\"'+getUrl"
    +"FromLoc(\"greyscale\")+'\">');\n});\n$(\"#twotone\").click(f"
    +"unction(){\n\tvar url = escapeUrl(getUrlFromLoc(\"twotone\")"
    +");\n\t$(\"#left\").empty();\n\t$(\"#left\").html('<img width"
    +"=\"500\" src=\"'+getUrlFromLoc(\"twotone\")+'\">');\n});\n$("
    +"\"#cleaned\").click(function(){\n\tvar url = escapeUrl(getUr"
    +"lFromLoc(\"cleaned\"));\n\t$(\"#left\").empty();\n\t$(\"#lef"
    +"t\").html('<img width=\"500\" src=\"'+getUrlFromLoc(\"cleane"
    +"d\")+'\">');\n});\n$(\"#baselines\").click(function(){\n\tva"
    +"r url = escapeUrl(getUrlFromLoc(\"baselines\"));\n\t$(\"#lef"
    +"t\").empty();\n\t$(\"#left\").html('<img width=\"500\" src=\""
    +"'+getUrlFromLoc(\"baselines\")+'\">');\n});\n$(\"#words\")."
    +"click(function(){\n\tvar url = escapeUrl(getUrlFromLoc(\"wor"
    +"ds\"));\n\t$(\"#left\").empty();\n\t$(\"#left\").html('<img "
    +"width=\"500\" src=\"'+getUrlFromLoc(\"words\")+'\">');\n});\n"
    +"$(\"#link\").click(function(){\n\tvar url = escapeUrl(getUr"
    +"lFromLoc(\"link\"));\n\t$(\"#left\").empty();\n\t$(\"#left\""
    +").html('<img width=\"500\" src=\"'+getUrlFromLoc(\"link\")+'"
    +"\">');\n\tvar gjurl = getGeoJsonUrl();\n\t$.get( gjurl, func"
    +"tion( data ) {\n\talert( data );\n\t});\n});\n$(\"#selection"
    +"s\").change(function(e){\n\tvar base_id = $(\"#selections\")"
    +".val();\n\tvar thtml = $(\"#\"+base_id+\"_TEXT\").html();\n\t"
    +"$(\"#geojson\").val($(\"#\"+base_id+\"_JSON\").text());\n\t"
    +"$(\"#content\").html(thtml);\n\t$(\"#text\").val(thtml);\n\t"
    +"disableAll();\n});\n$(\"#content\").width($(\"#geojson\").wi"
    +"dth());\nvar htext = $(\"#HARPUR_JSON\").text();\n$(\"#geojs"
    +"on\").val(htext);\nvar hhtml = $(\"#HARPUR_TEXT\").html();\n"
    +"$(\"#content\").html(hhtml);\n$(\"#text\").val(hhtml);\n$(\""
    +"#selections\").val(\"HARPUR\");\n});\n";
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
