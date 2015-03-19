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
package tilt;
import java.awt.image.WritableRaster;
import java.util.Iterator;
import java.util.ArrayList;
import java.awt.Point;
import java.awt.geom.PathIterator;
import java.awt.image.Raster;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import tilt.image.convexhull.*;
import tilt.image.page.Polygon;

/**
 * Some routines that need sharing by all
 * @author desmond
 */
public class Utils 
{
    /**
     * Convert a URL into a form suitable as a parameter
     * @param value the raw, unescapedURL
     * @return an escaped URL with / and  space escaped
     */
    public static String escape( String value )
    {
        StringBuilder sb = new StringBuilder();
        {
            for ( int i=0;i<value.length();i++ )
            if ( value.charAt(i) == ' ' )
                sb.append("%20");
            else if ( value.charAt(i) == '/' )
               sb.append("%2F");
            else
                sb.append( value.charAt(i) );
        }
        return sb.toString();
    }
    /**
     * Chop off the first component of a urn
     * @param urn the urn to chop
     * @return the first urn component
     */
    public static String first( String urn )
    { 
        int slashPos1 = -1;
        if ( urn.startsWith("/") )
            slashPos1 = urn.indexOf( "/" );
        int slashPos2 = urn.indexOf( "/", slashPos1+1 );
        if ( slashPos1 != -1 && slashPos2 != -1 )
            return urn.substring(slashPos1+1, slashPos2 );
        else if ( slashPos1 != -1 && slashPos2 == -1 )
            return urn.substring( slashPos1+1 );
        else if ( slashPos1 == -1 && slashPos2 != -1 )
            return urn.substring( 0,slashPos2 );
        else
            return urn;
    }
    /**
     * Extract the second component of a urn
     * @param urn the urn to extract from
     * @return the second urn component
     */
    public static String second( String urn )
    { 
        int start=-1,end=-1;
        for ( int state=0,i=0;i<urn.length();i++ )
        {
            char token = urn.charAt(i);
            switch ( state )
            {
                case 0:// always pass first char
                    state = 1;
                    break;
                case 1: 
                    if ( token == '/' )
                        state = 2;
                    break;
                case 2:
                    start=i;
                    if ( token == '/' )
                    {
                        state = -1;
                        end = i;
                    }
                    else
                        state = 3;
                    break;
                case 3:
                    if ( token == '/' )
                    {
                        end = i;
                        state = -1;
                    }
                    break;
            }
            if ( state == -1 )
                break;
        }
        if ( end == -1 )
            end = urn.length();
        if ( start == -1 )
            start = urn.length();
        return urn.substring( start, end );
    }
    /**
     * Pop off the frontmost part of the path
     * @param path the path to pop
     * @return the popped path
     */
    public static String pop( String path )
    {
        while ( path.length()>0 && path.startsWith("/") )
            path = path.substring(1);
        int pos = path.indexOf("/");
        if ( pos != -1 )
            path = path.substring( pos+1 );
        return path;
    }
    /**
     * Just escape quotes for a string to be jsonified
     * @param input the string to escape
     * @param replaceWith replace double quotes with this
     * @return the escaped string
     */
    public static String escapeQuotes( String input, String replaceWith )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<input.length();i++ )
        {
            char token = input.charAt(i);
            switch ( token )
            {
                case '"':
                    sb.append(replaceWith);
                    break;
                default:
                    sb.append(token);
                    break;
            }
        }
        return sb.toString();
    }
    /**
     * Creates a new raster that has a <b>copy</b> of the data in
     * <tt>ras</tt>.  This is highly optimized for speed.  There is
     * no provision for changing any aspect of the SampleModel.
     * However you can specify a new location for the returned raster.
     *
     * This method should be used when you need to change the contents
     * of a Raster that you do not "own" (ie the result of a
     * <tt>getData</tt> call).
     *
     * @param ras The Raster to copy.
     *
     * @param minX The x location for the upper left corner of the
     *             returned WritableRaster.
     *
     * @param minY The y location for the upper left corner of the
     *             returned WritableRaster.
     *
     * @return    A writable copy of <tt>ras</tt>
     */
    public static WritableRaster copyRaster(Raster ras, int minX, int minY) {
        WritableRaster ret = Raster.createWritableRaster
            (ras.getSampleModel(),
             new Point(0,0));
        ret = ret.createWritableChild
            (ras.getMinX()-ras.getSampleModelTranslateX(),
             ras.getMinY()-ras.getSampleModelTranslateY(),
             ras.getWidth(), ras.getHeight(),
             minX, minY, null);

        // Use System.arraycopy to copy the data between the two...
        DataBuffer srcDB = ras.getDataBuffer();
        DataBuffer retDB = ret.getDataBuffer();
        if (srcDB.getDataType() != retDB.getDataType()) {
            throw new IllegalArgumentException
                ("New DataBuffer doesn't match original");
        }
        int len   = srcDB.getSize();
        int banks = srcDB.getNumBanks();
        int [] offsets = srcDB.getOffsets();
        for (int b=0; b< banks; b++) {
            switch (srcDB.getDataType()) {
            case DataBuffer.TYPE_BYTE: {
                DataBufferByte srcDBT = (DataBufferByte)srcDB;
                DataBufferByte retDBT = (DataBufferByte)retDB;
                System.arraycopy(srcDBT.getData(b), offsets[b],
                                 retDBT.getData(b), offsets[b], len);
                break;
            }
            case DataBuffer.TYPE_INT: {
                DataBufferInt srcDBT = (DataBufferInt)srcDB;
                DataBufferInt retDBT = (DataBufferInt)retDB;
                System.arraycopy(srcDBT.getData(b), offsets[b],
                                 retDBT.getData(b), offsets[b], len);
                break;
            }
            case DataBuffer.TYPE_SHORT: {
                DataBufferShort srcDBT = (DataBufferShort)srcDB;
                DataBufferShort retDBT = (DataBufferShort)retDB;
                System.arraycopy(srcDBT.getData(b), offsets[b],
                                 retDBT.getData(b), offsets[b], len);
                break;
            }
            case DataBuffer.TYPE_USHORT: {
                DataBufferUShort srcDBT = (DataBufferUShort)srcDB;
                DataBufferUShort retDBT = (DataBufferUShort)retDB;
                System.arraycopy(srcDBT.getData(b), offsets[b],
                                 retDBT.getData(b), offsets[b], len);
                break;
            }
            }
        }

        return ret;
    }
    /**
     * The distance between two points squared
     * @param v the first point
     * @param w the second point
     * @return the distance between v and w squared
     */
    static double dist2( Point2D v, Point2D w ) 
    { 
        double xDiff = v.x() - w.x();
        double yDiff = v.y() - w.y();
        return xDiff*xDiff + yDiff*yDiff;
    }
    /**
     * Distance from a point to a line-segment squared
     * @param p the point
     * @param v the 1st point of the segment
     * @param w the 2nd point of the segment
     * @return the distance as a float
     */
    static double distToSegmentSquared( Point2D p, Point2D v, Point2D w) 
    {
        double l2 = dist2(v, w);
        if (l2 == 0.0f) 
            return dist2(p, v);
        double t = ((p.x()-v.x())*(w.x()-v.x())+(p.y()-v.y())*(w.y()-v.y()))/l2;
        if (t < 0) 
            return dist2(p, v);
        if (t > 1) 
            return dist2(p, w);
        Point2D q = new Point2D(v.x()+t*(w.x()-v.x()),v.y()
            +Math.round(t*(w.y()-v.y())));
        return dist2( p, q );
    }
    /**
     * Compute the distance from a point to a line segment
     * @param p the point
     * @param v first point of the line
     * @param w second point of the line
     * @return the distance as a float
     */
    public static double distanceToSegment( Point2D p, Point2D v, Point2D w )
    {
        double squared = distToSegmentSquared(p,v,w);
        return Math.sqrt(squared);
    }
    /**
     * Get the minimal separation between two polygons
     * @param P the first polygon
     * @param Q the second polygon
     * @return the smallest distance between segments or vertices of P, Q
     */
    public static double distanceBetween( Polygon P, Polygon Q )
    {
        double minDist = Double.MAX_VALUE;
        Point2D[] points1 = polygonToPoints(P);
        Point2D[] points2 = polygonToPoints(Q);
        Point2D last1=null,last2=null;
        for ( int i=0;i<points1.length;i++ )
        {
            Point2D p1 = points1[i];
            for ( int j=0;j<points2.length;j++ )
            {
                Point2D p2 = points2[j];
                double x = Math.abs(p1.x()-p2.x());
                double y = Math.abs(p1.y()-p2.y());
                // distance between vertices
                float dist = Math.round(Math.hypot(x,y));
                if ( dist < minDist )
                    minDist = dist;
                // distance between p1 and a segment of Q
                if ( last2 != null )
                {
                    double fDist2 = distanceToSegment( p1, last2, p2 );
                    if ( fDist2 < minDist )
                        minDist = fDist2;
                }
                // distance between p2 and a segment of P
                if ( last1 != null )
                {
                    double fDist1 = distanceToSegment( p2, last1, p1 );
                    if ( fDist1 < minDist )
                        minDist = fDist1;
                }
                last2 = p2;
            }
            last1 = p1;
        } 
        return minDist;
    }
    /**
     * Convert a polygon to an array of points
     * @param pg the poly
     * @return an arraylist of type Point2D (a la Sedgewick)
     */
    public static Point2D[] polygonToPoints( Polygon pg )
    {
        ArrayList<Point2D> points = new ArrayList<>();
        PathIterator iter = pg.getPathIterator(null);
        float[] coords = new float[6];
        int i = 0;
        while ( !iter.isDone() )
        {
            int step = iter.currentSegment(coords);
            switch ( step )
            {
                case PathIterator.SEG_CLOSE: case PathIterator.SEG_LINETO:
                    case PathIterator.SEG_MOVETO:
                    points.add( new Point2D(coords[0],coords[1]) );
                    break;
                default:
                    break;
            }
            iter.next();
        }
        Point2D[] array = new Point2D[points.size()];
        points.toArray( array );
        return array;
    }
    /**
     * Convert an array of points to a polygon
     * @param points an iterable set of points
     * @return a Polygon
     */
    public static Polygon pointsToPolygon( Iterable<Point2D> points )
    {
        Polygon pg = new Polygon();
        Iterator<Point2D> iter = points.iterator();
        while ( iter.hasNext() )
        {
            Point2D pt = iter.next();
            pg.addPoint( (int)Math.round(pt.x()), (int)Math.round(pt.y()) );
        }
        return pg;
    }
    /**
     * Merge two polygons into a minimal single polygon
     * @param pg1 the first poly
     * @param pg2 the second poly
     * @return one Polygon being the merge of both
     */
    public static Polygon mergePolygons( Polygon pg1, Polygon pg2 )
    {
        Point2D[] points1 = polygonToPoints(pg1);
        Point2D[] points2 = polygonToPoints(pg2);
        Point2D[] points3 = new Point2D[points1.length+points2.length];
        System.arraycopy( points1, 0, points3, 0, points1.length );
        System.arraycopy( points2, 0, points3, points1.length, points2.length );
        GrahamScan gs = new GrahamScan( points3 );
        Iterable<Point2D> points4 = gs.hull();
        return pointsToPolygon( points4 );
    }
    /**
     * Add a "/"to the end of a path if needed
     * @param path the original path
     * @return the path with ONE trailing slash
     */
    public static String ensureSlash( String path )
    {
        if ( path.endsWith("/") )
            return path;
        else
            return path+"/";
    }
}