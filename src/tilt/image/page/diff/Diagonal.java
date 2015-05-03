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
 *  (c) copyright Desmond Schmidt 2015
 */

package tilt.image.page.diff;

/**
 * Represent a diagonal in the Ukkonen diff algorithm
 * @author desmond
 */
public class Diagonal
{
   /** left is the horizontal move of increasing B indices
     right is the down move of increasing A indices */
    Diagonal left,right;
    Matrix parent;
    /** index of diagonal (y=x-index)*/
    int index;
    /** x-coordinate */
    int x;
    /** the path taken to get here */
    Path p;
    /**
     * Create a new diagonal
     * @param index its index from -M to +N (0 means x=0,y=0)
     * @param parent the parent matrix object
     * @param x the x-coordinate in the matrix.
     */
    Diagonal( int index, Matrix parent, int x )
    {
        this.parent = parent;
        this.index = index;
        this.left = this.right = null;
        this.x = x;
    }
    /**
     * Create a new path if there's a change in direction and join it to
     * the previous one
     * @param prev the previous path component
     * @param kind the kind of edit operation that got us here
     * @param len the length of the new operation
     */
    void setPrev( Path from, PathOperation kind, int fromX, int fromI, int len )
    {
        this.p = new Path( fromI, fromX, kind, len );
        this.p.prev = from;
    }
    /**
     * Seek to extend *this* diagonal. A goes down, B goes across
     */
    void extend()
    {
        int newX = x;
        while ( newX<parent.B.length && newX-index<parent.A.length
            && parent.B[newX] == parent.A[newX-index] )
            newX++;
        if ( newX > x )
        {
            setPrev( this.p, PathOperation.matched, x, index, newX-x );
            x = newX;
            parent.setBest( this );
        }
    }
    /**
     * Add a diagonal on the left moving horizontally
     */
    void addLeft()
    {
        if ( parent.check(index+1,x+1) )
        {
            left = new Diagonal( index+1, parent, x+1 );
            left.setPrev( p, PathOperation.deletion, this.x, this.index, 1 );
            left.right = this;
            left.extend();
        }
    }
    /**
     * Add a diagonal on the right
     */
    void addRight()
    {
        if ( parent.check(index-1, x) )
        {
            right = new Diagonal( index-1, parent, x );
            right.setPrev( p, PathOperation.insertion, this.x, this.index, 1 );
            right.left = this;
            right.extend();
            if ( parent.diagonals == this )
                parent.diagonals = right;
        }
    }
    /**
     * Advance this diagonal by considering the left and right diagonals
     * and choosing between insertion, deletion and exchange.
     */
    void advance()
    {
        // is insertion from left best?
        if ( (left != null && left.x > x+1)
            && (right == null || left.x > right.x+1) )
        {
            if ( parent.check(index,left.x) )
            {
                setPrev( left.p, PathOperation.insertion, left.x, left.index, 1 );
                x = left.x;
            }
            else if ( !parent.valid(index,left.x) )
                remove();
        }
        // or deletion from right?
        else if ( (right != null && right.x+1 > x+1)
            && (left == null || right.x+1 > left.x) )
        {
            if ( parent.check(index,right.x+1) )
            {
                setPrev( right.p, PathOperation.deletion, right.x, right.index, 1 );
                x = right.x+1;
            }
            else if ( !parent.valid(index,right.x+1) )
                remove();
        }
        // or exchange
        else if ( parent.check(index,x+1) )
        {
            setPrev( p, PathOperation.exchange, this.x, this.index, 1 );
            x = x+1;
        }
        else if ( !parent.valid(index,x+1) )
            remove();
        extend();
    }
    /**
     * Remove this diagonal from the list
     */
    void remove()
    {
        if ( left == null || right == null )
        {
            if ( this.left != null )
                this.left.right = this.right;
            if ( this.right != null )
                this.right.left = this.left;
            if ( this == parent.diagonals )
                parent.diagonals = this.left;
        }
        // only destroy if we are on the edge
    }
    /**
     * Seek the bottom right hand corner of the matrix. In this
     * revised version we only update the diagonal we are on,
     * not adjacent ones. However, if an adjacent diagonal is
     * empty we create a new diagonal as required.
     */
    void seek()
    {
        if ( x == 0 && index == 0 )
            extend();
        if ( left == null )
            addLeft();
        if ( right == null )
            addRight();
        advance();
    }
    /**
     * Check if we are finished. We must be beyond the final square at
     * bLen-1,aLen-1: either at bLen,aLen-1 (after deletion) or bLen,aLen
     * (after match/exchange) OR at bLen-1,aLen (after insertion).
     */
    boolean atEnd()
    {
        return (x==parent.B.length-1 && x-index==parent.A.length)
            || (x == parent.B.length && x-index>=parent.A.length-1);
    }
}
