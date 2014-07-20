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
import tilt.exception.MatchupException;
import java.util.ArrayList;

/**
 * Align word-shapes with actual words. 
 * @author desmond 7/7/13
 */
public class Matchup 
{
    /** the arrays to compare */
    int[] A,B;
    /** index of last element in D */
    int maxindex;
    /** the diagonals */
    Pos[] D;
    /** full matrix of computed positions */
    Pos[][] M;
    /**
     * Bracket an integer as a five-character string for debugging
     * @param l the left bracket
     * @param cost the number
     * @param r the right bracket
     * @return a formatted 6-character string
     */
    private static String printNumber( String l, int cost, String r )
    {
        StringBuilder sb = new StringBuilder();
        int initialCost = cost;
        boolean leading = true;
        sb.append(l);
        if ( !leading || cost/1000 > 0 )
        {
            sb.append( cost/1000 );
            cost = cost % 1000;
            leading = false;
        }
        else
            sb.append( " " );
        if ( !leading || cost/100 > 0 )
        {
            sb.append( cost/100 );
            cost = cost % 100;
            leading = false;
        }
        else
            sb.append( " " );
        if ( !leading || cost/10 > 0 )
        {
            sb.append( cost/10 );
            cost = cost % 10;
            leading = false;
        }
        else if ( initialCost > 9 )
            sb.append("0");
        else
            sb.append( " " );
        sb.append( cost );
        sb.append( r );
        return sb.toString();
    }
    /**
     * Set up the master object
     * @param A the first array of ints to matchup
     * @param B the second array
     */
    public Matchup( int[] A, int [] B )
    {
        this.A = A;
        this.B = B;
        D = new Pos[A.length+B.length+1];
        maxindex = D.length-1;
        M = new Pos[B.length+1][A.length+1];
    }
    /**
     * Get the index into the diagonals array starting at 0
     * @param diag the diagonal to convert to an index
     * @return the index
     */
    private int index( int diag )
    {
        return A.length + diag;
    }
    /**
     * Get the abstract index of the diagonal (goes from -A.length to B.length)
     * @param index the raw index goes from 0 to A.length+B.length
     * @return the notional "diagonal" index
     */
    private int diagonal( int index )
    {
        return index - A.length;
    }
    /**
     * Print out the saved matrix of scores for debug
     */
    private void print()
    {
        System.out.print("      ");
        for ( int i=0;i<B.length;i++ )
        {
            System.out.print( printNumber("{",B[i],"}") );
        }
        System.out.print(" (B)  ");
        System.out.print("\n");
        for ( int i=0;i<A.length+1;i++ )
        {
            if ( i < A.length )
                System.out.print( printNumber("{",A[i],"}") );
            else
                System.out.print(" (A)  ");
            for ( int j=0;j<B.length+1;j++ )
                System.out.print(printNumber("[",M[j][i].score,"]"));
            System.out.print("\n");
        }
        System.out.print("\n");
    }
    /**
     * Align segments of B with one from A, backwards
     * @param x the x-coordinate (index into B) to end at
     * @param y the y-coordinate (index into A) to end at
     * @return an extension object or null if not found
     */
    private Extension alignAcross( int x, int y )
    {
        int startX = x;
        int score=0;
        int bestX = --x;
        if ( x > 0 )
        {
            int bSum = B[x];
            while ( y > 0 && x > 0 )
            {
                if ( bSum+B[--x] <= A[y-1] )
                    bestX = x;
                else
                    break;
                bSum += B[x];
            }
            if ( bestX < startX-1 ) // we found something
            {
                score = M[bestX][y-1].score;
                int diff = A[y-1] - B[bestX];
                for ( int i=bestX+1;i<startX;i++ )
                    diff -= B[i];
                score += diff;
                return new Extension( bestX, y-1, score, M[bestX][y-1], true, 
                    startX-bestX );
            }       
        }
        return null;
    }
    /**
     * Align segments of A with one from B, upwards
     * @param x the x-coordinate (index into B) to end at
     * @param y the y-coordinate (index into A) to end at
     * @return an extension object or null if not found
     */
    private Extension alignUp( int x, int y )
    {
        int startY = y;
        int score=0;
        int bestY = --y;
        if ( y > 0 )
        {
            int aSum = A[y];
            while ( y > 0 && x > 0 )
            {
                if ( aSum+A[--y] <= B[x-1] )
                    bestY = y;
                else
                    break;
                aSum += A[y];
            }
            if ( bestY < startY-1 ) // we found something
            {
                score = M[x-1][bestY].score;
                int diff = B[x-1] - A[bestY];
                for ( int i=bestY+1;i<startY;i++ )
                    diff -= A[i];
                score += diff;
                return new Extension( x-1, bestY, score, M[x-1][bestY], false, 
                    startY-bestY );
            }
        }
        return null;
    }
    /**
     * Is there room to move below?
     * @param p the position to start from
     * @param d the diagonal we want to go to
     * @return true if we can move down up to 1-space outside the edit graph
     */
    private boolean roomBelow( Pos p, int d )
    {
        return p!=null && p.x-d < A.length;
    }
    /**
     * Is there room right?
     * @param p the position to start from
     * @return true if we can move right up to 1-space outside the edit graph
     */
    private boolean roomRight( Pos p )
    {
        return p != null && p.x < B.length;
    }
    /**
     * Create a path of linked Path objects tracing the optimal alignment
     * @param D the array of diagonals
     * @param finish the index of the final diagonal
     * @return the head of the list being the path of aligned cells
     */
    private Path makePath( Pos[] D, int finish )
    {
        Pos end = D[finish];
        ArrayList<Pos> list = new ArrayList<>();
        while ( end != null )
        {
            list.add(0,end);
            end = end.parent;
        }
        Pos current;
        Pos prev = list.get(0);
        Path head = null;
        Path p = null;
        for ( int i=1;i<list.size();i++ )
        {
            Path prevP = p;
            current = list.get(i);
            p = new Path( prev, current );
            if ( prevP != null )
                prevP.next = p;
            if ( head == null )
                head = p;
            prev = current;
        }
        return head;
    }
    /**
     * Find the best of five scores, including extensions
     * @param top the score moving from above
     * @param mid the score moving diagonally down
     * @param bot the score moving across
     * @param hext the score moving via an extension horizontally
     * @param vext the score moving via an extension vertically
     * @return the class of the best score
     */
    private Score getBestScore( int top, int mid, int bot, int hext, int vext )
    {
        Score s = Score.MID;
        // favour mid;
        int least = mid;
        if ( top < least )
        {
            s = Score.TOP;
            least = top;
        }
        if ( bot < least )
        {
            s= Score.BOT;
            least = bot;
        }
        if ( hext < least )
        {
            s = Score.HEXT;
            least = hext;
        }
        if ( vext < least )
            s = Score.VEXT;
        return s;
    }
    /**
     * Create a new position
     * @param top the Pos above us
     * @param mid the one on the same diagonal as us
     * @param bot the one to the left
     * @param d the diagonal we are trying to create a new Pos for
     */
    private Pos makePos( Pos top, Pos mid, Pos bot, int d )
    {
        Extension hExt=null,vExt=null;
        Pos p = new Pos();
        int topScore = Integer.MAX_VALUE;
        int botScore = Integer.MAX_VALUE;
        int midScore = Integer.MAX_VALUE;
        int hScore = Integer.MAX_VALUE;
        int vScore = Integer.MAX_VALUE;
        if ( top!=null && roomBelow(top,d+1) )
            topScore = top.score + A[top.x-(d+1)];
        if ( bot != null && roomRight(bot)  )
            botScore = bot.score + B[bot.x];
        if ( mid != null && roomRight(mid) && roomBelow(mid,d) )
        {
            midScore = mid.score + Math.abs(B[mid.x]-A[mid.x-d]);
            hExt = alignAcross( mid.x+1, mid.y+1);
            if ( hExt != null )
                hScore = hExt.score;
            vExt = alignUp( mid.x+1, mid.y+1 );
            if ( vExt != null )
                vScore = vExt.score;
        }
        Score s = getBestScore( topScore, midScore, botScore, hScore, vScore );
        switch ( s )
        {
            case TOP:
                p.x = top.x;
                p.y = top.x-d;
                p.score = topScore;
                p.parent = top;
                break;
            case MID:
                p.x = mid.x+1;
                p.y = p.x-d;
                p.score = midScore;
                p.parent = mid;
                break;
            case BOT:
                p.x = bot.x+1;
                p.y = p.x-d;
                p.score = botScore;
                p.parent = bot;
                break;
            case HEXT:
                p.x = hExt.xPosition();
                p.y = hExt.yPosition();
                p.score = hScore;
                p.parent = hExt.parent;
                break;
            case VEXT:
                p.x = vExt.xPosition();
                p.y = vExt.yPosition();
                p.score = vScore;
                p.parent = vExt.parent;
                break;
            
        }
        M[p.x][p.y] = p;
        return p;
    }
    /**
     * Run the alignment
     * @return a 3-D array of path rows, each a 2-D array of B,A word indices
     */
    public int[][][] align() throws MatchupException
    {
        // first Pos will be installed manually at index(0)
        int left=index(-1),right=index(1);
        // destination diagonal
        int finish = index(B.length-A.length);
        try
        {
            Pos top,mid,bot;
            // do the first match manually
            D[index(0)] = new Pos(0,0,0,null);
            M[0][0] = D[index(0)];
            do
            {
                for ( int i=left;i<=right;i+=2 )
                {
                    int d = diagonal(i);// diagonal we are trying to fill in
                    top = (i<maxindex&&roomBelow(D[i+1],d+1))?D[i+1]:null;
                    mid = (roomBelow(D[i],d)&&roomRight(D[i]))?D[i]:null;
                    bot = (i>0&&roomRight(D[i-1]))?D[i-1]:null;
                    if ( top!=null||bot!=null||mid!=null )
                    {
                        Pos p = makePos(top,mid,bot,d);
                        D[i] = p;
                    }
                }
                if ( roomBelow(D[left],diagonal(left)) )
                    left--;
                else
                    left++;
                if ( roomRight(D[right]) )
                    right++;
                else
                    right--;
                //print();
            } while ( D[finish]== null 
                || D[finish].x!=B.length||D[finish].y!=A.length );
            print();
            Path path = makePath( D, finish );
            //path.print();
            //System.out.println( "lowest cost alignment="+D[finish].score);
            return path.toArray();
        }
        catch ( Exception e )
        {
            throw new MatchupException( e );
        }
    }
    /**
     * Main entry point for the test
     * @param args unused
     */
    public static void main( String[] args )
    {
        /** some test arrays of ints */
        int[] A = {141,235,23,258,47,141,70,211,47};
        //,211,47,188,70,141,47,235,70,47,188,188,211,47,235,23,47,188,47,258,141,70,47,164,117,70,117,282,117,47,94,188,47,117,282,47,141,258,47,258,70,94,117,164,235,94,164};
        int[] B = {20,180,40,160,20,20,140,40,480,80};
        //,260,40,180,280,340,280,140,240,240,60,200,40,60,160,40,100,200,100,120,60,160,140,40,60,120,320,140,40,80,180,60,120,300,40,140,240,40,260,80,240,200,20,140,220};
        Matchup m = new Matchup( A, B );
        try
        {
            int[][][] rows = m.align();
            for ( int i=0;i<rows.length;i++ )
            {
                System.out.print("B: ");
                for ( int j=0;j<rows[i][0].length;j++ )
                {
                    System.out.print(B[rows[i][0][j]]);
                    //System.out.print(rows[i][0][j]);
                    if ( j<rows[i][0].length-1 )
                        System.out.print(",");
                }
                if ( rows[i][0].length==0 )
                    System.out.print("x");
                System.out.print(", ");
                System.out.print("A: ");
                for ( int j=0;j<rows[i][1].length;j++ )
                {
                    //System.out.print(rows[i][1][j]);
                    System.out.print(A[rows[i][1][j]]);
                    if ( j<rows[i][1].length-1 )
                        System.out.print(",");
                }
                if ( rows[i][1].length==0 )
                    System.out.print("x");
                System.out.print("\n");
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
        }
    }
}