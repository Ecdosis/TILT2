/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tilt.align;

/**
 * Exception if something went wrong with the matchup
 * @author desmond
 */
public class MatchupException extends Exception
{
    public MatchupException( Exception e )
    {
        super( e );
    }
}