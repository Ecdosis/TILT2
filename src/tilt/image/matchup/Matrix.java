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
import java.util.ArrayList;
/**
 * An MxN (x by y) matrix of ints through which to find the best alignment
 * (adapted from Needleman & Wunsch J. Mol. Biol. 48 (1970) 443-453)
 * @author desmond
 */
public class Matrix 
{
    /** the matrix of costs in *reaching* a square of size M+1xN+1 */
    int[][] MAT;
    /** lengths of Y and X dimensions respectively */
    int M,N;
    /** first list X-axis */
    int[] list1;
    /** second list Y-axis */
    int[] list2;
    /**
     * Compute the best alignment of two arrays of ints via dynamic programming
     * @param list1 the first list arranged on the X-axis
     * @param list2 the second list on the Y-axis
     */
    public Matrix( int[] list1, int[] list2 )
    {
        int cost;
        this.list1 = list1;
        this.list2 = list2;
        // NB; y is first index, x second
        M = list2.length;
        N = list1.length;
        // add extra row and column for final move 
        MAT = new int[M+1][N+1];
        // cost of getting to first square
        MAT[0][0] = 0;
        for ( int y=0;y<=M;y++ )
        {
            // compute the cost of getting to y,x
            for ( int x=(y==0)?1:0;x<=N;x++ )
            {
                // horizontal move
                int delCost = (x==0)?Integer.MAX_VALUE:MAT[y][x-1]+1;
                // vertical move
                int insCost = (y==0)?Integer.MAX_VALUE:MAT[y-1][x]+1;
                // diagonal move
                int diagCost = (y==0||x==0)?Integer.MAX_VALUE:MAT[y-1][x-1]
                    +Math.abs(list2[y-1]-list1[x-1]);
                cost = bestOfThree(delCost,insCost,diagCost);
                // bottom right in extra column/row contains the optimal cost
                MAT[y][x] = cost;
            }
        }
    }
    /**
     * Smallest of three
     * @param one first value
     * @param two second value
     * @param three third value
     * @return least of one, two, three
     */
    int bestOfThree( int one, int two, int three )
    {
        if ( one <= two )
            return (one<=three)?one:three;
        else
            return (two<=three)?two:three;
    }
    /**
     * Debug matrix to text
     * @return a string
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("   |");
        for ( int i=0;i<=N;i++ )
        {
            if ( i==N )
                sb.append("   ");
            else 
            {
                if ( list1[i]<10 )
                    sb.append("  ");
                else
                    sb.append(" ");
                sb.append( list1[i] );
            }
        }
        sb.append("\n----");
        for ( int i=0;i<=N;i++ )
            sb.append("---");
        sb.append("\n");
        for ( int y=0;y<=M;y++ )
        {
            if ( y==M )
                sb.append("  ");
            else
            {
                if ( list2[y]<10 )
                    sb.append(" ");
                sb.append(list2[y]);
            }
            sb.append(" |");
            for ( int x=0;x<=N;x++ )
            {
                if ( MAT[y][x]<10 )
                    sb.append("  ");
                else
                    sb.append(" ");
                sb.append(MAT[y][x]);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    /**
     * Print the path of move for debugging
     * @param path the path in question
     */
    public void printPath( ArrayList<Move> path )
    {
        StringBuilder line1 = new StringBuilder();
        StringBuilder line2 = new StringBuilder();
        StringBuilder line3 = new StringBuilder();
        int x = 0;
        int y = 0;
        for ( int i=0;i<path.size();i++ )
        {
            Move m = path.get(i);
            switch (m)
            {
                case exch:
                    line1.append((list1[x]<10)?"  ":" ");
                    line1.append(list1[x]);
                    line2.append("  |");
                    line3.append((list2[y]<10)?"  ":" ");
                    line3.append(list2[y]);
                    x++;
                    y++;
                    break;
                case ins:
                    line1.append("   ");
                    line2.append("  x");
                    line3.append((list2[y]<10)?"  ":" ");
                    line3.append(list2[y]);
                    y++;
                    break;
                case del:
                    line1.append((list1[x]<10)?"  ":" ");
                    line1.append(list1[x]);
                    line2.append("  x");
                    line3.append("   ");
                    x++;
                    break;
            }
        }
        System.out.println(line1);
        System.out.println(line2);
        System.out.println(line3);
    }
    /**
     * Trace back the path from M,N
     * @return the path of the optimal alignment
     */
    public ArrayList<Move> traceBack() throws Exception
    {
        int x = N;
        int y = M;
        ArrayList<Move> path = new ArrayList<Move>();
        while ( x > 0 || y > 0 )
        {
            int left = (x>0)?MAT[y][x-1]:Integer.MAX_VALUE;
            int top = (y>0)?MAT[y-1][x]:Integer.MAX_VALUE;
            int diag = (y>0&&x>0)?MAT[y-1][x-1]:Integer.MAX_VALUE;
            if ( x>0 && y>0 && diag+Math.abs(list2[y-1]-list1[x-1])==MAT[y][x] )
            {
                path.add(0,Move.exch);
                x--;
                y--;
            }
            else if ( left+1 == MAT[y][x] )
            {
                path.add(0,Move.del);
                x--;
            }
            else if ( top+1 == MAT[y][x] )
            {
                path.add(0,Move.ins);
                y--;
            }
            else 
                throw new Exception("Error in matrix");
        }
        return path;
    }
    /**
     * For testing
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        if ( args.length==2)
        {
            try
            {
                String[] arg1 = args[0].split(",");
                String[] arg2 = args[1].split(",");
                int[] list1 = new int[arg1.length];
                int[] list2 = new int[arg2.length];
                for ( int i=0;i<arg1.length;i++ )
                    list1[i] = Integer.parseInt(arg1[i]);
                for ( int i=0;i<arg2.length;i++ )
                    list2[i] = Integer.parseInt(arg2[i]);
                Matrix m = new Matrix( list1, list2 );
                System.out.println(m.toString());
                ArrayList<Move> moves = m.traceBack();
                m.printPath(moves);
            }
            catch ( Exception e )
            {
                e.printStackTrace(System.out);
            }
        }
        else
            System.out.println("usage: java IntDiff <list1> <list2>");
    }
}
