/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.exception;

/**
 *  Something wrong with an image
 *  @author desmond
 */
public class ImageException extends TiltException
{
    public ImageException( String message )
    {
        super( message );
    }
    public ImageException( Exception e )
    {
        super( e );
    }
}
