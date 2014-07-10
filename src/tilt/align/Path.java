/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tilt.matchup;

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
    void print()
    {
        System.out.print( "A: " );
        for ( int i=0;i<yValues.length;i++ )
        {
            System.out.print(yValues[i]);
            if ( i < yValues.length-1 )
                System.out.print(",");
        }
        System.out.print( ", B: " );
        for ( int i=0;i<xValues.length;i++ )
        {
            System.out.print(xValues[i]);
            if ( i < xValues.length-1 )
                System.out.print(",");
        }
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