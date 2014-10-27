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
import java.util.HashSet;
import java.util.Comparator;
import java.util.Arrays;
import tilt.exception.MatrixException;
import java.util.Iterator;
/**
 * A rewrite of the line-joining algorithm.We use a greedy approach. 
 * Differences between all points on the left and right are stored on a 
 * minimum priority queue, and those with the smallest differences are 
 * joined first.
 * @author desmond
 */
public class Matrix 
{
    /** left hand list of vertical y-positions */
    int[] list1;
    /** right-hand list of vertical y-positions */
    int[] list2;
    /** length of list2 */
    int M;
    /** length of list1 */
    int N;
    /** maximum amount times line depth to accept new lines */
    float LINE_DEPTH_FACTOR=3.0f;
    /** map of unique links between list1 and list2 y-positions */
    HashSet<QueueItem> uniqueMap;
    /** list1 line positions */
    ArrayList<Integer> lhs;
    /** list2 y-positions */
    ArrayList<Integer> rhs;
    /** running total of line diffs */
    float averageLineDepth;
    /**
     * Does the proposed join between lhs and rhs cross any other lines?
     * @param qi the item to test
     * @return true if it doesn't cross an existing line
     */
    private boolean noCrossover( QueueItem qi )
    {
        Iterator<QueueItem> iter = uniqueMap.iterator();
        while ( iter.hasNext() )
        {
            QueueItem item = iter.next();
            int lDiff = list1[qi.x]-list1[item.x];
            int rDiff = list2[qi.y]-list2[item.y];
            if ( (lDiff<0&&rDiff>0)||(lDiff>0&&rDiff<0) )
                return false;
        }
        return true;
    }
    /**
     * Compute the average lineDepth
     */
    private void computeLineDepth()
    {
        int totalLDiffs=0;
        int totalRDiffs = 0;
        if ( lhs.size()>1 )
        {
            // lhs and rhs should be the same size
            for ( int i=1;i<lhs.size();i++ )
            {
                int lDiff = lhs.get(i).intValue()-lhs.get(i-1).intValue();
                int rDiff = rhs.get(i).intValue()-rhs.get(i-1).intValue();
                totalLDiffs += lDiff;
                totalRDiffs += rDiff;
            }
            float nItems = lhs.size();
            float lAverage = (float)totalLDiffs / nItems;
            float rAverage = (float)totalRDiffs / nItems;
            averageLineDepth = (lAverage+rAverage)/2.0f;
        }
    }
    /**
     * Is the difference no more than some factor times line depth?
     * @param qi the queue item to test
     * @return true if the line ends are not too far apart
     */
    private boolean diffOK( QueueItem qi )
    {
        return qi.diff < Math.round(averageLineDepth*LINE_DEPTH_FACTOR);
    }
    /**
     * Insert into a integer list keeping it sorted
     * @param list the list to insert into
     * @param item the item to insert
     */
    private void insertItem( ArrayList<Integer>list, int item )
    {
        int oldSize = list.size();
        for ( int i=0;i<list.size();i++ )
        {
            int value= list.get(i).intValue();
            if ( value >= item )
            {
                list.add( i,new Integer(item) );
                break;
            }
        }
        if ( oldSize == list.size() )
            list.add( new Integer(item) );
    }
    /**
     * Do a binary search
     * @param the sorted list of Integers to search
     * @param item the item to looked for
     * @return true if it was there else false
     */
    boolean find( ArrayList<Integer> list, int item )
    {
        int top = 0;
        int bot = list.size()-1;
        while ( top <= bot )
        {
            int mid = (top+bot)/2;
            int value = list.get(mid).intValue();
            if ( item > value )
                top = mid+1;
            else if ( item < value )
                bot = mid-1;
            else
                return true;
        }
        return false;
    }
    /**
     * Would this queueItem have a unique x and y?
     * @param qi the queue item
     * @return true if it is attached to neither lhs nor rhs
     */
    boolean isUnique( QueueItem qi )
    {
        if ( find(this.lhs, list1[qi.x]) )
            return false;
        else
            return !find(this.rhs,list2[qi.y]);
    }
    /**
     * Compute the best alignment of two arrays of ints
     * @param list1 the first list arranged on the X-axis
     * @param list2 the second list on the Y-axis
     * @throws MatrixException
     */
    public Matrix( int[] list1, int[] list2 ) throws MatrixException
    {
        this.list1 = list1;
        this.list2 = list2;
        lhs = new ArrayList<>();
        rhs = new ArrayList<>();
        averageLineDepth = Float.MAX_VALUE;
        // NB; y is first index, x second
        M = list2.length;
        N = list1.length;
        QueueComparator qc = new QueueComparator();
        // make queue 10 times the minimum of M,N
        MinPQ<QueueItem> pq = new MinPQ<>(Math.min(M,N)*10,qc );
        for ( int y=0;y<M;y++ )
        {
            for ( int x=0;x<N;x++ )
            {
                int diff = Math.abs(list2[y]-list1[x]);
                QueueItem qi = new QueueItem(x,y,diff);
                pq.insert( qi );
            }
        }
        // now pop off the lowest unique min(M,N) joins
        uniqueMap = new HashSet<>();
        int nAlignments = Math.min(M,N);
        while ( uniqueMap.size()<nAlignments && !pq.isEmpty() )
        {
            QueueItem qi = pq.delMin();
            // if already present diff will be smaller
            if ( !uniqueMap.contains(qi) && noCrossover(qi) )
            {
                if ( diffOK(qi) && isUnique(qi) )
                {
                    insertItem(lhs,list1[qi.x]);
                    insertItem(rhs,list2[qi.y]);
                    computeLineDepth();
                    uniqueMap.add( qi );
                }
                else
                    break;
            }
        }
        // dump unique map
//        System.out.println("dumping map");
//        Iterator<QueueItem> iter = uniqueMap.iterator();
//        while ( iter.hasNext() )
//        {
//            QueueItem qi = iter.next();
//            System.out.println(qi);
//        }
    }
    /**
     * Comparator for MoveItems.
     */
    class MoveComparator implements Comparator<MoveItem>
    {
        /**
        * Compare two queue lines for y-position. Assume they don't cross.
        * @param q1 the first line
        * @param q2 the second line
        * @return -1 if q1 is higher than q2, 0 if equal or 1 if greater
        */
        @Override
        public int compare( MoveItem m1, MoveItem m2 )
        {
            if ( m1.move == Move.exch && m2.move==Move.exch )
            {
                if ( m1.y1<m2.y1 )
                    return -1;
                else if ( m1.y1 > m2.y1 )
                    return 1;
                else
                {
                    if( m1.y2 < m2.y2 )
                        return -1;
                    else if ( m1.y2 > m2.y2 )
                        return 1;
                    else
                        return 0;
                }
            }
            else if ( m1.move == Move.del && m2.move== Move.ins )
            {
                if ( m1.y1<m2.y2 )
                    return -1;
                else if ( m1.y1>m2.y2 )
                    return 1;
                else
                    return 0;
            }
            else if ( m1.move == Move.ins && m2.move==Move.del )
            {
                if ( m1.y2<m2.y1 )
                    return -1;
                else if ( m1.y2>m2.y1 )
                    return 1;
                else
                    return 0;
            }
            else if ( m1.move==Move.del )
            {
                if ( m1.y1<m2.y1 )
                    return -1;
                else if ( m1.y1 > m2.y1 )
                    return 1;
                else
                    return 0;
            }
            else //m1.move==Move.ins
            {
                if ( m1.y2<m2.y2 )
                    return -1;
                else if ( m1.y2 > m2.y2 )
                    return 1;
                else
                    return 0;
            }
        }
    }
    // now we have a map of minimal unique alignments
        // each alignment will be an exchange
        // and all the remaining elements of list1 are deletions
        // and all the remaining elements of list2 are insertions
        
