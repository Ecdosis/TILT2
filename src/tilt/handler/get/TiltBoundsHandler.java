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
 *  (c) copyright Desmond Schmidt 2015
 */

package tilt.handler.get;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tilt.constants.GeoJSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import tilt.Utils;
import tilt.constants.Params;
import tilt.exception.TiltException;

/**
 * Get the GeoJson for an pageid, plus pageid
 * @author desmond
 */
public class TiltBoundsHandler extends TiltGeoJsonHandler
{
    String text;
    private String getWord( String serverName, String docid, String pageid, 
        int offset )
    {
        if ( text == null )
        {
            try
            {
                String textUrl = "http://"+serverName+"/pages/html?docid="
                    +docid+"&pageid="+pageid;
                text = Utils.getFromUrl(textUrl);
            }
            catch ( Exception e )
            {
                text = null;
            }
        }
        if ( text != null )
        {
            StringBuilder sb = new StringBuilder();
            for ( int i=offset;i<text.length();i++ )
            {
                char token = text.charAt(i);
                if ( !Character.isWhitespace(token)&& token != '<' )
                    sb.append(token);
                else
                    break;
            }
            return sb.toString();
        }
        else
            return null;
    }
    JSONArray polygonFromBBox( JSONArray bbox )
    {
        JSONArray rect = new JSONArray();
        JSONArray tl = new JSONArray();
        JSONArray bl = new JSONArray();
        JSONArray br = new JSONArray();
        JSONArray tr = new JSONArray();
        JSONArray tl2 = new JSONArray();
        tl.add(bbox.get(0));
        tl.add(bbox.get(1));
        bl.add(bbox.get(0));
        bl.add(bbox.get(3));
        br.add(bbox.get(2));
        br.add(bbox.get(3));
        tr.add(bbox.get(2));
        tr.add(bbox.get(1));
        tl2.add(bbox.get(0));
        tl2.add(bbox.get(1));
        rect.add(tl);
        rect.add(bl);
        rect.add(br);
        rect.add(tr);
        rect.add(tl2);
        return rect;
    }
    JSONArray getBBox( JSONArray polygon )
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        for ( int i=0;i<polygon.size();i++ )
        {
            JSONArray pt = (JSONArray)polygon.get(i);
            double x = (double)pt.get(0);
            double y = (double)pt.get(1);
            if ( x < minX )
                minX = x;
            if ( y < minY )
                minY = y;
            if ( x > maxX )
                maxX = x;
            if ( y > maxY )
                maxY = y;
        }
        JSONArray corners = new JSONArray();
        corners.add(minX);
        corners.add(minY);
        corners.add(maxX);
        corners.add(maxY);
        return corners;
    }
    /**
     * Parse the geoJson to get the polygons and offsets GeoJson is a 
     * FeatureCollection of Lines, each of which is a FeatureCollection 
     * of Polygons so what we do is reduce the polygons to their bounding
     * boxes and pretend that is all there is.
     * @param request the http request
     * @param response the response to write to
     * @param urn the remaining urn (empty)
     * @throws TiltException 
     */
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws TiltException 
    {
        try 
        {
            String docid = request.getParameter(Params.DOCID);
            String pageid = request.getParameter(Params.PAGEID);
            if ( docid != null && pageid !=null )
            {
                String server = request.getServerName();
                JSONObject geoJson = getGeoJson( server, docid, pageid );
                JSONObject geoJsonRects = new JSONObject();
                JSONArray lines = new JSONArray();
                geoJsonRects.put( GeoJSON.TYPE, GeoJSON.FEATURE_COLLECTION );
                geoJsonRects.put( GeoJSON.BBOX, geoJson.get(GeoJSON.BBOX) );
                geoJsonRects.put( GeoJSON.FEATURE_COLLECTION, lines );
                JSONArray gjLines = (JSONArray)geoJson.get(GeoJSON.FEATURES);
                if ( gjLines != null )
                {
                    for ( int i=0;i<gjLines.size();i++ )
                    {
                        // copy shared features
                        JSONObject line = new JSONObject();
                        JSONObject gjLine = (JSONObject)gjLines.get(i);
                        line.put(GeoJSON.TYPE, GeoJSON.FEATURE_COLLECTION );
                        line.put(GeoJSON.NAME, GeoJSON.LINE);
                        JSONArray bbox = (JSONArray)gjLine.get(GeoJSON.BBOX);
                        line.put( GeoJSON.BBOX, bbox );
                        JSONArray rectangles = new JSONArray();
                        line.put(GeoJSON.FEATURES,rectangles);
                        JSONArray polygons = (JSONArray)gjLine.get(
                            GeoJSON.FEATURES);
                        for (int j=0;j<polygons.size();j++ )
                        {
                            JSONObject pg = (JSONObject)polygons.get(j);
                            JSONObject newPg = new JSONObject();
                            rectangles.add( newPg );
                            JSONObject oldProps = (JSONObject)pg.get(
                                GeoJSON.PROPERTIES);
                            if ( oldProps != null )
                            {
                                JSONObject newProps = new JSONObject();
                                Long longOffset = (Long)oldProps.get(GeoJSON.OFFSET);
                                if ( longOffset != null )
                                {
                                    String word = getWord(request.getServerName(),
                                        docid,pageid,longOffset.intValue());
                                    if ( word != null )
                                        newProps.put("word", word );
                                    newPg.put(GeoJSON.PROPERTIES, newProps );
                                }
                            }
                            newPg.put(GeoJSON.TYPE, pg.get(GeoJSON.TYPE) );
                            JSONObject gjGeometry = (JSONObject)pg.get(
                                GeoJSON.GEOMETRY);
                            JSONArray rectangle = getBBox(
                                (JSONArray)gjGeometry.get(GeoJSON.COORDINATES));
                            newPg.put( GeoJSON.BBOX, rectangle );
                            JSONObject geometry = new JSONObject();
                            geometry.put(GeoJSON.TYPE, GeoJSON.POLYGON);
                            geometry.put(GeoJSON.COORDINATES, 
                                polygonFromBBox(rectangle));
                            newPg.put(GeoJSON.GEOMETRY, geometry);
                        }
                        line.put(GeoJSON.FEATURES, rectangles );
                        lines.add( line );
                    }
                }
                response.setContentType("text/plain");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().print( geoJsonRects.toJSONString() );
            }
        }
        catch ( Exception e )
        {
            throw new TiltException(e);
        }
    }
}
