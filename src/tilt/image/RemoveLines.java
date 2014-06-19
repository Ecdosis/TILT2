/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;

/**
 * Remove lines from an image such as borders or internal lines
 * @author desmond
 */
public class RemoveLines 
{
    WritableRaster wr;
    int[] vertTotals;
    int[] horizTotals;
    ArrayList<Integer> horizPeaks;
    ArrayList<Integer> vertPeaks;
    static int MIN_PEAK_SEP = 10;
    private int computeAverage( int[] window, int N )
    {
        int total = 0;
        for ( int j=0;j<N;j++ )
            total += window[j];
        return total/N;
    }
    private ArrayList<Integer> computePeaks( int[] totals )
    {
        ArrayList<Integer> peaks = new ArrayList<Integer>();
        int top = 0;
        int[] window = new int[5];
        int seen = 0;
        int average;
        // now work out which peaks are above average height
        int total = 0;
        for ( int i=0;i<totals.length;i++ )
            total += totals[i];
        int meanHeight = total/totals.length;
        for ( int i=0;i<totals.length;i++ )
        {
            window[i%5] = totals[i];
            if ( seen < 5 )
                seen++;
            average = computeAverage( window, seen );
            if ( average < top && top > meanHeight 
                && (peaks.size()==0||Math.abs(
                    peaks.get(peaks.size()-1).intValue()-i)>MIN_PEAK_SEP) )
            {
                peaks.add( new Integer(i) );
            }
            top = average;
        }
        return peaks;
    }
    public RemoveLines( BufferedImage twotone )
    {
        wr = twotone.getRaster();
        horizTotals = new int[twotone.getHeight()];
        vertTotals = new int[twotone.getWidth()];
        int[] iArray = new int[1];
        // compute horiz and vert totals
        for ( int y=0;y<horizTotals.length;y++ )
        {
            for ( int x=0;x<vertTotals.length;x++ )
            {
                wr.getPixel(x,y,iArray);
                horizTotals[y] += (iArray[0]==0)?1:0;
                vertTotals[x] += (iArray[0]==0)?1:0;
            }
        }
        // find peaks
        vertPeaks = computePeaks( vertTotals );
        horizPeaks = computePeaks( horizTotals );
        // examine peaks for blobs
    }
    /**
     * Remove all identified lines
     */
    public void removeAll()
    {
    }
}
