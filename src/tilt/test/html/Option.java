/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.test.html;
import tilt.Utils;
/**
 * An Option in a select list
 * @author desmond
 */
public class Option extends Element
{
    public Option( String name, String value )
    {
        super("option");
        this.addText( name );
        this.addAttribute("value", Utils.escapeQuotes(value,"*") );
    }
}
