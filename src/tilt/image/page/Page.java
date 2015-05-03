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


package tilt.image.page;
import tilt.image.geometry.Polygon;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import tilt.image.geometry.Point;
import tilt.image.matchup.Matrix;
import tilt.image.matchup.Move;
import tilt.exception.*;
import java.awt.image.WritableRaster;
import java.awt.image.BufferedImage;
import org.json.simple.*;
import tilt.handler.post.Options;

/**
 * Represent a collection of lines already recognised on a page
 * @author desmond
 */
public class Page 
{
    ArrayList<Line> lines;
    QuadTree qt;
    int medianLineDepth;
    int minWordGap;
    int numWords;
    Options options;
    /**
     * Convert an array of scaled column-peaks into lines
     * @param cols a 2-D array of scaled line-peaks going *down* the page
     * @param hScale the scale in the horizontal dimension
     * @param vScale the scale in the vertical dimension 
     * @param numWords number of words on page
     * @param options options for this project
     * @param cropRect limits of image
     * @throws Exception
     */
    public Page( ArrayList[] cols, int hScale, int vScale, int numWords,
        Options options, Rectangle cropRect ) throws Exception
    {
        this.numWords = numWords;
        this.options = options;
        this.qt = new QuadTree( cropRect.x, cropRect.y, cropRect.width,
            cropRect.height);
        lines = new ArrayList<>();
        for ( int i=0;i<cols.length-1;i++ )
        {
            ArrayList col1 = cols[i];
            ArrayList col2 = cols[i+1];
            int[] list1 = new int[col1.size()];
            int[] list2 = new int[col2.size()];
            listToIntArray(col1,list1);
            listToIntArray(col2,list2);
            Matrix m = new Matrix( list1, list2, options );
            Move[] moves = m.traceBack();
            int x2,z2;
            for ( int y1=0,y2=0,j=0;j<moves.length;j++ )
            {
                Move move = (Move)moves[j];
                switch ( move )
                {
                    case exch:
                        x2=(i+1)*hScale;
                        z2=list2[y2]*vScale;
                        this.update(new Point(x2,z2), list1[y1],list2[y2]);
                        y1++;
                        y2++;
                        break;
                    case ins:
                        x2=(i+1)*hScale;
                        z2=list2[y2]*vScale;
                        open( new Point(x2,z2), list2[y2] );
                        y2++;
                        break;
                    case del:
                        close( list1[y1],hScale );
                        y1++;
                        break;
                }
            }
        }
    }
    /**
     * Convert an ArrayList of Integers to an int array
     * @param col the ArrayList of Integers
     * @param list the target int array
     * @throws Exception 
     */
    private void listToIntArray( ArrayList col, int[] list ) throws Exception
    {
        if ( list.length != col.size() )
            throw new ArrayIndexOutOfBoundsException(
                "col not the same length as array");
        for ( int j=0;j<col.size();j++ )
        {
            Object obj = col.get(j);
            if ( obj instanceof Integer )
                list[j] = ((Integer)col.get(j)).intValue();
            else
                throw new ClassCastException("object is not an Integer");
        }
    }
    /**
     * Draw the lines on the page
     * @param g the graphics environment
     */
    public void draw( Graphics g )
    {
        g.setColor(Color.BLACK);
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            l.draw( g );
        }
        //System.out.println("drew "+lines.size()+" lines");
    }
    /**
     * Align a new point to an existing line, or create one
     * @param loc the new location in true coordinates
     * @param oldEnd the previous end of the line 
     * @param newEnd the new end of the line 
     */
    private void update( Point loc, int oldEnd, int newEnd )
    {
        int i;
        for ( i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            if ( l.open && oldEnd == l.end )
            {
                l.update( loc, newEnd );
                break;
            }
        }
    }
    /**
     * Stop a line from propagating further
     * @param end the scaled vertical point of the line
     * @param hScale width of a rectangle
     */
    private void close( int end, int hScale )
    {
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            if ( l.end == end )
            {
                l.close(hScale);
                break;
            }
        }
    }
    /**
     * Insert a new line in the array
     * @param loc the location of its leftmost position
     * @param end the unscaled end y-coordinate
     */
    private void open( Point loc, int end )
    {
        int i;
        for ( i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            if ( l.end > end )
            {
                Line line = new Line();
                line.addPoint( loc, end );
                lines.add( i,line );
                break;
            }
        }
        if ( i == lines.size() )
        {
            Line line = new Line();
            line.addPoint( loc, end );
            lines.add( line );
        }
    }
    /**
     * Trim all the line starts right to the first black pixel on the image
     * @param wr the raster
     * @param hScale the size of a rectangular sample
     * @param vScale the vertical scale
     * @param black the value of a "black" pixel
     */
    public void refineLeft( WritableRaster wr, int hScale, int vScale, int black )
    {
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            l.refineLeft( wr, hScale, 10, black );
        }
    }
    /**
     * Trim all the line ends right to the last black pixel in the row
     * @param wr the raster
     * @param hScale the size of a rectangular sample
     * @param vScale the vertical scale
     * @param black the value of a "black" pixel
     */
    public void refineRight( WritableRaster wr, int hScale, int vScale, int black )
    {
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            l.refineRight( wr, hScale, 10, black );
        }
    }
    /**
     * Get the lines array of this page
     * @return an array of line objects
     */
    public ArrayList<Line> getLines()
    {
        return lines;
    }
    /**
     * Add a shape to the 
     * @param pg the shape to remember
     */
    public void addShape( Polygon pg )
    {
        if ( pg.getLine() != null )
            pg.getLine().add(pg);
        this.qt.addPolygon(pg);
    }
    /**
     * Print the word shapes over the top of the original image
     * @param original the original raster to write to
     */
    public void print( BufferedImage original )
    {
        Graphics g = original.getGraphics();
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get( i );
            l.print( g, original.getRaster(), i+1 );
        }
    }
    /**
     * Sort all lines based on their medianY position
     */
    public void sortLines()
    {
        int i, j, k, h; 
        Line v;
        int[] incs = { 1391376, 463792, 198768, 86961, 33936,
            13776, 4592, 1968, 861, 336, 
            112, 48, 21, 7, 3, 1 };
        for ( k = 0; k < 16; k++)
        {
            for ( h=incs[k],i=h;i<lines.size();i++ )
            { 
                v = lines.get(i); 
                j = i;
                while (j >= h && lines.get(j-h).getAverageY()>v.getAverageY() )
                { 
                    lines.set(j,lines.get(j-h)); 
                    j -= h; 
                }
                lines.set(j,v);
            }
        }
    }
    /**
     * Work out the median word-gap and median line-depth
     * @param wr the raster of the twotone or cleaned image
     */
    public void finalise( WritableRaster wr )
    {
         // compute median line depth
        HashSet<Integer> vGaps = new HashSet<>();
        for ( int i=0;i<lines.size()-1;i++ )
        {
            Line l1 = lines.get( i );
            Line l2 = lines.get( i+1 );
            vGaps.add( Math.abs(l2.getAverageY()-l1.getAverageY()) );
        }
        Integer[] vArray = new Integer[vGaps.size()];
        vGaps.toArray( vArray );
        Arrays.sort( vArray );
        medianLineDepth = (vArray.length<=1)?0:vArray[vArray.length/2].intValue();
        //sortLines();
        joinBrokenLines();
    }
    /**
     * Remove lines with only 1 shape that is &lt; or = minWordGap
     */
    public void pruneShortLines()
    {
        ArrayList<Line> delenda = new ArrayList<>();
        for( int i=0;i<this.lines.size();i++ )
        {
            Line l = lines.get(i);
            if ( l.shapes.size()==1 )
            {
                Polygon pg = l.shapes.get(0);
                if ( pg.getBounds().width <= minWordGap )
                    delenda.add( l );
            }
        }
        for ( int i=0;i<delenda.size();i++ )
            this.lines.remove( delenda.get(i) );
    }
    /**
     * Retrieve the minimum gap between words in pixels
     * @return an int
     */
    public int getWordGap()
    {
        if ( minWordGap == 0 )
        {
            HashMap<Integer,Integer> hGaps = new HashMap<>();
            for ( int i=0;i<lines.size();i++ )
            {
                Line l = lines.get(i);
                l.addGaps( hGaps );
            }
            Set<Integer> keys = hGaps.keySet();
            Integer[] hArray = new Integer[hGaps.size()];
            keys.toArray( hArray );
            Arrays.sort( hArray );
            int runningTotal= 0;
            int targetGaps = (numWords-(lines.size()-1))-1;
            //System.out.println("target gaps="+targetGaps+" numWords="+numWords);
            for ( int i=hArray.length-1;i>=0;i-- )
            {
                runningTotal += hGaps.get(hArray[i]).intValue();
                if ( runningTotal >= targetGaps )
                {
                    minWordGap = hArray[i].intValue();
                    break;
                }
            }
        }
        return minWordGap;
    }
    /**
     * Retrieve the median line-height (distance between baselines) in pixels
     * @return an int
     */
    public int getLineHeight()
    {
        return medianLineDepth;
    }
    /**
     * Join up adjacent part-words
     */
    public void joinWords()
    {
        int nWords=0;
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            l.mergeWords( minWordGap );
            nWords += l.countShapes();
        }
        //System.out.println("recognised"+nWords+" words");
    }
    /**
     * Join up broken lines BEFORE any shapes have been recognised
     */
    void joinBrokenLines()
    {
        // 1. find lines that are less than half the average lineheight apart
        // and overlap by less than 10% of their overall length
        ArrayList<ArrayList<Line>> sets = new ArrayList<>();
        int halfMedianHeight = medianLineDepth/2;
        Line prev = null;
        ArrayList<Line> current = null;
        for ( int i=0;i<lines.size();i++ )
        {
            Line line = lines.get(i);
            if ( prev != null 
                && Math.abs(prev.getAverageY()-line.getAverageY())<halfMedianHeight
                && prev.overlap(line) < 0.25f )
            {
                if ( current == null )
                    current = new ArrayList<>();
                if ( !current.contains(prev) )
                    current.add( prev );
                current.add( line );
            }
            else if ( current != null )
            {
                sets.add( current );
                current = null;
            }
            prev = line;
        }
        // coda
        if ( current != null )
            sets.add( current );
        // examine closely related lines
        for ( int i=0;i<sets.size();i++ )
        {
            ArrayList<Line> set = sets.get(i);
            Line[] array = new Line[set.size()];
            set.toArray(array);
            Arrays.sort(array);
            int mTotal = 0;
            for ( int m=0;m<set.size();m++ )
            {
                mTotal+= set.get(m).end;
            }
            int newEnd = mTotal/set.size();
            Line merged = new Line();
            for ( int j=0;j<set.size();j++ )
            {
                Line l = set.get(j);
                for ( int k=0;k<l.points.size();k++ )
                {
                    Point p = l.points.get(k);
                    merged.addPoint( p, newEnd );
                }
                // remove once finished
                lines.remove( l );
            }
            merged.sortPoints();
            // insert merged line into page
            int j;
            for ( j=0;j<lines.size();j++ )
            {
                Line l = lines.get(j);
                if ( l.getAverageY() > merged.getAverageY() )
                {
                    lines.add( j, merged );
                    break;
                }
            }
            if ( j == lines.size() )
                lines.add( merged );
        }
    }
    /**
     * Compose the shape information as GeoJson
     * @param pageWidth the width of the "page" (maybe a part of the image)
     * @param pageHeight the height of the "page"
     * @return the GeoJson
     */
    public String toGeoJson( int pageWidth, int pageHeight )
    {
        JSONObject image=new JSONObject();
        image.put("type","FeatureCollection");
        JSONArray bounds = new JSONArray();
        // assume one bounding box for now, without rotation
        bounds.add(new Double(0.0) );
        bounds.add(new Double(0.0) );
        bounds.add(new Double(1.0) );
        bounds.add(new Double(1.0) );
        image.put( "bbox", bounds );
        JSONArray features = new JSONArray();
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            features.add( l.toGeoJSON(pageWidth, pageHeight) );
        }
        image.put("features", features );
        return image.toString();
    }
    /**
     * Get the number of pixels per char
     * @param numChars the number of chars on the page
     * @return the fractional number of pixels per char
     */
    public float pixelsPerChar( int numChars )
    {
        float pixelWidth = 0;
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            int[] wordWidths = l.getWidths();
            for ( int j=0;j<wordWidths.length;j++ )
                pixelWidth += wordWidths[j];
        }
        return pixelWidth/(float) numChars;
    }
    /**
     * Get the widths of all the shapes on the page in pixels
     * @return an array of rounded shape widths as ints
     */
    public int[] getShapeWidths()
    {
        int size = 0;
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            size += l.countShapes();
        }
        int[] widths = new int[size];
        int start = 0;
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            start += l.getShapeWidths( widths, start );
        }
        return widths;
    }
    /**
     * Count the number of syllables needed for words split over lines
     * @param nShapes number of shapes consumed by one word
     * @param k offset in current line
     * @param j index into lineStarts
     * @param lineStarts the linestarts (shapeOffsets) array
     * @return number of syllables to divide word into
     */
    private int countSyllables( int nShapes, int k, int j, int[] lineStarts )
    {
        int nSyl = 0;
        while ( nShapes > 0 && j<lineStarts.length )
        {
            int lineLen;
            if ( j < lineStarts.length-1 )
                lineLen = lineStarts[j+1]-lineStarts[j];
            else
                lineLen = lines.get(j).countShapes();
            int oldK = k;
            while ( nShapes>0 && k < lineLen )
            {
                nShapes--;
                k++;
            }
            // found at least one shape on line
            if ( oldK < k )
                nSyl++;
            j++;
            k = 0;
        }
        return nSyl;
    }
    /**
     * Align word shapes in image to words in text
     * @param alignments an array of alignments, each [0]=shapes,[1]=words
     * @param shapeOffsets offsets into the shapes array for each line start
     * @param words the word objects on the page
     * @param wr raster of cleaned image
     * @throws AlignException
     */
    public void align( int[][][] alignments, int[] shapeOffsets, 
        Word[] words, WritableRaster wr ) throws AlignException
    {
        if ( lines.size() > 0 )
        {
            // line index
            int j = 0;
            // shape index per line
            int k = 0;
            Line l = lines.get(0);
            l.reset();
            ArrayList<Merge> merges = new ArrayList<>();
            ArrayList<Split> splits = new ArrayList<>();
            for ( int i=0;i<alignments.length;i++ )
            {
                int[][] alignment = alignments[i];
                int[] sIndices = alignment[0];
                int[] wIndices = alignment[1];
                while ( !l.hasShape(k) )
                {
                    if ( j+1==lines.size() )
                    {
                        System.out.println("tried to get missing line");
                        break;
                    }
                    l = lines.get(++j);
                    l.reset();
                    k = 0;
                }
                if ( sIndices.length>0 && k+shapeOffsets[j] != sIndices[0] )
                    System.out.println("Out of sync!");
                if ( sIndices.length == wIndices.length )
                {
                    l.setShapeWord( k++, words[wIndices[0]] );
                }
                else if ( sIndices.length == 1 && wIndices.length > 1 )
                {
                    Word[] wObjs = new Word[wIndices.length];
                    for ( int m=0;m<wIndices.length;m++ )
                        wObjs[m] = words[wIndices[m]];
                    splits.add( new Split(l,k++,wObjs,wr) );
                }
                else if ( sIndices.length > 1 && wIndices.length == 1 )
                {
                    int last = sIndices.length-1;
                    if ( j==shapeOffsets.length-1 
                        || sIndices[last] < shapeOffsets[j+1] )
                    {
                        // all fit one line
                        merges.add( new Merge(l, shapeOffsets[j], sIndices, 
                            words[wIndices[0]]) );
                        k += sIndices.length;
                    }
                    else
                    {
                        // more than one line
                        // m is index into sIndices
                        int m = 0;
                        // s is index into syllables
                        int s = 0;
                        // count number of syllables
                        int nSyl = countSyllables(sIndices.length,k,j,shapeOffsets);
                        Word[] syllables = words[wIndices[m]].spitInto(nSyl);
                        while ( m<sIndices.length )
                        {
                            int chunk = Math.min(l.countShapes()-k,sIndices.length-m);
                            if ( chunk == 0 )
                            {
                                l = lines.get(++j);
                                l.reset();
                                k = 0;
                            }
                            else
                            {
                                int [] slice = new int[chunk];
                                for ( int n=0;n<chunk;n++ )
                                    slice[n] = sIndices[m++];
                                merges.add( new Merge(l, shapeOffsets[j], slice, 
                                    syllables[s++]) );
                                k += chunk;
                            }
                        }
                    }
                }
                else if ( wIndices.length==0 )
                    k++;
                else    // no shape for word
                    continue;
            }
            // now execute the saved merges and splits
            try
            {
                for ( Merge m : merges )
                    m.execute();
                for ( Split s : splits )
                    s.execute();
            }
            catch ( Exception e )
            {
                throw new AlignException( e );
            }
        }
    }
    /**
     * Get the indices into the shapes for the page at each line start
     * @return an array of ints
     */
    public int[] getShapeLineStarts()
    {
        int[] starts = new int[lines.size()];
        int i = 0;
        int total = 0;
        for ( Line l : lines )
        {
            starts[i++] = total;
            total += l.shapes.size();
        }
        return starts;
    }
    public void resetShapes()
    {
        for ( int i=0;i<lines.size();i++ )
            lines.get(i).resetShapes();
    }
    public Polygon shapeForPoint( Point p )
    {
        return this.qt.pointInPolygon( p );
    }
    public void joinLines()
    {
        ArrayList<Line> merges= new ArrayList<>();
        for ( int i=1;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            Line p = lines.get(i-1);
            // is l to the left of p?
            float end = l.points.get(l.points.size()-1).x;
            float start = p.points.get(0).x;
            if ( end <= start )
            {
                float lY = l.points.get(l.points.size()-1).y;
                float pY = p.points.get(0).y;
                if ( Math.abs(lY-pY) <= medianLineDepth/2 )
                    merges.add(l);
            }
            //is p to the left of l?
            end = p.points.get(p.points.size()-1).x;
            start = l.points.get(0).x;
            if ( end <= start )
            {
                float pY = p.points.get(p.points.size()-1).y;
                float lY = l.points.get(0).y;
                if ( Math.abs(lY-pY) <= medianLineDepth/2 )
                    merges.add(l);
            }
        }
        for ( int i=0;i<merges.size();i++ )
        {
            Line l = merges.get(i);
            int index = lines.indexOf(l);
            l.mergeWith( lines.get(index-1) );
            lines.remove(index-1);
        }
    }
}