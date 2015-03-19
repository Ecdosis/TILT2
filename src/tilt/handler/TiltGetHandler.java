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
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
                        if (second == null || second.length() == 0) {
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
                new TiltRecogniseHandler().handle(request, response, Utils.pop(urn));
            }
            else
            // serve up any other form of data in its native format
            // this is for secondary requests made by this service itself
            {
                File parent1 = new File(System.getProperty("user.dir"));
                String path = TiltGetHandler.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                File parent2 = new File(path).getParentFile();
                File file = new File(parent1,urn);
                if ( !file.exists())
                    file = new File(parent2,urn);
                if ( file.exists() )
                {
                    FileInputStream fis = new FileInputStream(file);
                    int len = (int)file.length();
                    byte[] data = new byte[len];
                    fis.read( data );
                    fis.close();
                    if ( file.getName().endsWith(".png") )
                        response.setContentType("image/png");
                    else if ( file.getName().endsWith(".jpg") )
                        response.setContentType("image/jpg");
                    else if ( file.getName().endsWith(".json") )
                        response.setContentType("application/json");
                    else if ( file.getName().endsWith(".html") )
                        response.setContentType("text/html");
                    else if ( file.getName().endsWith(".js") )
                        response.setContentType("text/javascript");
                    else if ( file.getName().endsWith(".css") )
                        response.setContentType("text/css");
                    else if ( file.getName().endsWith(".txt"))
                        response.setContentType("text/plain");
                    else    // else assume binary data
                        response.setContentType("application/octet-stream");
                    response.getOutputStream().write(data);
                }
                else
                    throw new FileNotFoundException(file.getAbsolutePath());
            }
                
        } catch (Exception e) {
            throw new TiltException(e);
        }
    }
}