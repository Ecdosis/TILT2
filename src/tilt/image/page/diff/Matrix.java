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
/**
 * Represent the matrix of string1 versus string 2 economically as a linked
 * list of active diagonals and manage the alignment process.
 */
package tilt.image.page.diff;
import java.util.ArrayList;

public class Matrix
{
    int[] A;
    int[] B;
    int goalIndex;
    int d;
    Diagonal best;
    Diagonal diagonals;
    boolean reachedEnd;
    /** test data */
    static int[] A_INT = {34,12,45,67,89,21,43,65,87,98};
    static int[] B_INT = {33,12,44,66,98,87,64,43,21};
        
    /**
     * Set up the matrix
     * @param str1 the first array of ints
     * @param str2 the second array of ints
     */
    protected Matrix( int[] str1, int[] str2 )
    {
        this.A = str1;
		// B is the old or base version
        this.B = str2;
        this.diagonals = null;
        this.goalIndex = B.length-A.length;
        this.best = null;
        this.reachedEnd = false;
    }
    /**
     * Convenience method to call compute basic diffs
     * @param newtext the new text not yet committed to the MVD
     * @param base the original version already in the MVD
     * @return an array of Diffs contained only changed diffs
     */
    public static Diff[] computeBasicDiffs( int[] newtext, int[] base )
    {
        Matrix m = new Matrix( newtext, base );
        m.compute();
        return m.getDiffs( true );
    }
    /**
     * Convenience method to call compute detailed diffs
     * @param newtext the new text not yet committed to the MVD
     * @param base the original version already in the MVD
     * @return an array of Diffs containing inserts dels and exchanges
     */
    public static Diff[] computeDetailedDiffs( int[] newtext, int[] base )
    {
        Matrix m = new Matrix( newtext, base );
        m.compute();
        return m.getDiffs( false );
    }
    /**
     * Compute only the runs of matching ints
     * @return an array of Diffs containing only 
     */
    public static Diff[] computeRuns( int[] text1, int[] text2 )
    {
        Diff[] diffs = computeDetailedDiffs( text1, text2 );
        int last1 = 0;
        int last2 = 0;
        ArrayList<Diff> alignments = new ArrayList<>();
        for ( Diff d : diffs )
        {
            if ( d.newOffset > last1 && d.oldOffset > last2 )
                alignments.add(new Diff(last2, last1, 
                    d.oldOffset-last2, d.newOffset-last1, DiffKind.ALIGNED));
            last1 = d.newOffset+d.newLen;
            last2 = d.oldOffset+d.oldLen;
        }
        Diff[] array = new Diff[alignments.size()];
        alignments.toArray( array );
        return array;
    }
    /**
     * Compute the matrix by finding the optimal path from source to sink.
     * We cannot compute each diagonal in sequence because they interfere
     * with their neighbours. So we alternatively compute the odd and even
     * diagonals. Once both odd and even diagonals have been done we increment
     * d (the edit distance).
     */
    void compute()
    {
        diagonals = new Diagonal( 0, this, 0 );
        diagonals.p = new Path( 0, 0, PathOperation.nothing, 0 );
        Diagonal current = diagonals;
        best = diagonals;
        d = 1;
        int run = 0;
        int N = (A.length>B.length)?A.length:B.length;
        while ( d < N )
        {
            while ( current != null && !current.atEnd() )
            {
                if ( (run % 2) == Math.abs(current.index % 2) )
                    current.seek();
                current = current.left;
            }
            if ( current != null && current.atEnd() )
                break;
            if ( run % 2 == 1 )
                d++;
            run++;
            current = diagonals;
        }
    }
    /**
     * Get the basic or detailed diffs after a run of compute
     * @param basic if true get only changed ranges, else show inserts dels etc
     * @return and array of Diffs
     */
    Diff[] getDiffs( boolean basic )
    {
        Diff[] diffs = new Diff[0];
        /*if ( t != null )
            t.print();*/
        if ( best.p != null )
            diffs = best.p.print( B.length, A.length, basic );
        return diffs;
    }
    /**
     * Check if we can move to the requested position.
     * If the minimum cost of the new diagonal is greater than the maximum
     * cost of the best diagonal then it is not viable. Also if we go outside
     * the square it's not valid either.
     * @param index the index of the diagonal requested
     * @param x its x value at the moment
     * @return true if it was OK, false otherwise
     */
    boolean check( int index, int x )
    {
        return valid(index,x) && within(index,x);
    }
    /**
     * Is this move within the matrix bounds? We can go one outside the square.
     * @param index the diagonal's index
     * @param x the x-value
     * @return true if it is the square
     */
    boolean within( int index, int x )
    {
        return x <= B.length && x-index <= A.length;
    }
    /**
     * Is this move on a valid diagonal?
     * @param index the diagonal index
     * @param x the x-value along it
     * @return true if it is valid, false otherwise
     */
    boolean valid( int index, int x )
    {
        int minCost = Math.abs(index-goalIndex);
        int maxCost = Math.abs(best.index-goalIndex)+(B.length-best.x);
        return minCost <= maxCost;
    }
    /**
     * Set the best value for the best diagonal
     * @param index the index of the diagonal
     * @param x its new x value that MAY be the best
     */
    void setBest( Diagonal dia )
    {
        if ( best == null )
            best = dia;
        else
        {
            int oldDist = (this.B.length-best.x) + Math.abs(best.index-goalIndex);
            int dist = (this.B.length-dia.x) + Math.abs(dia.index-goalIndex);
            if ( dist < oldDist )
                best = dia;
        }
        reachedEnd = best.atEnd();
    }
    public static void main( String[] args )
    {
        Diff[] diffs = Matrix.computeRuns( A_INT,B_INT);
        for ( Diff d : diffs )
            System.out.println(d );
    }
}
