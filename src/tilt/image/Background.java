/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tilt.image;

/**
 * Maintain information about the current background value
 * @author desmond
 */
public class Background 
{
    /** circular queue */
    int[] queue;
    int average;
    int lightest;
    int darkest;
    int len;
    int pos;
    static final int QUEUE_LEN = 128;
    Background()
    {
        this.queue = new int[QUEUE_LEN];
        this.average = 0xE5DCD7;
        this.lightest = 0;
        this.darkest = 255;
        this.len = this.pos = 0;
    }
    /**
     * Update the queue and return the new average
     * @param rgba the current bright rgba pixel value
     * @return the new average rgb value
     */
    int update( int rgba )
    {
        int averageRGBA = getAverage(rgba);
        if ( averageRGBA > lightest )
            lightest = averageRGBA;
        if ( averageRGBA < darkest )
            darkest = averageRGBA;
        // remove alpha channel
        int value = rgba & 0x00FFFFFF;
        queue[pos] = value;
        if ( this.len < queue.length )
            this.len++;
        pos = (pos+1)<queue.length?pos+1:0;
        int totalRed=0,totalBlue=0,totalGreen=0;
        for ( int i=1;i<=this.len;i++ )
        {
            int j = pos-i;
            if ( j < 0 )
                j = queue.length+j;
            totalRed += (queue[j] & 0x00FF0000)>>16;
            totalGreen += (queue[j] & 0x0000FF00)>>8;
            totalBlue += (queue[j] & 0x000000FF);
        }
        totalRed /= len;
        totalBlue /= len;
        totalGreen /= len;
        totalRed <<= 16;
        totalGreen <<= 8;
        return totalRed | totalGreen | totalBlue;
    }
    /**
     * Is the average brightness closer to the darkest or lightest colour?
     * @param rgba the current rgba pixel
     * @return true if it is more bright than dark
     */
    boolean isBright( int rgba )
    {
        if ( lightest == 0 || darkest == 255 )
            return true;
        else
        {
            rgba = getAverage(rgba);
            if ( rgba >= lightest )
                return true;
            else if ( rgba <= darkest )
                return false;
            else
            {
                int diff1 = rgba - darkest;
                int diff2 = lightest - rgba;
                // only bright if it is MUCH closer to bright than dark
                return (float)Math.abs(diff1)/(float)Math.abs(diff2) > 2.0f;
            }
        }
    }
    private int getAverage( int col )
    {
        int blueCol = col & 0x000000FF;
        int redCol = col & 0x00FF0000;
        redCol >>= 16;
        int greenCol = col & 0x0000FF00;
        greenCol >>= 8;
        return (redCol+greenCol+blueCol)/3;  
    }
}
