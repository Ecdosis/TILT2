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

package tilt.handler.post;
import org.json.simple.*;

/**
 * Store options for TILT2
 * @author desmond
 */
public class Options 
{
    /** width of vertical stripes, line recognition */
    public float hScaleRatio = 0.04f;
    /** number of pixels to smooth line-peaks vertically */
    public int smoothN = 9;
    /** amount to blur image */
    public int blur = 0;
    public float minBlackPC = 0.5f;
    /** minimum proportion of width or height taken up by blob */
    public float minHProportion = 0.05f;
    public float minVProportion = 0.05f;
    public float oddShape = 4.0f;
    /** maximum amount times line depth to accept new lines */
    public float lineDepthFactor=1.0f;
    public String docid = "";
    public String pageid="";
    /** line */
    public float sharedRatio = 0.75f;
    /** percent-wise proportion of image to be scanned*/
    public JSONArray coords;
    /** is the text hyphenated at line-end?*/
    public boolean hyphenated = false;
    // url override - optional for testing
    String url;
    /**
     * Initialise an options object from a possibly empty set of properties
     * @param opts a RAW JSON object
     */
    public Options( JSONObject opts )
    {
        if ( opts.containsKey("docid"))
            this.docid = (String)opts.get("docid");
        if ( opts.containsKey("pageid"))
            this.pageid = (String)opts.get("pageid");
        if ( opts.containsKey("blur") )
            this.blur = ((Number)opts.get("blur")).intValue();
        if ( opts.containsKey("smoothN") )
            this.smoothN = ((Number)opts.get("smoothN")).intValue();
        if ( opts.containsKey("hScaleRatio") )
            this.hScaleRatio = ((Number)opts.get("hScaleRatio")).floatValue();
        if ( opts.containsKey("minBlackPC") )
            this.minBlackPC = ((Number)opts.get("minBlackPC")).floatValue();
        if ( opts.containsKey("minHProportion") )
            this.minHProportion = ((Number)opts.get("minHProportion")).floatValue();
        if ( opts.containsKey("minVProportion") )
            this.minVProportion = ((Number)opts.get("minVProportion")).floatValue();
        if ( opts.containsKey("oddShape") )
            this.oddShape = ((Number)opts.get("oddShape")).floatValue();
        if ( opts.containsKey("lineDepthFactor") )
            this.lineDepthFactor = ((Number)opts.get("lineDepthFactor")).floatValue();
        if ( opts.containsKey("sharedRatio") )
            this.sharedRatio = ((Number)opts.get("sharedRatio")).floatValue();
        if ( opts.containsKey("hyphenated") )
            this.hyphenated = ((Boolean)opts.get("hyphenated")).booleanValue();
        if ( opts.containsKey("url") )
            this.url = (String)opts.get("url");
        String s = "[[0.0,0.0],[100.0,0.0],[100.0,100.0],[0.0,100.0]]";
        Object obj=JSONValue.parse(s);
        this.coords = (JSONArray)obj;
    }
    /**
     * Override the image coordinates
     * @param array the array in question
     */
    public void setCoords( JSONArray array )
    {
        this.coords = (JSONArray)((JSONArray)array).get(0);
    }
}
