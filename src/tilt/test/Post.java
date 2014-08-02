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
    static String POST_CSS =
    "#left {float:left;margin:10px}\n"
    +"#right {float:left;margin:10px}\n"
    +"#toolbar {clear:both}\n"
    +"#content { width: 500px}\n"
    +"#content table tr td { font-size:small}\n"
    +"#content p { font-size:small}\n"
    +"#content table tr td ul { margin: 0 }\n"
    +".hidden { display: none}\n"
    +"textarea { display: none }\n"
    +"#title { font-weight: bold}\n"
    +"#container{ display:inline-block;\n"
    +"margin: 0 auto;\n"
    +"position: relative }\n"
    +"img{position:absolute;z-index:1}\n"
    +"canvas{position:relative;z-index:2}\n"
    +".highlight{background-color: #FFFF88}\n";
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
            doc.getHead().addScriptFile( "jquery.highlight.js" );
            doc.getHead().addScriptFile( "post.js" );
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
            Element container = new Element("div");
            container.addAttribute("id","container");
            lhs.addElement(container);
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