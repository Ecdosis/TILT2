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
 * Represent a HTML5 element
 * @author desmond
 */
public class Element
{
    StringBuilder contents;
    ArrayList<Attribute> attrs;
    String name;
    public Element( String name )
    {
        this.name = name;
        contents = new StringBuilder();
        attrs = new ArrayList<Attribute>();
    }
    /**
     * Add an attribute to the script element
     * @param name name of the attribute
     * @param value its value
     */
    public void addAttribute( String name, String value )
    {
        attrs.add( new Attribute(name,value) );
    }
    /**
     * Add some text to the HTML
     * @param text
     */
    public void addText( String text )
    {
        contents.append( text );
    }
    /**
     * Add an element to the body
     * @param elem the element
     */
    public void addElement( Element elem )
    {
        contents.append( elem.toString() );
    }
    /**
     * Compose a HTML element for output
     * @return a String
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append( name );
        for ( int i=0;i<attrs.size();i++ )
            sb.append( attrs.get(i).toString() );
        sb.append(">");
        sb.append( contents );
        sb.append("</");
        sb.append(name);
        sb.append(">\n");
        return sb.toString();
    }
}
