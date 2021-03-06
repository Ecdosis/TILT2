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
 *  (c) copyright Desmond Schmidt 2015
 */     

package tilt.handler.get;
import html.HTML;
import html.Element;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tilt.exception.TiltException;
import tilt.handler.TiltGetHandler;
import tilt.constants.Params;
import java.io.File;
import java.util.HashSet;
import html.Head;

/**
 * Editor GUI for Tilt GeoJSON documents
 * @author desmond
 */
public class TiltEditor extends TiltGetHandler 
{
    HTML doc;
    static HashSet<String> cssExceptions;
    static HashSet<String> jsExceptions;
    static
    {
        jsExceptions = new HashSet<String>();
        jsExceptions.add("jquery-1.11.1.js");
        jsExceptions.add("tilt.js");
        jsExceptions.add("post.js");
        cssExceptions = new HashSet<String>();
        cssExceptions.add("post.css");
    }
    /**
     * Add all the css or js files in the given directory to the header
     * @param suffix the file type suffix (minus the dot)
     */
    void addFilesInDir( String suffix, HashSet<String> exceptions )
    {
        Head h = doc.getHead();
        File parent1 = new File(System.getProperty("user.dir"));
        String path = TiltEditor.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File parent2 = new File(path).getParentFile();
        File cssDir = new File(parent1,"/static/"+suffix);
        if ( !cssDir.exists())
            cssDir = new File(parent2,"/static/"+suffix);
        if ( cssDir.exists() )
        {
            File[] css = cssDir.listFiles();
            for ( int i=0;i<css.length;i++ )
            {
                String name = css[i].getName();
                if ( name.endsWith("."+suffix) )
                {
                    if ( suffix.equals("css") && !exceptions.contains(name) )
                        h.addCssFile( "/tilt/static/"+suffix+"/"+name );
                    else if ( suffix.equals("js") && !exceptions.contains(name) )
                        h.addScriptFile( "/tilt/static/"+suffix+"/"+name );
                }
            }
        }
        else
            System.out.println("No "+suffix+" dir found");
    }
    /**
     * Build the document header
     */
    final void composeHeader()
    {
        Head h = doc.getHead();
        h.addEncoding("text/html; charset=UTF-8");
        h.addJQuery( "1.11.1", "/tilt/static/js", true );
        addFilesInDir( "css", cssExceptions );
        addFilesInDir( "js", jsExceptions );
    }
    /**
     * Tilt editor is a basic HTML document with an embedded script
    */
    public TiltEditor()
    {
        doc = new HTML();
        composeHeader();
    }
    /**
     * The request should have docid and pageid parameters set
     * @param request the http request
     * @param response the http response
     * @param urn the residual request urn probably empty
     * @throws TiltException 
     */
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws TiltException 
    {
        try
        {
            Element script = new Element("script");
            String docid = request.getParameter(Params.DOCID);
            String pageid = request.getParameter(Params.PAGEID);
            script.addAttribute("type","text/javascript");
            // if docid and pageid are unset handle it in the javascript
            script.addAttribute("src","/tilt/static/js/tilt.js?docid="+docid
                +"&pageid="+pageid+"&target=content");
            // simulate a CMS page
            Element div = new Element("div");
            div.addAttribute("id","content");
            div.addElement( script );
            doc.addElement( div );
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write( doc.toString() );
        }
        catch ( Exception e )
        {
            throw new TiltException(e);
        }
    }
}