    public Move[] traceBack()
    {
        ArrayList<MoveItem>moves = new ArrayList<>();
        boolean[] list1Marked = new boolean[list1.length];
        boolean[] list2Marked = new boolean[list2.length];
        Iterator<QueueItem>iter = uniqueMap.iterator();
        while ( iter.hasNext() )
        {
            QueueItem qi = iter.next();
            list1Marked[qi.x] = true;
            list2Marked[qi.y] = true;
            moves.add( new MoveItem(list1[qi.x],list2[qi.y],Move.exch) );
        }
        // add lines that petered out
        for ( int i=0;i<list1Marked.length;i++ )
        {
            if ( !list1Marked[i] )
                moves.add( new MoveItem(list1[i],-1,Move.del) );
        }
        // add lines that start here
        for ( int i=0;i<list2Marked.length;i++ )
        {
            if ( !list2Marked[i] )
                moves.add( new MoveItem(-1,list2[i],Move.ins) );
        }
        // now sort the moves by increasing y
        MoveItem[] array = new MoveItem[moves.size()];
        moves.toArray(array);
        Arrays.sort(array,new MoveComparator());
        Move[] actual = new Move[array.length];
        for ( int i=0;i<array.length;i++ )
            actual[i] = array[i].move;
        return actual;
    }
}
