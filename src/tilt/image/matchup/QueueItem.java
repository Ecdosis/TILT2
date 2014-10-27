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
 * An item in the priority queue of optimal alignments
 * @author desmond
 */
public class QueueItem 
{
    /** the x-index into the Matrix == index of list1 */
    int x;
    /** the y-index into the Matrix == index of list2 */
    int y;
    /** the difference between the two vertical locations */
    int diff;
    QueueItem( int x, int y, int diff )
    {
        this.x = x;
        this.y= y;
        this.diff= diff;
    }
    /**
     * This is needed to ensure that items are unique
     * @return the hash value of the QueueItem
     */
    public int hashCode() {
        return Math.abs(x*(y+101));
    }
    /**
     * Two queue items are the same if they share X and Y values
     * @param obj the other obj
     * @return true if they are equal
     */
    public boolean equals(Object obj) {
        QueueItem q2 = (QueueItem)obj;
        return this.x==q2.x||this.y==q2.y;
    }
    /**
     * Pretty-print ourselves
     * @return a string representation of ourself
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("x=");
        sb.append(this.x);
        sb.append(" y=");
        sb.append(this.y);
        sb.append(" diff=");
        sb.append(this.diff);
        return sb.toString();
    }
}
