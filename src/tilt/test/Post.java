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

import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.constants.JSONKeys;
import html.*;
import org.json.simple.*;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tilt.exception.TiltException;
import java.util.HashMap;
import tilt.constants.Database;
import tilt.Utils;

/**
 * Test the POST command
 * @author desmond
 */
public class Post extends Test
{
    static int TEXT = 1;
    static int HTML = 2;
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
    * Guess the format of some text (plain text or HTML)
    * @param text the text to test
    * @return TextIndex.HTML if 1st non-space is &lt; else TextIndex.TEXT
    */
    private int guessFormat( String text )
    {
       int format = Post.TEXT;
       for ( int i=0;i<text.length();i++ )
       {
           char token = text.charAt(i);
           if ( !Character.isWhitespace(token) )
           {
               if ( token=='<' )
                   format = Post.HTML;
               else
                   format = Post.TEXT;
               break;
           }
       }
       return format;
    }
    String toHTML(String text)
    {
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer( text, "\n" );
        int lastParaStart = 0;
        while ( st.hasMoreTokens() )
        {
            String token = st.nextToken();
            if ( token.length()== 0 )
            {
                sb.insert(lastParaStart,"<p>");
                sb.append("</p>");
                lastParaStart = sb.length();
            }
            else
            {
                if ( sb.length()>0 )
                    sb.append("<br>");
                sb.append(token);
            }
        }
        if ( lastParaStart == 0 )
            sb.insert(0,"<p>");
        sb.append("</p>");
        return sb.toString();
    }
    /**
     * Add a hidden element that will get submitted to the server
     * @param form the form to add it to
     * @param name the parameter name
     * @param value its initial value
     */
    private void addHiddenElement( Element form, String name, String value )
    {
        Element textHidden = new Element("input");
        textHidden.addAttribute("type","hidden");
        textHidden.addAttribute("id", name);
        textHidden.addAttribute("name",name);
        textHidden.addAttribute("value",value);
        form.addElement( textHidden );  
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
            doc.getHead().addEncoding("UTF-8");
            doc.getHead().addScriptFile( "/tilt/static/js/jquery-1.11.1.js" );
            doc.getHead().addScriptFile( "/tilt/static/js/jquery.highlight.js" );
            doc.getHead().addScriptFile( "/tilt/static/js/post.js" );
            doc.getHead().addCssFile("/tilt/static/css/post.css");
            Connection conn = Connector.getConnection();
            String[] docids = conn.listCollection(Database.OPTIONS);
            HashMap<String,String> selections = new HashMap<>();
            String first = null;
            String defaultDocId = (docids.length>0)?docids[0]:"";
            String defaultPageId = "";
            for ( int i=0;i<docids.length;i++ )
            {
                String jstr = conn.getFromDb(Database.OPTIONS, docids[i]);
                JSONObject jobj = (JSONObject)JSONValue.parse(jstr);
                String title;
                if ( jobj.containsKey(JSONKeys.TITLE) )
                    title = (String)jobj.get(JSONKeys.TITLE);
                else
                    title = docids[i];  // default
                Number pageNum = (Number)jobj.get("test_page");
                String pageId = new Integer(pageNum.intValue()).toString();
                if ( first == null )
                {
                    defaultDocId = docids[i];
                    // we'll get a double with exponent notation otherwise
                    defaultPageId = pageId;
                    first = title;
                }
                selections.put(title, docids[i]+"#"+pageId);
            }
            Form f = new Form( "POST", "http://"+request.getServerName()
                +"/tilt/test/","multipart/form-data" );
            f.addAttribute("id","main");
            Select s = new Select( selections, "selections", first );
            Element p = new Element("p");
            Element span = new Element("span");
            span.addAttribute("id","title");
            span.addText("TILT upload test: ");
            p.addElement(span);
            p.addElement(s);
            f.addElement( p );
            p = new Element("p");
            p.addElement( new Input("preflight","button","preflight",false) );
            p.addElement( new Input("greyscale","button","greyscale",false) );
            p.addElement( new Input("twotone","button","two tone",false) );
            p.addElement( new Input("cleaned","button","cleaned",false) );
            p.addElement( new Input("reconstructed","button","reconstructed",false) );
            p.addElement( new Input("baselines","button","baselines",false) );
            p.addElement( new Input("words","button","words",false) );
            p.addElement( new Input("link","button","link",false) );
            f.addElement( p );
            addHiddenElement(f,"docid",defaultDocId);
            addHiddenElement(f,"pageid",defaultPageId);
            addHiddenElement(f,"text","");
            doc.addElement( f );
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