/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.handler;
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
    public String url = "http://localhost/images/frontispiece3.jpg";
    /** line */
    public float sharedRatio = 0.75f;
    /** percent-wise proportion of image to be scanned*/
    public JSONArray coords;
    /**
     * Initialise an options object fro a possibly empty set of properties
     * @param opts a RAW JSON object
     */
    public Options( JSONObject opts )
    {
        if ( opts.containsKey("url"))
            this.url = (String)opts.get("url");
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
