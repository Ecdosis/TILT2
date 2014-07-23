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
import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import tilt.align.Matchup;
import tilt.image.FastConvexHull;
import tilt.exception.*;
import tilt.Utils;
import tilt.image.page.Word;

/**
 * Split a polygon in appropriate places
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
            for ( int j=0;j<shapeHeight;j++ )
            {
                int x = i+shape.getBounds().x;
                int y = j+shape.getBounds().y;
                wr.getPixel( x, y, iArray );
                if ( iArray[0] == 0 )
                    blackPixels++;
            }
            if ( blackPixels > 0 )
            {
                if ( gapLen > 0 )
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
            Matchup m = new Matchup( shapeArray, widths );
            int[][][] matches = m.align();
            ArrayList<Integer> splitArray = new ArrayList<>();
            // for each of the word matches, work out the gap, then split 
            // the polygon at that point, creating one polygon per word
            // running counters for all blobs, gaps
            int blob = 0;
            int gap = 0;
            for ( int i=0;i<matches.length-1;i++ )
            {
                int[] blobs = matches[i][1];
                int blobWidth = 0;
                for ( int j=0;j<blobs.length;j++ )
                {
                    blobWidth += shapeArray[blob++];
                    if ( j < blobs.length-1 )
                        blobWidth += gapArray[gap++];
                }
                splitArray.add( blobWidth + gapArray[gap++]/2 );
            }
            int[] splits = toIntArray( splitArray );
            Polygon[] shapes = split( splits );
            // replace existing shape with split portions
            int shapeIndex = line.getShapeIndex( shape );
            int i = 0;
            line.removeShape( shape );
            for ( Polygon pg : shapes )
                line.addShape( shapeIndex, pg, words[i++] );
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
        ArrayList<Point>[] newPolys = new ArrayList[splits.length+1];
        ArrayList<Point> points = Utils.polygonToPoints( shape );
        for ( int i=0;i<newPolys.length;i++ )
            newPolys[i] = new ArrayList<>();
        // examine each original point
        for ( int i=0;i<points.size();i++ )
        {
            Point p = points.get( i );
            // determine which shape it falls into
            for ( int j=0;j<=splits.length;j++ )
            {
                int leftSplit = -1;
                int rightSplit = -1;
                if ( j==splits.length )
                    leftSplit = splits[j-1];
                else if ( j==0 )
                    rightSplit = splits[0];
                else
                {
                    leftSplit = splits[j-1];
                    rightSplit = splits[j];
                }
                // determine where p goes
                if ( p.x < rightSplit || (rightSplit==-1 && j==splits.length) )
                {
                    // so p belongs within shapes[j]
                    if ( i > 0 )
                    {
                        // examine previous point
                        Point q = points.get( i-1 );
                        if ( q.x < leftSplit )
                            newPolys[j].add( getDiagonalPoint(q,p,leftSplit) );
                        else if ( rightSplit!=-1 && q.x >= rightSplit )
                            newPolys[j].add(getDiagonalPoint(p,q,rightSplit) );
                    }
                    newPolys[j].add( p );
                    if ( i < points.size()-1 )
                    {
                        Point q = points.get( i+1 );
                        if ( q.x < leftSplit )
                            newPolys[j].add(getDiagonalPoint(q,p,leftSplit));
                        else if ( rightSplit!=-1 && q.x >= rightSplit )
                            newPolys[j].add(getDiagonalPoint(p,q,rightSplit) );
                    }
                    break;
                }
            }
        }
        // now close the generated shapes
        for ( int i=0;i<newPolys.length;i++ )
        {
            ArrayList<Point> r = newPolys[i];
            if ( r.size()>0 )
            {
                Point p = r.get(0);
                Point q = r.get(r.size()-1);
                if ( !p.equals(q) )
                    r.add( new Point(p.x,p.y) );
            }
        }
        // convert to polygons
        Polygon[] polys = new Polygon[newPolys.length];
        for ( int i=0;i<polys.length;i++ )
        {
            polys[i] = new Polygon();
            ArrayList<Point> trimmedPts = FastConvexHull.execute(newPolys[i]);
            for ( Point pt : trimmedPts )
                polys[i].addPoint( pt.x, pt.y );
        }
        return polys;
    }
    /**
     * Add a new point at the given x-offset between 2 points
     * @param p the left point
     * @param q the right point
     * @param split the split x-position
     */
    private Point getDiagonalPoint( Point p, Point q, int split ) 
        throws SplitException
    {
        Point u = null;
        if ( p.x < q.x && split > p.x )
        {
            float deltaSplitX = (float)(split-p.x);
            float deltaX = (float)(q.x-p.x);
            float ratio = deltaSplitX/deltaX;
            float deltaY = (float)(q.y-p.y);
            u = new Point( split, p.y+Math.round(ratio*deltaY) );
        }
        else
            throw new SplitException("p.x >= q.x during split");
        return u;
    }
}
