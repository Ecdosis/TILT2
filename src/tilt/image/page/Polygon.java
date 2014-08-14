/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.image.page;
import java.awt.geom.PathIterator;
import java.awt.*;
import java.util.ArrayList;

/**
 * Override the awt Polygon class so we can store them as keys in a HashMap
 * @author desmond
 */
public class Polygon extends java.awt.Polygon
{
    static int MOD_ADLER = 65521;
    Point centroid;
    /**
     * Convert a polygon to its constituent points
     * @param pg the polygon to convert
     * @return an array of awt Points
     */
    private ArrayList<Point> toPoints( Polygon pg )
    {
        ArrayList<Point> list = new ArrayList<>();
        float[] coords = new float[6];
        PathIterator iter = pg.getPathIterator(null);
        while ( !iter.isDone() )
        {
            Point pt;
            int step = iter.currentSegment(coords);
            switch ( step )
            {
                case PathIterator.SEG_CLOSE: case PathIterator.SEG_LINETO:
                case PathIterator.SEG_MOVETO:
                    pt = new Point(Math.round(coords[0]),Math.round(coords[1]));
                    list.add(pt);
                break;
            default:
                break;
            }
            iter.next();
        }
        return list;
    }
    /**
     * Convert a list of points to a string
     * @param list the list of points in the polygon
     * @return a string
     */
    private String listToString( ArrayList<Point> list )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<list.size();i++ )
        {
            Point p = list.get(i);
            sb.append( p.x );
            sb.append( "," );
            sb.append( p.y );
            if ( i<list.size()-1 )
                sb.append(":");
        }
        return sb.toString();
    }
    /**
     * Return a (nearly) unique hash for this object
     * @return an int
     */
    @Override
    public int hashCode()
    {
        ArrayList<Point> list = toPoints( this );
        String s = listToString(list);
        byte[] data = s.getBytes();
        int a = 1, b = 0;
        int index;

        /* Process each byte of the data in order */
        for (index = 0; index < data.length; ++index)
        {
            a = (a + data[index]) % MOD_ADLER;
            b = (b + a) % MOD_ADLER;
        }
        return (b << 16) | a;
    }
    /**
     * Is this polygon equal to another (used to assess membership of hashmap)
     * @param obj the other Polygon to compare to
     * @return true if this is equal to obj
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( obj instanceof Polygon )
        {
            Polygon pg = (Polygon) obj;
            ArrayList<Point> list1 = toPoints( this );
            ArrayList<Point> list2 = toPoints( pg );
            String s1 = listToString( list1 );
            String s2 = listToString( list2 );
            return s1.equals(s2);
        }
        else
            return false;
    }
    /**
     * Compute the centroid of this polygon
     * @return the centre point
     */
    Point getCentroid()
    {
        if ( centroid == null )
        {
            int xSum = 0;
            int ySum = 0;
            ArrayList<Point> list = toPoints(this);
            for ( int i=0;i<list.size();i++ )
            {
                Point p = list.get(i);
                xSum += p.x;
                ySum = p.x;
            }
            centroid = new Point(xSum/list.size(),ySum/list.size());
        }
        return centroid;
    }
}
