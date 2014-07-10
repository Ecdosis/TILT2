/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tilt.align;

/**
* Shortcut, or cross-diagonal multi-Pos extension
*/
class Extension extends Pos
{
    /** does it extend across or down (false) */
    boolean across;
    /** the number of aligned segments */
    int length;
    Extension ( int x, int y, int score, Pos src, boolean across, int length )
    {
        // but x,y are for this Pos, backward to src
        super( x, y, score, src );
        this.across = across;
        this.length = length;
    }
    /**
     * Compute the finishing x-position of this extension. Since two extensions 
     * will only be compared when they are on the same diagonal, this is a 
     * good measure of which is ahead of the other.
     * @return the x-coordinate at which we end
     */
    int xPosition()
    {
        return (across)?x+length:x+1;
    }
    int yPosition()
    {
        return (across)?y+1:y+length;
    }
}