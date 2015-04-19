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

/**
 * Find the lines based on a Gaussian-blurred image
 * @author desmond
 */
public class FindLinesBlurred 
{
    int LIGHT_SHADE = 128;
    int smoothN;
    BufferedImage src;
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
    public FindLinesBlurred( BufferedImage src, Rectangle cropRect, 
        int numWords, Options options ) throws Exception
    {
        this.opts = options;
        // first blur the image appropriately
        int blur = Math.round(options.getFloat(Options.Keys.blurForLines)
            *cropRect.height);
        BlurImage bi = new BlurImage(src,blur);
        BufferedImage blurred = bi.blur();
        WritableRaster wr = blurred.getRaster();
        //WritableRaster wr = src.getRaster();
        hScale = Math.round(wr.getWidth()
            *options.getFloat(Options.Keys.verticalSliceSize));
        width = (wr.getWidth()+hScale-1)/hScale;
        smoothN = Math.round(opts.getFloat(Options.Keys.smoothN)*cropRect.height);
        peaks = new ArrayList[width];
        average = computeAverage( wr );
        for ( int i=0;i<width;i++)
            peaks[i] = findPeaks(wr,i);
        if ( options.getBoolean(Options.Keys.test) )
            lightenImage( wr );
        // now draw the lines
        page = new Page( peaks, hScale, 1, numWords, options );
        page.refineRight( wr, hScale, 1, LIGHT_SHADE );
        page.refineLeft( wr, hScale, 1, LIGHT_SHADE );
        page.finalise( wr );
        if ( options.getBoolean(Options.Keys.test) )
            page.draw( src.getGraphics() );
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
     * Ensure that the peak really is the darkest point of the peak
     * @param totals the total pixel intensities in the strip
     * @param peak the approximate peak position
     * @return the precise peak
     */
//    private int refinePeak( float[] totals, int peak )
//    {
//        int best = peak;
//        int left = peak-1;
//        while ( left >=0 && totals[left] < totals[peak] )
//            left--;
//        int right = peak+1;
//        while ( right < totals.length && totals[right] < totals[peak] )
//            right++;
//        if ( left >= 0 )
//        {
//            if ( totals[left] < totals[peak] )
//                best = left;
//        }
//        if ( right < totals.length )
//        {
//            if ( totals[right] < totals[best] )
//                best = right;
//        }
//        return best;
//    }
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
     * Smooth the array using a moving average
     * ys(i)=1/(2N+1)(y(i+N)+y(i+N−1)+...+y(i−N))
     * @param data the array to smooth, unchanged
     * @return the smoothed version of the original data
     */
//    private float[] smooth( float[] data )
//    {
//        int limit = data.length-smoothN;
//        float[] copy = new float[data.length];
//        // just copy the ends over without smoothing
//        for ( int i=0;i<smoothN;i++ )
//            copy[i]= data[i];
//        for ( int i=data.length-1;i>data.length-smoothN;i-- )
//            copy[i] = data[i];
//        for ( int i=smoothN;i<limit;i++ )
//        {
//            int sum = 0;
//            for ( int j=i-smoothN;j<=i+smoothN;j++ )
//                sum += data[j];
//            copy[i] = sum/(2*smoothN+1);
//        }
//        return copy;
//    }
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
}
