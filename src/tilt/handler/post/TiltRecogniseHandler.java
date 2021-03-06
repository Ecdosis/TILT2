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

package tilt.handler.post;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import tilt.constants.Params;
import tilt.exception.TiltException;
import tilt.image.Picture;
import tilt.image.PictureRegistry;
import tilt.Utils;
import tilt.constants.ImageType;
import tilt.handler.TiltPostHandler;
import tilt.exception.ImageException;
import java.net.InetAddress;
import calliope.core.database.*;
import calliope.core.constants.Database;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.List;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.FileUploadException;

/**
 * Handle an Ajax request for geoJson data about a picture
 * @author desmond
 */
public class TiltRecogniseHandler extends TiltPostHandler
{
    /** the image URL */
    String docid;
    /** the image page id */
    String pageid;
    /** the GeoJson we extract from the picture */
    String geoJson;
    ImageType picType;
    TextIndex text;
    void parseRequest( HttpServletRequest request ) throws FileUploadException, 
        Exception
    {
        if ( ServletFileUpload.isMultipartContent(request) )
        {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            List<FileItem> items = upload.parseRequest(request);
            for ( int i=0;i<items.size();i++ )
            {
                FileItem item = (FileItem) items.get( i );
                if ( item.isFormField() )
                {
                    String fieldName = item.getFieldName();
                    if ( fieldName != null )
                    {
                        String contents = item.getString();
                        if ( fieldName.equals(Params.DOCID) )
                            this.docid = contents;
                        else if ( fieldName.equals(Params.PAGEID) )
                            this.pageid = contents;
                        else if ( fieldName.equals(Params.GEOJSON) )
                            this.geoJson = contents;
                        else if ( fieldName.equals(Params.TEXT) )
                        {
                            String textParam = contents;
                            text = new TextIndex( textParam, "en_GB" );
                        }
                    }
                }
                // we're not uploading files
            }
        }       
    }
    /**
     * Run through the recognition phases and send back progress messages
     * @param p the picture to recognise
     * @param response the http response to write progress to
     * @param src the start imagetype
     * @param dest the end-image type
     * @throws ImageException
     * @throws IOException 
     */
    void doRecogniseProgress( Picture p, HttpServletResponse response, 
        ImageType src, ImageType dest ) 
        throws ImageException, IOException
    {
        picType = src;
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        float nValues = (ImageType.values().length-1);
        PrintWriter pw = response.getWriter();
        do
        {
            float value = (float) picType.ordinal();
            float progress = value*100.0f/nValues;
            pw.print( Math.round(progress) );
            pw.print( " " );
            pw.println( picType.getMessage() );
            pw.flush();
            switch ( picType )
            {
                case load:
                    p.load();
                    picType = ImageType.preflight;
                    break;
                case preflight:
                    p.convertToPreflight();
                    picType = ImageType.greyscale;
                    break;
                case greyscale:
                    p.convertToTwoTone();
                    picType = ImageType.twotone;
                    break;
                case twotone:
                    p.convertToCleaned();
                    picType = ImageType.cleaned;
                    break;
                case cleaned:
                    p.convertToReconstructed();
                    picType = ImageType.baselines;
                    break;
                case reconstructed:
                    p.convertToBaselines();
                    picType = ImageType.baselines;
                    break;
                case baselines:
                    p.convertToWords();
                    picType = ImageType.words;
                    break;
                case words:
                    p.convertToLinks();
                    picType = ImageType.link;
                    break;
            }
        } while ( picType != dest );
        if ( picType == ImageType.link )
        {
            pw.print( "100 " );
            pw.println( picType.getMessage() );
        }
    }
    /**
     * Handle a request for geoJEON text-t-image links from editor
     * @param request the http request
     * @param response the http response
     * @param urn the remaining urn of the request
     * @throws TiltException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws TiltException
    {
        try
        {
            parseRequest( request );
            if ( docid != null && pageid != null && geoJson != null 
                    && text != null )
            {
                String url = Utils.getUrl(request.getServerName(),docid,pageid);
                Picture p = PictureRegistry.get(url);
                if ( p == null )
                {
                    Object obj = JSONValue.parse(geoJson);
                    if ( obj instanceof JSONObject )
                    {
                        JSONObject g = (JSONObject)obj;
                        Options opts = new Options((JSONObject)g.get("properties"));
                        JSONObject geometry = (JSONObject)g.get("geometry");
                        if ( geometry != null && geometry.get("coordinates") 
                            instanceof JSONArray )
                        {
                            InetAddress poster = getIPAddress(request);
                            JSONArray cc = (JSONArray)geometry.get("coordinates");
                            p = new Picture( opts, url, text, coordsToArray(cc), poster);
                            doRecogniseProgress( p, response,
                                ImageType.load,ImageType.preflight );
                            PictureRegistry.update( url, p );
                            // it will be identified later by its url during GET
                        }
                        else
                            throw new Exception("Invalid geoJSON");
                    }
                    else
                        throw new Exception("Invalid geoJSON");
                }
                doRecogniseProgress( p, response,ImageType.preflight, 
                    ImageType.link );
                Connection conn = Connector.getConnection();
                geoJson = p.getGeoJson();
                // caller should now GET the geojson result
                conn.putToDb( Database.TILT, 
                    Utils.ensureSlash(docid)+pageid, geoJson );
            }
            else
                throw new Exception("Need a docid param: an image url!");
        }
        catch ( Exception e )
        {
            throw new TiltException( e );
        }
    }
}
