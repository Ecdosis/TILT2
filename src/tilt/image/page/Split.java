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
import java.awt.Polygon;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Iterator;
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
                    if ( j < blobs.length-1 )
                        splitPos += gapArray[gap++];
                }
                if ( matches[i][0].length>0 )
                    splitArray.add( splitPos + gapArray[gap]/2 );
                gap++;
            }
            if ( splitArray.size()<= 1 )
                throw new SplitException("Not enough break points for split");
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
        ArrayList<Point2D>[] newPolys = new ArrayList[splits.length+1];
        Point2D[] points = Utils.polygonToPoints( shape );
        for ( int i=0;i<newPolys.length;i++ )
            newPolys[i] = new ArrayList<>();
        // examine each original point
        for ( int i=0;i<points.length;i++ )
        {
            Point2D p = points[i];
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
                if ( p.x() < rightSplit || (rightSplit==-1 && j==splits.length) )
                {
                    // so p belongs within shapes[j]
                    if ( i > 0 )
                    {
                        // examine previous point
                        Point2D q = points[i-1];
                        if ( q.x() < leftSplit )
                            newPolys[j].add( getDiagonalPoint(q,p,leftSplit) );
                        else if ( rightSplit!=-1 && q.x() >= rightSplit )
                            newPolys[j].add(getDiagonalPoint(p,q,rightSplit) );
                    }
                    newPolys[j].add( p );
                    if ( i < points.length-1 )
                    {
                        Point2D q = points[i+1];
                        if ( q.x() < leftSplit )
                        {
                            Point2D s = getDiagonalPoint(q,p,leftSplit);
                            newPolys[j].add(s);
                            newPolys[j-1].add(new Point2D(s.x()-5,s.y()));
                        }
                        else if ( rightSplit!=-1 && q.x() >= rightSplit )
                        {
                            Point2D s = getDiagonalPoint(p,q,rightSplit);
                            newPolys[j].add(s );
                            newPolys[j+1].add(new Point2D(s.x()+5,s.y()));
                        }
                    }
                    break;
                }
            }
        }
        // now close the generated shapes
        for ( int i=0;i<newPolys.length;i++ )
        {
            ArrayList<Point2D> r = newPolys[i];
            if ( r.size()>0 )
            {
                Point2D p = r.get(0);
                Point2D q = r.get(r.size()-1);
                if ( !p.equals(q) )
                    r.add( new Point2D(p.x(),p.y()) );
            }
        }
        // convert to polygons
        Polygon[] polys = new Polygon[newPolys.length];
        for ( int i=0;i<polys.length;i++ )
        {
            polys[i] = new Polygon();
            Point2D[] newPolyArray = new Point2D[newPolys[i].size()];
            newPolys[i].toArray(newPolyArray);
            GrahamScan gs = new GrahamScan( newPolyArray );
            Iterable<Point2D> trimmedPts = gs.hull();
            Iterator<Point2D> iter = trimmedPts.iterator();
            while ( iter.hasNext() )
            {
                Point2D pt = iter.next();
                polys[i].addPoint( (int)Math.round(pt.x()), 
                    (int)Math.round(pt.y()) );
            }
        }
        return polys;
    }
    /**
     * Add a new point at the given x-offset between 2 points
     * @param p the left point
     * @param q the right point
     * @param split the split x-position
     */
    private Point2D getDiagonalPoint( Point2D p, Point2D q, int split ) 
        throws SplitException
    {
        Point2D u = null;
        if ( p.x() < q.x() && split > p.x() )
        {
            double deltaSplitX = (float)(split-p.x());
            double deltaX = (float)(q.x()-p.x());
            double ratio = deltaSplitX/deltaX;
            double deltaY = (q.y()-p.y());
            u = new Point2D( split, p.y()+(ratio*deltaY) );
        }
        else
            throw new SplitException("p.x >= q.x during split");
        return u;
    }
}
