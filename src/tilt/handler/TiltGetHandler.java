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
package tilt.handler;


import tilt.constants.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tilt.exception.TiltException;
import tilt.Utils;
import tilt.test.Test;
import tilt.editor.TiltEditor;
import tilt.handler.get.*;
/**
 * Handle a GET request for various image types, text, GeoJSON
 *
 * @author desmond
 */
public class TiltGetHandler extends TiltHandler {
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws TiltException {
        try {
            String service = Utils.first(urn);
            if (service.equals(Service.TEST.toString())) {
                try {
                    String second = Utils.second(urn);
                    if ( second.contains(".") )
                    {
                        new TiltFileHandler().handle(request,response, second );
                    }
                    else
                    {
                        if (second.length() == 0) {
                            second = "Post";
                        } else if (second.length() > 0) {
                            second = Character.toUpperCase(second.charAt(0))
                                + second.substring(1);
                        }
                        String className = "tilt.test." + second;
                        Class tClass = Class.forName(className);
                        Test t = (Test) tClass.newInstance();
                        t.handle(request, response, Utils.pop(urn));
                    }
                } catch (Exception e) {
                    throw new TiltException(e);
                }
            } 
            else if (service.equals(Service.EDITOR))
            {
                new TiltEditor().handle(request, response, Utils.pop(urn));
            }
            else if (service.equals(Service.IMAGE)) {
                new TiltImageHandler().handle(request, response, Utils.pop(urn));
            }
            else if ( service.equals(Service.GEOJSON) )
            {
                new TiltGeoJsonHandler().handle(request, response, Utils.pop(urn));
            }
            else
                new TiltFileHandler().handle(request,response,urn);
                
        } catch (Exception e) {
            throw new TiltException(e);
        }
    }
}