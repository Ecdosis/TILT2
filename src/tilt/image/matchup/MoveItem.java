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
package tilt.image.matchup;

/**
 * Store a move and its leftmost y position.
 * @author desmond
 */
public class MoveItem implements Comparable<MoveItem> {
    Move move;
    int y1;
    int y2;
    public MoveItem( int y1, int y2, Move move )
    {
        this.y1 = y1;
        this.y2 = y2;
        this.move = move;
    }
    /**
    * Compare two moveitems for y-position. Assume they don't cross.
    * @param m2 the second moveitem
    * @return -1 if this is higher than m2, 0 if equal or 1 if greater
    */
    @Override
    public int compareTo( MoveItem m2 )
    {
        if ( this.move == Move.exch && m2.move==Move.exch )
        {
            if ( this.y1<m2.y1 )
                return -1;
            else if ( this.y1 > m2.y1 )
                return 1;
            else
            {
                if ( this.y2 < m2.y2 )
                    return -1;
                else if ( this.y2 > m2.y2 )
                    return 1;
                else
                    return 0;
            }
        }
        else if ( this.move == Move.del && (m2.move== Move.ins||m2.move==Move.exch) )
        {
            if ( this.y1<m2.y2 )
                return -1;
            else if ( this.y1>m2.y2 )
                return 1;
            else
                return 0;
        }
        else if ( (this.move == Move.ins||this.move==Move.exch) && m2.move==Move.del )
        {
            if ( this.y2<m2.y1 )
                return -1;
            else if ( this.y2>m2.y1 )
                return 1;
            else
                return 0;
        }
        else if ( this.move==Move.del )
        {
            if ( this.y1<m2.y1 )
                return -1;
            else if ( this.y1 > m2.y1 )
                return 1;
            else
                return 0;
        }
        else //if ( this.move==Move.ins||this.move==Move.exch )
        {
            if ( this.y2<m2.y2 )
                return -1;
            else if ( this.y2 > m2.y2 )
                return 1;
            else
                return 0;
        }
    }
}