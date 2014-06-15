/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.exception;

/**
 * Denial of Service exception
 * @author desmond
 */
public class DoSException extends TiltException
{
    /**
     * Create a new ImageExeption from scratch
     * @param message the message it is to contain
     */
    public DoSException( String message )
    {
        super( message );
    }
    /**
     * For wrapping another exception
     * @param e the other exception
     */
    public DoSException( Exception e )
    {
        super( e );
    }
}
