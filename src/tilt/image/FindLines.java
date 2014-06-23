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

import java.awt.image.WritableRaster;

/**
 * Find lines in manuscripts/printed text when tilted/warped etc.
 * @author desmond
 */
public class FindLines 
{
    float averagePixelDensity;
    float[] hAverages;
    float[] vAverages;
    float[][] cusum;
    int height;
    int width;
    public FindLines( WritableRaster wr )
    {
        height = wr.getHeight();
        width = wr.getWidth();
        hAverages = new float[height];
        vAverages = new float[height];
        for ( int i=0;i<height;i++ )
            hAverages[i] = averageRow(wr,i);
        cusum = new float[width][height];
        for ( int y=0;y<height;y++ )
        {
            cusum[0][y] = 0.0f;
            for ( int x=1;x<width;x++ )
                cusum[x][y] = cusum[x-1][y]-hAverages[y];
        }
        for ( int x=0;x<width;x++ )
            vAverages[x] = averageCol(x);
        for ( int x=0;x<width;x++ )
        {
            cusum[x][0] = 0.0f;
            for ( int y=1;y<height;y++ )
            {
                cusum[x][y] = cusum[x][y-1]-vAverages[x];
            }
        }
        // now for each column find the peaks, record them
        // as they for each coloumn, trace them with the 
        // closest matching values, allowing some to peter out 
        // or new ones to start
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
        for ( int i=0;i<wr.getWidth();i++ )
        {
            wr.getPixel(i,row,iArray );
            total += iArray[0];
        }
        return (float)total/(float)wr.getWidth();
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
            total += cusum[col][i];
        return total/(float)height;
    }
}
