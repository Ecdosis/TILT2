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
    float[] vAverages;
    float average;
    int[][] compacted;
    float[][] cusum;
    int[][] peaks;
    int height;
    int width;
    static float SCALE_RATIO = 0.005f;
    /** number of vertical or horizontal pixels to combine */
    int scale;
    public FindLines( WritableRaster wr )
    {
        scale = Math.round(wr.getWidth()/SCALE_RATIO);
        height = wr.getHeight()/scale;
        width = wr.getWidth()/scale;
        compacted = new int[height][width];
        vAverages = new float[width];
        peaks = new int[width][];
        cusum = new float[height][width];
        // average the columns
        for ( int x=0;x<width;x++ )
        {
            vAverages[x] = averageCol(x);
            average += vAverages[x];
        }
        // calculate the cusum
        average /= width;
        for ( int x=0;x<width;x++ )
        {
            cusum[0][x] = 0.0f;
            for ( int y=1;y<height;y++ )
            {
                cusum[y][x] = cusum[y-1][x]+(cusum[y][x]-vAverages[x]);
            }
        }
        // find peaks
        ArrayList<Integer> peaks = new ArrayList<Integer>();
        int totalPeaks = 0;
        for ( int x=0;x<width;x++ )
        {
            float current = 0.0f;
            for ( int y=0;y<height;y++ )
            {
                if ( cusum[y][x] > average )
                {
                    if ( cusum[y][x] > current )
                        current = cusum[y][x];
                    else if ( current != 0.0f && cusum[y][x] < current )
                    {
                        peaks.add(new Integer(y) );
                        current = 0.0f;
                    }
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
    void compact( WritableRaster wr )
    {
    }
    /**
     * Compute the average of a row
     * @param wr the raster with the pixels
     * @param row the row number
     * @return the average pixel intensity for the row
     */
    float averageRow( WritableRaster wr, int row )
    {
        int total = 0;
        int[] iArray = new int[1];
        int rowWidth = wr.getWidth()/SCALE;
        for ( int i=0;i<rowWidth;i++ )
        {
            for ( int y=0;y<SCALE;y++ )
            {
                for ( int x=0;x<SCALE;x++ )
                {
                    wr.getPixel(i*SCALE+x,row*SCALE+y,iArray );
                    total += (iArray[0]==0)?1:0;
                }
            }
        }
        return (float)total/(float)rowWidth;
    }
    /**
     * Compute the average of a row
     * @param col the column number
     * @return the average pixel intensity for the column
     */
    float averageCol( int col )
    {
        float total = 0.0f;
        for ( int i=0;i<height;i++ )
            total += cusum[i][col];
        return total/(float)height;
    }
}
