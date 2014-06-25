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
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.Graphics2D;
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
    static float V_SCALE_RATIO = 0.005f;
    static float H_SCALE_RATIO = 0.02f;
    /** number of vertical or horizontal pixels to combine */
    int hScale;
    int vScale;
    BufferedImage src;
    public FindLines( BufferedImage src )
    {
        WritableRaster wr = src.getRaster();
        this.src = src;
        hScale = Math.round(wr.getWidth()*H_SCALE_RATIO);
        vScale = Math.round(wr.getHeight()*V_SCALE_RATIO);
        height = (wr.getHeight()+vScale-1)/vScale;
        width = (wr.getWidth()+hScale-1)/hScale;
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
        ArrayList[] cols = new ArrayList[width];
        for ( int x=0;x<width;x++ )
        {
            cols[x] = new ArrayList<Integer>();
            for ( int y=0;y<height;y++ )
            {
                float current = 0.0f;
                int index = -1;
                if ( cusum[x][y] > average )
                {
                    if ( cusum[x][y] > current )
                    {
                        current = cusum[x][y];
                        index = y;
                    }
                    else if ( cusum[x][y] != current )
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
        Graphics2D g2 = src.createGraphics();
        for ( int i=0;i<cols.length-1;i++ )
        {
            ArrayList col1 = cols[i];
            ArrayList col2 = cols[i+1];
            int j = 0;
            int k = 0;
            while ( j<col1.size() && k<col2.size() )
            {
                int kBest = findBestMatch(col2,j,k);
                int jBest = findBestMatch(col1,k,j);
                int cRes1 = compareRow(j,kBest);
                int cRes2 = compareRow(k,jBest);
                if ( cRes1 > cRes2 )
                {
                    drawLine( col1.get(jBest), col2.get(k) );
                    j = jBest+1;
                    k++;
                }
                else
                {
                    drawLine( col1.get(j), col2.get(kBest) );
                    j++;
                    k = kBest+1;
                }
            }
        }
        // now for each column find the peaks, record them
        // as they are for each coloumn, trace them with the 
        // closest matching values, allowing some to peter out 
        // or new ones to start
    }
//    
//                if ( row1 == row2 )
//                {
//                    g2.drawLine( 
//                        (hScale/2)+hScale*i,
//                        (vScale/2)+vScale*j,
//                        (hScale/2)+hScale*(i+1),
//                        (vScale/2)+vScale*row2
//                    );
//                    j++;
//                    k++;
//                }
//                else
    /**
     * Given a value in one list, find the closest match in another list
     * @param list the list to search
     * @param value the value in the other list, fixed
     * @param from start from this index in list
     * @return 
     */
    private int findBestMatch( ArrayList list, int value, int from )
    {
        int m = from+1;
        int best = ((Integer)list.get(from)).intValue();
        int row;
        do
        {
            row = ((Integer)list.get(m)).intValue();
            m++;
        } while ( Math.abs(row-value) < Math.abs(best-value));
        return row;
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
