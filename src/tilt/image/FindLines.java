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

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.Rectangle;
import java.util.ArrayList;
import tilt.image.page.Page;
import tilt.handler.post.Options;
import tilt.image.geometry.Point;
import tilt.image.page.Line;
import tilt.image.geometry.Polygon;
import tilt.image.page.QuadTree;
import tilt.image.page.diff.Diff;
import tilt.image.page.diff.Matrix;

/**
 * Find the lines based on a Gaussian-blurred image
 * @author desmond
 */
public class FindLines 
{
    int LIGHT_SHADE = 128;
    int smoothN;
    BufferedImage src;
    WritableRaster  wr;
    WritableRaster wrOrig;
    int shapeID;
    /** average image pixel density*/
    float average;
    /** width of image in hscale units */
    int width;
    /** array of arraylists of black pixel peaks */
    ArrayList[] peaks;
    /** the page object where we store and draw the lines */
    Page page;
    /** number of horizontal pixels to combine */
    int hScale;
    Options opts;
    /**
     * Detect lines n the basis of an already blurred image
     * @param src the source blurred image
     * @param numWords the number of words on the page (needed by page object)
     * @throws Exception 
     */
    public FindLines( BufferedImage src, Rectangle cropRect, 
        int numWords, Options options ) throws Exception
    {
        this.opts = options;
        this.shapeID = 1;
        this.src = src;
        // first blur the image appropriately
        int blur = Math.round(options.getFloat(Options.Keys.blurForLines)
            *cropRect.height);
        BlurImage bi = new BlurImage(src,blur);
        BufferedImage blurred = bi.blur();
        this.wr = blurred.getRaster();
        this.wrOrig = src.getRaster();
        hScale = Math.round(wr.getWidth()
            *options.getFloat(Options.Keys.verticalSliceSize));
        width = (wr.getWidth()+hScale-1)/hScale;
        smoothN = Math.round(opts.getFloat(Options.Keys.smoothN)*cropRect.height);
        peaks = new ArrayList[width];
        average = computeAverage( wr );
        for ( int i=0;i<width;i++)
            peaks[i] = findPeaks(wr,i);
        // now draw the lines
        page = new Page( peaks, hScale, 1, numWords, options, cropRect );
        page.sortLines();
        prune(cropRect);
        if ( options.getBoolean(Options.Keys.test) )
            lightenImage( wrOrig );
        page.finalise( wr );
        if ( options.getBoolean(Options.Keys.test) )
            page.drawLines( src.getGraphics() );
    }
    /**
     * Lighten an image in preparation for drawing on top of it
     * @param wr the raster of the image
     */
    private void lightenImage( WritableRaster wr )
    {
        int[] iArray = new int[1];
        for ( int y=0;y<wr.getHeight();y++ )
        {
            for ( int x=0;x<wr.getWidth();x++ )
            {
                wr.getPixel( x, y, iArray );
                if ( iArray[0] < LIGHT_SHADE )
                {
                    iArray[0] = LIGHT_SHADE;
                    wr.setPixel( x, y, iArray );
                }
            }
        }
    }
    /**
     * Compute the average pixel intensity across the whole image
     * @return the averages a float
     */
    private float computeAverage( WritableRaster wr )
    {
        float total = 0.0f;
        int w = wr.getWidth();
        int h = wr.getHeight();
        int[] iArray = new int[w];
        for ( int y=0;y<h;y++ )
        {
            wr.getPixels(0,y,w,1,iArray);
            for ( int x=0;x<w;x++ )
            {
                total += iArray[x];
            }
        }
        return (float)total/(float)(h*w);
    }
    /**
     * Collapse the pixels in a vertical strip horizontally, look for peaks
     * @param wr the entire raster
     * @param the index of the strip of at most hScale pixels
     * @return an arraylist of peaks
     */
    private ArrayList findPeaks( WritableRaster wr, int strip )
    {
        int stripWidth = ((strip+1)*hScale<wr.getWidth())?hScale:wr.getWidth()
            -(strip*hScale);
        int xStart = strip*hScale;
        float[] totals = new float[wr.getHeight()];
        int[] iArray = new int[stripWidth];
        for ( int y=0;y<wr.getHeight();y++ )
        {
            wr.getPixels(xStart,y,stripWidth,1,iArray);
            for ( int x=0;x<stripWidth;x++ )
            {
                totals[y] += iArray[x];
            }
            totals[y] /= stripWidth;
        }
        float old_slope;
        float slope = 0.0f;
        ArrayList<Integer> list= new ArrayList<>();
        //totals = smooth( totals );
        for ( int y=smoothN;y<wr.getHeight();y++ )
        {
            float dy = (totals[y]-totals[y-smoothN]);
            float dx = (float)smoothN;
            old_slope = slope;
            slope = dy/dx;
            if ( slope >0.0f && old_slope <= 0.0f )
            {
                int peak = y-(smoothN)/2;
                //int peak = refinePeak(totals,y-(smoothN/2));
                if ( totals[peak] < average )
                    list.add(new Integer(peak));
            }
        }
        return list;
    }
    /**
     * Get the completed page object
     * @return a {@link Page} object
     */
    Page getPage()
    {
        return page;
    }
    /**
     * Get the per pixel average density of the twotone image
     * @return a float
     */
    float getPPAverage()
    {
        return average;
    }
    int newShapeID()
    {
        return this.shapeID++;
    }
    /**
     * Ensure that each blob belongs primarily to one line only. Remove 
     * lines that don't have any assigned blobs.
     */
    public void prune( Rectangle r )
    {
        QuadTree qt = new QuadTree(r.x, r.y, r.width, r.height );
        WritableRaster dirty = src.copyData(null);
        ArrayList<Line> lines = page.getLines();
        int[] dArray = new int[1];
        Blob.setToWhite(dirty);
        for ( int i=0;i<lines.size();i++ )
        {
            Line line = lines.get(i);
            Point[] points = line.getPoints();
            boolean lastWasBlack=false;
            for ( int j=1;j<points.length-1;j++ )
            {
                Point last = points[j-1];
                Point curr = points[j];
                float yDiff = curr.y-last.y;
                float xDiff = curr.x-last.x;
                for ( int x=Math.round(last.x);x<curr.x;x++ )
                {
                    int y = Math.round(last.y)
                        +Math.round(((x-last.x)/xDiff)*yDiff);
                    wrOrig.getPixel( x, y, dArray);
                    if ( dArray[0]==0 && !lastWasBlack )
                    {
                        Point q = new Point(x,y);
                        Polygon shape = qt.pointInPolygon( q );
                        if ( shape != null )
                        {
                            if ( !line.hasShapeByID(shape.ID) )
                                line.add( shape );
                        }
                        else
                        {
                            Blob b = new Blob(wrOrig,opts,null);
                            b.save(dirty, wrOrig, q.toAwt());
                            if ( b.hasHull() )
                            {
                                shape = b.toPolygon();
                                shape.toPoints();
                                qt.addPolygon( shape );
                                line.add( shape );
                                shape.setID( newShapeID() );
                            }
                        }
                        lastWasBlack = true;
                    }
                    if ( dArray[0]!= 0 )
                        lastWasBlack = false;
                }
            }
        }
        // now we have all the identifiable shapes on the page in qt and they 
        // are assigned to lines, maybe more than one line to each shape. Now 
        // we have to identify RUNS of shapes in each line that are also on 
        // the next line - and decide to which line they should belong
        for ( int i=1;i<lines.size();i++ )
        {
            Line prev = lines.get(i-1);
            Line curr = lines.get(i);
            int[] prevIDs = prev.getShapeIDs();
            int[] currIDs = curr.getShapeIDs();
            Diff[] diffs = Matrix.computeRuns( prevIDs, currIDs );
            for ( int j=0;j<diffs.length;j++ )
            {
                int[] aligned = new int[diffs[j].newLen];
                int kEnd = diffs[j].newOffset+diffs[j].newLen;
                for ( int m=0,k=diffs[j].newOffset;k<kEnd;k++ )
                {
                    aligned[m++] = prevIDs[k];
                }
                Point centroid = prev.getCentroid( aligned );
                float distA = prev.closestDistTo( centroid );
                float distB = curr.closestDistTo( centroid );
                if ( distA < distB && prev.countShapes() > aligned.length )
                {
                    for ( int k=0;k<aligned.length;k++ )
                        curr.removeShapeByID(aligned[k]);
                }
                else 
                {
                    for ( int k=0;k<aligned.length;k++ )
                        prev.removeShapeByID(aligned[k]);
                }
            }
        }
        int i = 0;
        while ( i < lines.size() )
        {
            Line l = lines.get(i);
            if ( l.countShapes() == 0 )
                lines.remove(i);
            else
            {
                l.refineRight( wrOrig, hScale, 1, 0 );
                l.refineLeft( wrOrig, hScale, 1, 0 );
                if ( l.countPoints()==0 )
                    lines.remove(i);
                else
                    i++;
            }
        }
        for ( Line l : lines )
            l.resetShapes();
    }
}
