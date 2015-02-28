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
 * Represent a path from the best finishing position back to the start
 */
class Path
{
    Path next;
    int[] xValues,yValues;
    Path( int[] xValues, int[] yValues )
    {
        this.xValues = xValues;
        this.yValues = yValues;
    }
    Path( Pos parent, Pos child )
    {
        int xLen = child.x-parent.x;
        int yLen = child.y-parent.y;
        this.xValues = new int[xLen];
        this.yValues = new int[yLen];
        for ( int j=0,i=parent.x;i<child.x;i++,j++ )
            this.xValues[j] = i;
        for ( int j=0,i=parent.y;i<child.y;i++,j++ )
            this.yValues[j] = i;
    }
    void print()
    {
        System.out.print( "A: " );
        for ( int i=0;i<yValues.length;i++ )
        {
            System.out.print(yValues[i]);
            if ( i < yValues.length-1 )
                System.out.print(",");
        }
        if ( yValues.length==0 )
            System.out.print("x");
        System.out.print( ", B: " );
        for ( int i=0;i<xValues.length;i++ )
        {
            System.out.print(xValues[i]);
            if ( i < xValues.length-1 )
                System.out.print(",");
        }
        if ( xValues.length==0 )
            System.out.print("x");
        System.out.print("\n");
        if ( next != null )
            next.print();
    }
    /**
     * Get the length of this path
     * @return the number of path components in the list
     */
    int length()
    {
        int len = 0;
        Path p = this;
        while ( p != null )
        {
            len++;
            p = p.next;
        }
        return len;
    }
    /**
     * Assuming this is the list head convert it in to a 3-D array
     * @return an array of alignments between A and B, each of 1 or more ints
     */
    int[][][] toArray()
    {
        int[][][] rows = new int[length()][2][];
        Path p = this;
        int i = 0;
        while ( p != null )
        {
            rows[i][0] = p.xValues;
            rows[i][1] = p.yValues;
            i++;
            p = p.next;
        }
        return rows;
    }
}