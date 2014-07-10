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
 * Represent a position in the edit graph
 */
 class Pos
 {
    /** the cost expended to get here */
    int score;
    /** from whence we came */
    Pos parent;
    /** the x-position (index into B) */
    int x;
    /** the y-position (index into A) */
    int y;
    /**
     * Manual constructor
     * @param x the Pos's x-value
     * @param y the Pos's y-value
     * @param score its score
     * @param parent its parent
     */
    Pos( int x, int y, int score, Pos parent )
    {
        this.x = x;
        this.y = y;
        this.parent = parent;
        this.score = score;
    }
    Pos()
    {
    }
}