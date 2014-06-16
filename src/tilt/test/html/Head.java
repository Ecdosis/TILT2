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

package tilt.test.html;
import java.util.ArrayList;
/**
 * Represent a HTML5 head
 * @author desmond
 */
public class Head 
{
    ArrayList<Script> scripts;
    ArrayList<Element> metas;
    Element css;
    /**
     * Create a head
     */
    public Head()
    {
        scripts = new ArrayList<Script>();
        metas = new ArrayList<Element>();
        css = new Element("style");
        css.addAttribute("type","text/css");
    }
    /**
     * Add a JQuery reference to an external script
     * @param version the version required
     */
    public void addJQuery( String version )
    {
        scripts.add( new JQueryRef(version) );
    }
    /**
     * Add a script with contents
     * @param script the contents
     */
    public void addScript( String script )
    {
        scripts.add( new Script(script) );
    }
    /**
     * Add somecss
     * @param text the css to add
     */
    public void addCss( String text )
    {
        css.addText( text );
    }
    /**
     * Add a header encoding like UTF-8
     * @param enc the text of the encoding in HTML5 compatible form
     */
    public void addEncoding( String enc )
    {
        Element elem = new Element("meta");
        elem.addAttribute("http-equiv","Content-Type");
        elem.addAttribute("content",enc);
        metas.add( elem );
    }
    /**
     * Convert the head to a string
     * @return a HTML string being the entire header
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<head>\n");
        for ( int i=0;i<metas.size();i++ )
            sb.append( metas.get(i) );
        for ( int i=0;i<scripts.size();i++ )
            sb.append( scripts.get(i) );
        if ( css.contents.length()>0 )
            sb.append(css);
        sb.append("</head>\n");
        return sb.toString();
    }
}
