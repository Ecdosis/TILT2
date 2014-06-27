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
import java.awt.Graphics;
import java.awt.Color;
import tilt.image.matchup.*;
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
    static float V_SCALE_RATIO = 0.005f;
    static float H_SCALE_RATIO = 0.04f;
    /** number of vertical or horizontal pixels to combine */
    int hScale;
    int vScale;
    BufferedImage src;
    public FindLines( BufferedImage src ) throws Exception
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
        compact( wr );
        // average the rows
        average = 0.0f;
        for ( int y=0;y<height;y++ )
        {
            hAverages[y] = averageRow(y);
            average += hAverages[y];
        }
        average /= height;
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
        Graphics g2 = src.getGraphics();
        g2.setColor(Color.BLACK);
        for ( int i=0;i<cols.length-1;i++ )
        {
            ArrayList col1 = cols[i];
            ArrayList col2 = cols[i+1];
            int[] list1 = new int[col1.size()];
            int[] list2 = new int[col2.size()];
            listToIntArray(col1,list1);
            listToIntArray(col2,list2);
            Matrix m = new Matrix( list1, list2 );
            ArrayList moves = m.traceBack();
            int x1,x2,z1,z2;
            for ( int y1=0,y2=0,j=0;j<moves.size();j++ )
            {
                Move move = (Move)moves.get(j);
                switch ( move )
                {
                    case exch:
                        x1=i*hScale;
                        x2=(i+1)*hScale;
                        z1=list1[y1]*vScale;
                        z2=list2[y2]*vScale;
                        g2.drawLine(x1,z1,x2,z2);
                        //System.out.println("drew line from "+x1+","+z1+" to "+z1+","+z2);
                        y1++;
                        y2++;
                        break;
                    case ins:
                        y2++;
                        break;
                    case del:
                        y1++;
                        break;
                }
            }
        }
    }
    /**
     * Convert an ArrayList of Integers to an int array
     * @param col the ArrayList of Integers
     * @param list the target int array
     * @throws Exception 
     */
    private void listToIntArray( ArrayList col, int[] list ) throws Exception
    {
        if ( list.length != col.size() )
            throw new ArrayIndexOutOfBoundsException(
                "col not the same length as array");
        for ( int j=0;j<col.size();j++ )
        {
            Object obj = col.get(j);
            if ( obj instanceof Integer )
                list[j] = ((Integer)col.get(j)).intValue();
            else
                throw new ClassCastException("object is not an Integer");
        }
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
}
