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
import java.util.ArrayList;
import tilt.image.page.Page;

/**
 * Find the lines based on a Gaussian-blurred image
 * @author desmond
 */
public class FindLinesBlurred {
    /** width of vertical strips */
    static float H_SCALE_RATIO = 0.04f;
    /** the shade to reduce the blackness to */
    static int LIGHT_SHADE = 128;
    /** range of moving average smoothing +- N*/
    static int SMOOTH_N = 9;
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
    /**
     * Detect lines n the basis of an already blurred image
     * @param src the source blurred image
     * @param numWords the number of words on the page (needed by page object)
     * @throws Exception 
     */
    public FindLinesBlurred( BufferedImage src, int numWords ) throws Exception
    {
        this.src = src;
        WritableRaster wr = src.getRaster();
        hScale = Math.round(wr.getWidth()*H_SCALE_RATIO);
        width = (wr.getWidth()+hScale-1)/hScale;
        peaks = new ArrayList[width];
        average = computeAverage( wr );
        for ( int i=0;i<width;i++)
            peaks[i] = findPeaks(wr,i);
        lightenImage( wr );
        // now draw the lines
        page = new Page( peaks, hScale, 1, numWords );
        page.refineRight( wr, hScale, 1, LIGHT_SHADE );
        page.refineLeft( wr, hScale, 1, LIGHT_SHADE );
        page.finalise( wr );
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
     * Compute the average pixel intensity across the whole image (expensive)
     * @return the averages a float
     */
    private float computeAverage( WritableRaster wr )
    {
        float total = 0.0f;
        int[] iArray = new int[1];
        int w = wr.getWidth();
        int h = wr.getHeight();
        for ( int x=0;x<w;x++ )
        {
            for ( int y=0;y<h;y++ )
            {
                wr.getPixel(x,y,iArray);
                total += iArray[0];
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
        int xEnd = xStart+stripWidth;
        float[] totals = new float[wr.getHeight()];
        int[] iArray = new int[1];
        for ( int y=0;y<wr.getHeight();y++ )
        {
            for ( int x=xStart;x<xEnd;x++ )
            {
                wr.getPixel(x,y,iArray);
                totals[y]+=iArray[0];
            }
            totals[y] /= stripWidth;
        }
        float old_slope;
        float slope = 0.0f;
        ArrayList<Integer> list= new ArrayList<>();
        totals = smooth( totals );
        for ( int y=SMOOTH_N;y<wr.getHeight();y++ )
        {
            float dy = (totals[y]-totals[y-SMOOTH_N]);
            float dx = (float)SMOOTH_N;
            old_slope = slope;
            slope = dy/dx;
            if ( slope >0.0f && old_slope <= 0.0f )
            {
                int peak = y-(SMOOTH_N/2);
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
    private float[] smooth( float[] data )
    {
        int limit = data.length-SMOOTH_N;
        float[] copy = new float[data.length];
        // just copy the ends over without smoothing
        for ( int i=0;i<SMOOTH_N;i++ )
            copy[i]= data[i];
        for ( int i=data.length-1;i>data.length-SMOOTH_N;i-- )
            copy[i] = data[i];
        for ( int i=SMOOTH_N;i<limit;i++ )
        {
            int sum = 0;
            for ( int j=i-SMOOTH_N;j<=i+SMOOTH_N;j++ )
                sum += data[j];
            copy[i] = sum/(2*SMOOTH_N+1);
        }
        return copy;
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
}
