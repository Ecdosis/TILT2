/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.exception;

/**
 * An exception raised when finding lines
 * @author desmond
 */
public class MatrixException extends TiltException
{
    /**
     * Create a new SplitExeption from scratch
     * @param message the message it is to contain
     */
    public MatrixException( String message )
    {
        super( message );
    }
    /**
     * For wrapping another exception
     * @param e the other exception
     */
    public MatrixException( Exception e )
    {
        super( e );
    }
}
