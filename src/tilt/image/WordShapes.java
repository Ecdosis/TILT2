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

package tilt.image;
import tilt.image.page.Line;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Comparator;
import tilt.image.geometry.Polygon;
import tilt.handler.post.Options;
import tilt.handler.post.TextIndex;

/**
 * Compute goodness of fit of found word-shapes to the reference computed 
 * from actual words and their shapes in printed form.
 * @author desmond
 */
public class WordShapes 
{
    /** lines array from page */
    ArrayList<Line> lines;
    /** deep copy of original lines */
    ArrayList<ArrayList<Polygon>> copyOfLines;
    /** value for current best gap between words */
    double bestGap;
    /** global options set by user */
    Options opts;
    /** number of words in text */
    int numWords;
    /** fraction of average polygon area to identify runt */
    static double RUNT_FACTOR = 0.05;
    static int MAX_CULLS = 3;
    /**
     * Create a word-space calculator
     * @param text the text of the page
     * @param lines the lines array from the page
     * @param opts the global options for the project
     */
    WordShapes( TextIndex text, ArrayList<Line> lines, Options opts )
    {
        this.opts = opts;
        this.lines = lines;
        this.numWords = text.getWords().length;
    }
    /**
     * Make a shallow copy of the lines array (same polygons)
     * @return the deeply cloned copy
     */
    private ArrayList<ArrayList<Polygon>> cloneLines()
    {
        ArrayList<ArrayList<Polygon>> copy = new ArrayList<>();
        for ( Line l : lines )
        {
            Polygon[] shapes = l.getShapes();
            ArrayList<Polygon> list = new ArrayList<>();
            for ( Polygon shape : shapes )
            {
                list.add( shape );
            }
            copy.add(list);
        }
        return copy;
    }
    /**
     * Compute the mean area of polygons in the lines copy
     * @return the mean
     */
    private double meanArea()
    {
        double total = 0.0;
        int N = 0;
        for ( ArrayList<Polygon> line : copyOfLines )
        {
            Polygon last;
            Polygon curr = null;
            for ( Polygon pg : line )
            {
                last = curr;
                curr = pg;
                if ( last != null && last.distanceBetween(curr) < bestGap )
                    curr = last.merge( curr );
                else if ( last != null )
                {
                    total += last.area();
                    N++;
                }
            }
            if ( curr != null )
            {
                total += curr.area();
                N++;
            }
        }
        return total / N;
    }
    /**
     * Remove polygons less than about 5% the average size
     * @param minArea the minimum polygon area
     * @return a list of runt polygons
     */
    private HashSet<Integer> getRunts( double minArea )
    {
        HashSet<Integer> runts = new HashSet<>();
        for ( ArrayList<Polygon> line : copyOfLines )
        {
            Polygon last;
            Polygon curr = null;
            ArrayList<Polygon> saved = new ArrayList<>();
            for ( Polygon shape : line )
            {
                last = curr;
                curr = shape;
                saved.add( curr );
                if ( last != null )
                {
                    double dist = last.distanceBetween(curr);
                    if ( dist < this.bestGap )
                        curr = last.merge(curr);
                    else 
                    {
                        if ( last.area() < minArea )
                        {
                            saved.remove( curr );
                            for ( Polygon pg : saved )
                                runts.add(pg.ID);
                        }
                        saved.clear();
                        saved.add( curr ); // curr not assessed yet
                    }
                }
            }
            if ( curr != null && curr.area() < minArea )
                runts.add( curr.ID );
        }
        return runts;
    }
    /**
     * Compute the optimal gap based on current word-shapes
     */
    private double computeGap()
    {
        ArrayList<Double> gaps = new ArrayList<>();
        for ( ArrayList<Polygon> line : copyOfLines )
        {
            Polygon last;
            Polygon curr=null;
            for ( Polygon shape : line )
            {
                last = curr;
                curr = shape;
                if ( last != null )
                {
                    double distance = last.distanceBetween(curr);
                    gaps.add(distance);
                }
            }
        }
        // sort gaps by decreasing order
        Double[] gapArray = new Double[gaps.size()];
        gaps.toArray( gapArray );
        Arrays.sort( gapArray, new ReverseDoubleComparator() );
        // so now the gaps are in decreasing order
        int numGaps = numWords - lines.size();
        double[] distances = new double[numGaps];
        for ( int i=0;i<numGaps;i++ )
            distances[i] = gapArray[i];
        return distances[distances.length-1];
    }
    /**
     * Actually remove the runts of the litter from lines
     * @param runts set of polygons of ill repute
     * @param minArea the minimum area below which we assume a runt
     */
    private void removeRunts( HashSet<Integer> runts, double minArea )
    {
        int lineNo = 1;
        for ( Line l : lines )
        {
            ArrayList<Polygon> replacement = new ArrayList<>();
            Polygon[] polys = l.getShapes();
            for ( Polygon pg : polys )
            {
                if ( pg.area() < minArea )
                {
                    int target = -1;
                    if ( runts.contains(pg.ID) )
                         target = pg.ID;
                    if ( target == -1 )
                        replacement.add( pg );
                    else
                    {
                        runts.remove( target );
                        System.out.println( "removed runt polygon id="+target
                            +" on line "+lineNo );
                    }
                }
                else
                    replacement.add( pg );
            }
            l.setShapes( replacement );
            lineNo++;
        }
    }
    /**
     * Form optimal word-shapes and store them in situ in the lines array
     */
    public void makeWords()
    {
        copyOfLines = cloneLines();
        int numCulls = 0;
        this.bestGap = computeGap();
        double mean = meanArea();
        while ( numCulls < MAX_CULLS )
        {
            double minArea = mean*RUNT_FACTOR;
            HashSet<Integer> runts = getRunts(minArea);
            if ( runts.size()> 0 )
            {
                removeRunts( runts, minArea );
                copyOfLines = cloneLines();
                this.bestGap = computeGap();
                System.out.println("best gap ="+bestGap);
                mean = meanArea();
                numCulls++;
            }
            else
                break;
        }
        // now update the actual lines array
        for ( Line l : lines )
        {
            Polygon[] shapes = l.getShapes();
            Polygon last;
            Polygon curr = null;
            ArrayList<Polygon> replacement = new ArrayList<>();
            for ( Polygon shape : shapes )
            {
                last = curr;
                curr = shape;
                if ( last != null )
                {
                    double dist = last.distanceBetween(curr);
                    if ( dist < this.bestGap )
                        curr = last.merge(curr);
                    else
                        replacement.add( last );
                }
            }
            if ( curr != null )
                replacement.add( curr );
            l.setShapes( replacement );
        }
    }
    /**
     * Compare areas of polygon by decreasing order
     */
    class ReverseDoubleComparator implements Comparator<Double>
    {
        @Override
        public int compare( Double i1, Double i2 )
        {
            return i2.compareTo(i1);
        }
        @Override
        public boolean equals(Object obj)
        {
            return this == obj;
        }
    }
}
