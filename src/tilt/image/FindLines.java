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
import java.util.ArrayList;

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
    float[][] cusum;
    int[][] peaks;
    int height;
    int width;
    static float SCALE_RATIO = 0.006f;
    /** number of vertical or horizontal pixels to combine */
    int scale;
    public FindLines( WritableRaster wr )
    {
        scale = Math.round(wr.getWidth()*SCALE_RATIO);
        height = (wr.getHeight()+scale-1)/scale;
        width = (wr.getWidth()+scale-1)/scale;
        compacted = new int[width][height];
        hAverages = new float[height];
        vAverages = new float[width];
        peaks = new int[width][];
        cusum = new float[width][height];
        compact( wr );
        // average the rows
        average = 0.0f;
        for ( int y=0;y<height;y++ )
        {
            hAverages[y] = averageRow(y);
            average += hAverages[y];
        }
        // calculate the cusums for each row
        average /= height;
        for ( int y=0;y<height;y++ )
        {
            cusum[0][y] = 0.0f;
            for ( int x=1;x<width;x++ )
            {
                cusum[x][y] = cusum[x-1][y]+(compacted[x][y]-hAverages[y]);
            }
        }
        // average the columns
        for ( int x=0;x<width;x++ )
        {
            vAverages[x] = averageCol(x);
        }
        // calculate the cusums for each column
        for ( int x=0;x<width;x++ )
        {
            cusum[x][0] = 0.0f;
            for ( int y=1;y<height;y++ )
            {
                cusum[x][y] = cusum[x][y-1]+(cusum[x][y]-vAverages[x]);
            }
        }
        // find peaks
        ArrayList<Integer> peaks = new ArrayList<Integer>();
        int totalPeaks = 0;
        for ( int y=0;y<height;y++ )
        {
            float current = 0.0f;
            int index = -1;
            for ( int x=0;x<width;x++ )
            {
                if ( cusum[x][y] > average )
                {
                    if ( cusum[x][y] > current )
                    {
                        current = cusum[x][y];
                        index = y;
                    }
                    else if ( cusum[x][y] != current )
                    {
                        peaks.add(new Integer(index) );
                        current = 0.0f;
                        index = -1;
                    }
                }
                else if ( current > 0.0f )
                {
                    peaks.add(new Integer(index) );
                    current = 0.0f;index = -1;
                }
            }
            totalPeaks += peaks.size();
            peaks.clear();
        }
        System.out.println("Average number of peaks per column="+(totalPeaks/width));
        // now for each column find the peaks, record them
        // as they are for each coloumn, trace them with the 
        // closest matching values, allowing some to peter out 
        // or new ones to start
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
                    compacted[x/scale][y/scale]++;
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
     * Compute the average of a column
     * @param row the col number
     * @return the average cusum value for the col
     */
    float averageCol( int col )
    {
        float total = 0.0f;
        for ( int i=0;i<height;i++ )
            total += cusum[col][i];
        return total/(float)height;
    }
}
