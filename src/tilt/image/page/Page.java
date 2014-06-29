/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.image.page;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.awt.Point;
import tilt.image.matchup.Matrix;
import tilt.image.matchup.Move;
import java.awt.image.WritableRaster;

/**
 * Represent a collection of lines already recognised on a page
 * @author desmond
 */
public class Page 
{
    ArrayList<Line> lines;
    /**
     * Convert an array of scaled column-peaks into lines
     * @param cols a 2-D array of scaled line-peaks going *down* the page
     * @param hScale the scale in the horizontal dimension
     * @param vScale the scale in the vertical dimension 
     */
    public Page( ArrayList[] cols, int hScale,int vScale ) throws Exception
    {
        lines = new ArrayList<Line>();
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
                        this.update(new Point(x2,z2), list1[y1],list2[y2]);
                        y1++;
                        y2++;
                        break;
                    case ins:
                        x2=(i+1)*hScale;
                        z2=list2[y2]*vScale;
                        open( new Point(x2,z2), list2[y2] );
                        y2++;
                        break;
                    case del:
                        close( list1[y1],hScale );
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
     * Draw the lines on the page
     * @param g the graphics environment
     */
    public void draw( Graphics g )
    {
        g.setColor(Color.BLACK);
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            l.draw( g );
        }
    }
    /**
     * Align a new point to an existing line, or create one
     * @param loc the new location in true coordinates
     * @param oldEnd the previous end of the line 
     * @param newEnd the new end of the line 
     */
    private void update( Point loc, int oldEnd, int newEnd )
    {
        int i;
        for ( i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            if ( l.open && oldEnd == l.end )
            {
                l.update( loc, newEnd );
                break;
            }
        }
    }
    /**
     * Stop a line from propagating further
     * @param end the scaled vertical point of the line
     * @param hScale width of a rectangle
     */
    private void close( int end, int hScale )
    {
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            if ( l.end == end )
            {
                l.close(hScale);
                break;
            }
        }
    }
    /**
     * Insert a new line in the array
     * @param loc the location of its leftmost position
     * @param end the unscaled end y-coordinate
     */
    private void open( Point loc, int end )
    {
        int i;
        for ( i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            if ( l.end > end )
            {
                Line line = new Line();
                line.addPoint( loc, end );
                lines.add( i,line );
                break;
            }
        }
        if ( i == lines.size() )
        {
            Line line = new Line();
            line.addPoint( loc, end );
            lines.add( line );
        }
    }
    /**
     * Trim all the line starts right to the first black pixel on the image
     * @param wr the raster
     * @param hScale the size of a rectangular sample
     * @param vScale the vertical scale
     * @paramblack the value of a "black" pixel
     */
    public void refineLeft( WritableRaster wr, int hScale, int vScale, int black )
    {
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            l.refineLeft( wr, hScale, vScale, black );
        }
    }
    /**
     * Trim all the line ends right to the last black pixel in the row
     * @param wr the raster
     * @param hScale the size of a rectangular sample
     * @param vScale the vertical scale
     * @paramblack the value of a "black" pixel
     */
    public void refineRight( WritableRaster wr, int hScale, int vScale, int black )
    {
        for ( int i=0;i<lines.size();i++ )
        {
            Line l = lines.get(i);
            l.refineRight( wr, hScale, vScale, black );
        }
    }
    public ArrayList<Line> getLines()
    {
        return lines;
    }
}