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
import tilt.image.geometry.Point;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import tilt.align.Matchup;
import tilt.image.convexhull.*;
import tilt.exception.*;
import tilt.Utils;

/**
 * Split a polygon in appropriate places. It does this not by looking for 
 * the biggest gaps, but by looking at all contiguous segments of the page 
 * image within the shape to be split, and then aligning them with the words 
 * that have been aligned to the shape. The best split-points will then be 
 * gaps that correspond most closely to word-gaps.
 * @author desmond
 */
public class Split 
{
    WritableRaster wr;
    Word[] words;
    Polygon shape;
    Line line;
    /**
     * Create a Splitter
     * @param l the line containing the polygon to split
     * @param shape the index of the shape that's too big
     * @param words th&gt; 1 words belong to the shape to be split
     * @param wr the raster of the cleaned image
     */
    Split( Line l, int shape, Word[] words, WritableRaster wr )
    {
        this.words = words;
        this.wr = wr;
        this.shape = l.getShape(shape);
        this.line = l;
    }
    /**
     * Convert an Integer ArrayList to a plain int array
     * @param list the list to convert
     * @return the int array
     */
    private int[] toIntArray( ArrayList<Integer> list )
    {
        int[] array = new int[list.size()];
        for ( int i=0;i<array.length;i++ )
            array[i] = list.get(i).intValue();
        return array;
    }
    /**
     * Check that the array of shapes is at least as long as that of the words
     * @param shapeArray an array of shape widths
     * @param widths an array of word-widths
     * @return an new array of shape widths or the same one
     */
    private int[] checkShapeArray( int[] shapeArray, int[] widths )
    {
        if ( shapeArray.length >= widths.length )
            return shapeArray;
        else
        {
            double total = 0;
            int[] newShapes = new int[widths.length];
            for ( int i=0;i<widths.length;i++ )
                total += widths[i];
            double scale = (double)shape.getBounds().width/total;
            for ( int i=0;i<newShapes.length;i++ )
                newShapes[i] = (int)Math.round(widths[i]*scale);
            return newShapes;
        }
    }
    /**
     * The gap array has to match the shape array. It should be the same.
     * @param gapArray the gap array as it is
     * @param shapeArray the shape array already adjusted
     * @return the remodeled gap array
     */
    private int[] checkGapArray( int[] gapArray, int [] shapeArray )
    {
        if ( gapArray.length >= shapeArray.length )
            return gapArray;
        else
        {
            int[] newGapArray = new int[shapeArray.length];
            for ( int i=0;i<gapArray.length;i++ )
                newGapArray[i] = gapArray[i];
            for ( int i=gapArray.length;i<newGapArray.length;i++ )
                newGapArray[i] = 2;
            return newGapArray;
        }
    }
    /**
     * Actually split the designated shape, storing the result directly in 
     * the line's shapes array.
     */
    void execute() throws SplitException
    {
        int shapeWidth = shape.getBounds().width;
        int shapeHeight = shape.getBounds().height;
        ArrayList<Integer> shapeWidths = new ArrayList<>();
        ArrayList<Integer> gapWidths = new ArrayList<>();
        int[] iArray = new int[1];
        int shapeLen = 0;
        int gapLen = 0;
        for ( int i=0;i<shapeWidth;i++ )
        {
            int blackPixels = 0;
            int x = i+shape.getBounds().x;
            for ( int j=0;j<shapeHeight;j++ )
            {
                int y = j+shape.getBounds().y;
                wr.getPixel( x, y, iArray );
                if ( iArray[0] == 0 )
                    blackPixels++;
            }
            if ( blackPixels > 0 )
            {
                if ( shapeLen > 0 && gapLen > 0 )
                {
                    gapWidths.add( new Integer(gapLen) );
                    gapLen = 0;
                }
                shapeLen++;
            }
            else if ( shapeLen > 0 )
            {
                shapeWidths.add( new Integer(shapeLen) );
                shapeLen = 0;
                gapLen = 1;
            }
            else
                gapLen++;
        }
        int[] shapeArray = toIntArray( shapeWidths );
        int[] gapArray = toIntArray( gapWidths );
        try
        {
            int[] widths = new int[words.length];
            for ( int i=0;i<widths.length;i++ )
                widths[i] = words[i].width();
            shapeArray = checkShapeArray( shapeArray, widths );
            gapArray = checkGapArray( gapArray, shapeArray );
            Matchup m = new Matchup( shapeArray, widths );
            int[][][] matches = m.align();
            ArrayList<Integer> splitArray = new ArrayList<>();
            // for each of the word matches, work out the gap, then split 
            // the polygon at that point, creating one polygon per word
            // running counters for all blobs, gaps
            int blob = 0;
            int gap = 0;
            int splitPos = shape.getBounds().x;
            for ( int i=0;i<matches.length;i++ )
            {
                int[] blobs = matches[i][1];
                for ( int j=0;j<blobs.length;j++ )
                {
                    splitPos += shapeArray[blob++];
                    if ( j < blobs.length-1&&gap<gapArray.length )
                        splitPos += gapArray[gap++];
                }
                if ( matches[i][0].length>0 )
                {
                    if ( gap < gapArray.length )
                        splitPos += gapArray[gap]/2;
                    splitArray.add( splitPos );
                }
                gap++;
            }
            // need at least 1 split
            if ( splitArray.size()> 1 )
                splitArray.remove(splitArray.size()-1);
            int[] splits = toIntArray( splitArray );
            Polygon[] shapes = split( splits );
            // replace existing shape with split portions
            int shapeIndex = line.getShapeIndex( shape );
            int i = 0;
            line.removeShape( shape );
            for ( Polygon pg : shapes )
                line.addShape( shapeIndex++, pg, words[i++] );
        }
        catch ( Exception e )
        {
            throw new SplitException( e );
        }
    }
    /**
     * Split the existing shape into several horizontal sections
     * @param splits the split-points
     * @return an array of split regions covering the same area as before
     * @throws SplitException
     */
    public Polygon[] split( int[] splits ) throws SplitException
    {
        SplitShape[] newShapes = new SplitShape[splits.length+1];
        ArrayList<Point> points = shape.toPoints();
        // create new shapes to generate split polygons
        int shapeRight = shape.getBounds().width+shape.getBounds().x;
        for ( int start=shape.getBounds().x,i=0;i<newShapes.length;i++ )
        {
            int splitPt = (i<splits.length)?splits[i]:shapeRight;
            newShapes[i] = new SplitShape(start,splitPt);
            start = splitPt;
        }
        // distribute points among shapes
        for ( int i=0;i<points.size();i++ )
        {
            Point p = points.get(i);
            int x = (int)Math.round(p.x);
            // determine which shape it falls into
            for ( int j=0;j<newShapes.length;j++ )
            {
                // determine where p goes
                if ( newShapes[j].wants(x) )
                {
                    newShapes[j].add( new Point2D(p.x,p.y) );
                    break;
                }
            }
        }
        // add split points
        for ( int i=0;i<newShapes.length;i++ )
        {
            if ( i==0 )
                newShapes[i].addRightIntersectPoints(this.shape);
            else if ( i == newShapes.length-1 )
                newShapes[i].addLeftIntersectPoints(this.shape);
            else
            {
                newShapes[i].addRightIntersectPoints(this.shape);
                newShapes[i].addLeftIntersectPoints(this.shape);
            }
        }
        // convert to polygons
        ArrayList<Polygon> polys = new ArrayList<>();
        for  ( int i=0;i<newShapes.length;i++ )
        {
            if ( newShapes[i].numPoints()> 3)
                polys.add( newShapes[i].getPoly() );
        }
        Polygon[] array = new Polygon[polys.size()];
        polys.toArray(array );
        return array;
    }
}
