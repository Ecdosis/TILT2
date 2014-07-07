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
    +"\n\t\tbreak;\n\t}\n}\nreturn newUrl;\n}\nfunction unescapeQu"
    +"otes( input )\n{\n\tvar sb = \"\";\n\tfor ( var i=0;i<input."
    +"length;i++ )\n\t{\n\t\tvar token = input.charAt(i);\n\t\tif "
    +"( token=='*' )\n\t\t\tsb += '\"';\n\t\telse\n\t\t\tsb += tok"
    +"en;\n\t}\n\treturn sb;\n}\nfunction getUrlFromLoc( pictype )"
    +"\n{\n\tvar gjurl = getUrl($(\"#geojson\").val());\n\tvar url"
    +" = \"../tilt/?docid=\"+escapeUrl(gjurl)+\"&pictype=\"+pictyp"
    +"e;\n\treturn url;\n}\nfunction enableAll()\n{\n\t$(\"#origin"
    +"al\").prop('disabled', false);\n\t$(\"#greyscale\").prop('di"
    +"sabled', false);\n\t$(\"#twotone\").prop('disabled', false);"
    +"\n\t$(\"#cleaned\").prop('disabled', false);\n\t$(\"#baselin"
    +"es\").prop('disabled', false);\n\t$(\"#words\").prop('disabl"
    +"ed', false); \n}\nfunction disableAll()\n{\n\t$(\"#original\""
    +").prop('disabled', true);\n\t$(\"#greyscale\").prop('disabl"
    +"ed', true);\n\t$(\"#twotone\").prop('disabled', true);\n\t$("
    +"\"#cleaned\").prop('disabled', true);\n\t$(\"#baselines\").p"
    +"rop('disabled', true);\n\t$(\"#words\").prop('disabled', tru"
    +"e);\n}\n$(document).ready(function() {\ndisableAll();\n$(\"#"
    +"upload\").click(function(e){\n\tvar localurl = \"http://\"+l"
    +"ocation.hostname+\"/tilt/\";\n\t$.post( localurl, $(\"#main\""
    +").serialize(), function( data ) {\n\t$(\"#left\").empty();\n"
    +"\t$(\"#left\").html(data);\n\tenableAll();\n\t},\"text\").f"
    +"ail(function(xhr, status, error){\n\talert(status);\n\tdisab"
    +"leAll();\n\t});\n});\n$(\"#original\").click(function(){\n\t"
    +"$(\"#left\").empty();\n\t$(\"#left\").html('<img width=\"500"
    +"\" src=\"'+getUrlFromLoc(\"original\")+'\">');\n});\n$(\"#gr"
    +"eyscale\").click(function(){\n\t$(\"#left\").empty();\n\t$(\""
    +"#left\").html('<img width=\"500\" src=\"'+getUrlFromLoc(\"g"
    +"reyscale\")+'\">');\n});\n$(\"#twotone\").click(function(){\n"
    +"\tvar url = escapeUrl(getUrlFromLoc(\"twotone\"));\n\t$(\"#"
    +"left\").empty();\n\t$(\"#left\").html('<img width=\"500\" sr"
    +"c=\"'+getUrlFromLoc(\"twotone\")+'\">');\n});\n$(\"#cleaned\""
    +").click(function(){\n\tvar url = escapeUrl(getUrlFromLoc(\""
    +"cleaned\"));\n\t$(\"#left\").empty();\n\t$(\"#left\").html('"
    +"<img width=\"500\" src=\"'+getUrlFromLoc(\"cleaned\")+'\">')"
    +";\n});\n$(\"#baselines\").click(function(){\n\tvar url = esc"
    +"apeUrl(getUrlFromLoc(\"baselines\"));\n\t$(\"#left\").empty("
    +");\n\t$(\"#left\").html('<img width=\"500\" src=\"'+getUrlFr"
    +"omLoc(\"baselines\")+'\">');\n});\n$(\"#words\").click(funct"
    +"ion(){\n\tvar url = escapeUrl(getUrlFromLoc(\"words\"));\n\t"
    +"$(\"#left\").empty();\n\t$(\"#left\").html('<img width=\"500"
    +"\" src=\"'+getUrlFromLoc(\"words\")+'\">');\n});\n$(\"#selec"
    +"tions\").change(function(e){\n\tvar val = $(\"#selections\")"
    +".val();\n\t$(\"#geojson\").val(unescapeQuotes(val));\n\tdisa"
    +"bleAll();\n});\n});\n";
    static String DEFAULT_JSON = 
    "{\n\"type\": \"Feature\",\n\"geometry\": {\n\t\"type\": \"Pol"
    +"ygon\",\n\t\"coordinates\": [\n\t[ [0.0, 0.0], [100.0, 0.0],"
    +" [100.0, 100.0], [0.0, 100.0] ]\n\t]\n},\n\"properties\": {\n"
    +"\t\"url\": \"http://ecdosis.net/images//00000005.jpg\"\n}\n}\n";
    static String CAPUANA_JSON = 
    "{\n\"type\": \"Feature\",\n\"geometry\": {\n\t\"type\": \"Pol"
    +"ygon\",\n\t\"coordinates\": [\n\t[ [0.0, 0.0], [100.0, 0.0],"
    +" [100.0, 100.0], [0.0, 100.0] ]\n\t]\n},\n\"properties\": {\n"
    +"\t\"url\": \"http://ecdosis.net/images/frontispiece3.jpg\",\n"
    +"\"manuscript\": true}\n}\n";
    static String DEROBERTO_JSON = 
    "{\n\"type\": \"Feature\",\n\"geometry\": {\n\t\"type\": \"Pol"
    +"ygon\",\n\t\"coordinates\": [\n\t[ [0.0, 0.0], [100.0, 0.0],"
    +" [100.0, 100.0], [0.0, 100.0] ]\n\t]\n},\n\"properties\": {\n"
    +"\t\"url\": \"http://ecdosis.net/images/V2-69.jpg\",\n"
    +"\"manuscript\": true}\n}\n";
    static String DEROBERTO2_JSON = 
    "{\n\"type\": \"Feature\",\n\"geometry\": {\n\t\"type\": \"Pol"
    +"ygon\",\n\t\"coordinates\": [\n\t[ [0.0, 0.0], [100.0, 0.0],"
    +" [100.0, 100.0], [0.0, 100.0] ]\n\t]\n},\n\"properties\": {\n"
    +"\t\"url\": \"http://ecdosis.net/images/DR_1920_008.jpg\"\n}\n}\n";
    static String POST_CSS =
    "#left {float:left;margin:10px}\n" +
    "#right {float:left;margin:10px}\n" +
    "#toolbar {clear:both}";
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
            Element h1 = new Element("h1");
            h1.addText("TILT Upload test");
            doc.addElement( h1 );
            Form f = new Form( "POST", "http://"+request.getServerName()
                +"/tilt","multipart/form-data" );
            f.addAttribute("id","main");
            HashMap<String,String> selections = new HashMap<String,String>();
            selections.put("Harpur example",DEFAULT_JSON);
            selections.put("Capuana example",CAPUANA_JSON);
            selections.put("De Roberto V2",DEROBERTO_JSON);
            selections.put("De Roberto 1920 edition",DEROBERTO2_JSON);
            Select s = new Select( selections, "selections", "Harpur example" );
            Element p = new Element("p");
            p.addElement(s);
            f.addElement( p );
            Element div1 = new Element("div");
            div1.addAttribute("id","left");
            doc.addElement( div1 );
            Element div2 = new Element("div");
            div2.addAttribute("id","right");
            Element textarea = new Element("textarea");
            textarea.addAttribute("name","geojson");
            textarea.addAttribute("id","geojson");
            textarea.addAttribute("rows","12");
            textarea.addAttribute("cols","80");
            textarea.addText(DEFAULT_JSON);
            f.addElement( textarea );
            p = new Element("p");
            p.addElement( new Input("upload","button","upload",true) );
            p.addElement( new Input("original","button","original",false) );
            p.addElement( new Input("greyscale","button","greyscale",false) );
            p.addElement( new Input("twotone","button","two tone",false) );
            p.addElement( new Input("cleaned","button","cleaned",false) );
            p.addElement( new Input("baselines","button","baselines",false) );
            p.addElement( new Input("words","button","words",false) );
            f.addElement( p );
            doc.addElement( f );
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println(doc);
        }
        catch ( Exception e )
        {
            throw new TiltException(e);
        }
    }
}
