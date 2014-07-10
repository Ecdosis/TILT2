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