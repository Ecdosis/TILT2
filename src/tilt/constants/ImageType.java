/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.constants;

/**
 * Types of image requested
 * @author desmond
 */
public enum ImageType 
{
    original,
    greyscale,
    twotone;
    public static ImageType read( String str )
    {
        ImageType it;
        try
        {
            it = valueOf(str);
        }
        catch ( Exception e )
        {
            it= original;
        }
        return it;
    }
}
