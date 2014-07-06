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
import tilt.image.page.Page;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
/**
 * Find lines in manuscripts/printed text when tilted/warped etc.
 * @author desmond
 */
public class FindLines 
{
    float[] hAverages;
    float[] vAverages;
    float average;
    int[][] compacted;
    int[][] peaks;
    int height;
    int width;
    /** colour of shaded background pixels */
    static int LIGHT_SHADE = 128;
    static float V_SCALE_RATIO = 0.005f;
    static float H_SCALE_RATIO = 0.04f;
    static int SMOOTHING = 3;
    /** number of vertical or horizontal pixels to combine */
    int hScale;
    int vScale;
    BufferedImage src;
    Page page;
    public FindLines( BufferedImage src ) throws Exception
    {
        WritableRaster wr = src.getRaster();
        this.src = src;
        hScale = Math.round(wr.getWidth()*H_SCALE_RATIO);
        vScale = Math.round(wr.getHeight()*V_SCALE_RATIO);
        if ( vScale==0 )
            vScale = 1;
        height = (wr.getHeight()+vScale-1)/vScale;
        width = (wr.getWidth()+hScale-1)/hScale;
        compacted = new int[width][height];
        hAverages = new float[height];
        vAverages = new float[width];
        peaks = new int[width][];
        compact( wr );
        // average the rows
        average = 0.0f;
        for ( int y=0;y<height;y++ )
        {
            hAverages[y] = averageRow(y);
            average += hAverages[y];
        }
        average /= height;
        // lighten image
        int[] iArray = new int[1];
        for ( int y=0;y<wr.getHeight();y++ )
        {
            for ( int x=0;x<wr.getWidth();x++ )
            {
                wr.getPixel( x, y, iArray );
                if ( iArray[0] == 0 )
                {
                    iArray[0] = LIGHT_SHADE;
                    wr.setPixel( x, y, iArray );
                }
            }
        }
        // smooth cols
//        for ( int x=0;x<width;x++ )
//        {
//            for ( int y=0;y<height;y++ )
//            {
//                compacted[x][y] = smooth(x,y);
//            }
//        }
        // find peaks
        ArrayList[] cols = new ArrayList[width];
        for ( int x=0;x<width;x++ )
        {
            float current = 0.0f;
            int index = -1;
            cols[x] = new ArrayList<Integer>();
            for ( int y=0;y<height;y++ )
            {
                if ( compacted[x][y] > average )
                {
                    if ( compacted[x][y] > current )
                    {
                        current = compacted[x][y];
                        index = y;
                    }
                    else if ( compacted[x][y] != current )
                    {
                        cols[x].add(new Integer(index) );
                        current = 0.0f;
                        index = -1;
                    }
                }
                else if ( current > 0.0f )
                {
                    cols[x].add(new Integer(index) );
                    current = 0.0f;
                    index = -1;
                }
            }
        }
        // now draw the lines
        page = new Page( cols, hScale, vScale );
        page.refineRight( wr, hScale, vScale, LIGHT_SHADE );
        page.refineLeft( wr, hScale, vScale, LIGHT_SHADE );
        page.draw( src.getGraphics() );
    }
    /**
     * Smooth the pixel densities in a column
     * @param x the column index in compacted
     * @param y the current row position
     * @return the smoothed value for that cell in compacted
     */
    int smooth( int x, int y )
    {
        float den = 1.0f;
        int half = (SMOOTHING-1)/2;
        int top = (y-half<0)?0:y-half;
        int bot = (y+half>=height)?height-1:half+y;
        float total = compacted[x][y];
        for ( int i=top;i<=bot;i++ )
        {
            total += compacted[x][i];
            den += 1.0f;
        }
        return Math.round(total/den);
    }
    /**
     * Collapse the whole image by merging squares of pixels
     * @param wr the raster with the original pixels
     */
    void compact( WritableRaster wr )
    {
        int[] iArray = new int[1];
        int h = wr.getHeight();
        int w = wr.getWidth();
        for ( int y=0;y<h;y++ )
        {
            for ( int x=0;x<w;x++ )
            {
                wr.getPixel( x, y, iArray );
                if ( iArray[0] == 0 )
                    compacted[x/hScale][y/vScale]++;
            }
        }
    }
    /**
     * Compute the average of a row
     * @param row the row number
     * @return the average pixel intensity for the row
     */
    float averageRow( int row )
    {
        float total = 0.0f;
        for ( int i=0;i<width;i++ )
            total += compacted[i][row];
        return total/(float)width;
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
        float nPixels = hScale*vScale;
        return average / nPixels;
    }
}
