/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        sb.append(">\n");
        sb.append( contents );
        sb.append("\n</");
        sb.append(name);
        sb.append(">\n");
        return sb.toString();
    }
}
