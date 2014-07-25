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

/**
 * <h3>TILT &ndash; text-to-image linking tool</h3>
 * <p>The TILT service is designed to automate the process of recognising
 * word-shapes on images. TILT doesn't attempt to do optical character
 * recognition (OCR) on those words because in manuscripts this is too
 * difficult and the word-shapes vary too much between hands etc. Instead
 * it tries just to recognise the shapes or outlines of words, and to link
 * them with words in a supplied transcription. In this way the reader of a
 * text+image can pass the mouse over or tap on a text word by word and see
 * the area of an image corresponding to a word or vice versa.</p>
 * <h4>First stage &ndash; reduce an image to plain black and white</h4>
 * <p>In order to do this TILT first reduces the image, assumed to be in
 * colour, to 256 shade greyscale. The {@link
 * tilt.image.Picture#convertToGreyscale() convertToGreyscale} method
 * performs this service. Next the image is reduced to pure black and white
 * by passing over it a local contrast filter. Simply converting to black
 * and white in the same way as converting it to greyscale would not work
 * if the lighting is uneven. The method employed in {@link
 * tilt.image.Picture#convertToTwoTone() convertToTwoTone} is borrowed from
 * the <a href="https://code.google.com/p/ocropus/"> ocropus
 * toolset</a>.</p>
 * <p>Removal of noise involves the identification of borders. For this the
 * {@link tilt.image.Border} class examines the edges of the image, looking
 * for dark areas or 'blobs' ({@link tilt.image.Blob Blob}) on the extreme
 * margins, which are extended inwards by following any blobs discovered
 * there. Then a broader area further in from this narrow border is
 * examined for large blobs with a high aspect-ratio (horizontal or
 * vertical). These are also added to the border. Finally, the blobs
 * identified in this way are removed.</p>
 * <h4>Second Stage &ndash; Recognise lines</h4>
 * <p>In order to recognise words their approximate location and their
 * sequence on the page must be worked out. For this TILT currently assumes
 * a top to bottom and left to right text-orientation. Other orientations
 * could, however, be supplied as options later on. Since text in
 * manuscripts may be deliberately warped, and line spacing may be very
 * uneven, and may include deletions and insertions, traditional OCR
 * methods, which rely on a basically even distribution of lines on the
 * page won't work. TILT thus divides the page into a number of rectangular
 * regions, about 200 of these down for a 1200 pixel image and just 25
 * blocks across. The reason for this is that text lines basically have
 * this shape. The black pixels in each rectangle are then added up, and if
 * they exceed the mean, they are considered to be part of a line. Lines
 * are identified by examining the columns of rectangles, and assuming that
 * a peak value is at the centre of a line. This is the purpose of the
 * {@link tilt.image.FindLines} class.</p>
 * <p>But this only identifies line fragments in vertical columns and does
 * not arrange them into lines. The purpose of the {@link
 * tilt.image.matchup} package is to align adjacent line-fragments and
 * assemble them into lines. The aligned line-fragments are then assembled
 * into an entire page of lines by the {@link tilt.image.page} package.</p>
 * <h4>Third Stage &ndash; Recognise Words</h4>
 * <p>The lines have been recognised. By following them, and
 * examining a small region on each side, blobs of adjacent black pixels that
 * represent words and parts of words can be identified.</p>
 * <p>Each blob is then surrounded by a polygon, using the {@link
 * tilt.image.FastConvexHull FastConvexHull} algorithm. This assesses the
 * outermost pixels of each blob and produces a minimal set belonging to a 
 * convex polygon. The distance between adjacent polygons is then
 * calculated. The {@link tilt.Utils#distanceBetween} method is used to 
 * compute the gaps between polygons.
 * These are then sorted by decreasing size. Next the number of words in
 * the text to be aligned <em>with</em> these blobs is used to determine
 * what is the smallest inter-polygon gap that designates a word-gap. So a gap 
 * chosen for, say, 250 words, ensures that approximately 249 gaps will be 
 * designated as inter-word gaps. All other gaps on the same line are
 * eliminated by merging bobs on either side. {@link
 * tilt.Utils#mergePolygons(java.awt.Polygon, java.awt.Polygon)} performs
 * this task.</p> <p>Finally, having determined the word-shapes, those
 * shared by more than one line are moved to the closest line, 
 * using the {@link tilt.image.page.Line#merge} method.</p>
 * <h4>Fourth Stage &ndash;Linking shapes to text</h4>
 * <p>To link shapes with the text can be achieved automatically by
 * converting the actual words of the known transcription to pixel widths,
 * and then comparing them to the actual pixel-widths of those words in the
 * page image. Since the number of words and chaaracters in the text are
 * known, it is simple to average out the pixel-widths of each letter and
 * so estimate the widths of each word in the text. Then, by adapting a
 * text-alignment algorithm (Ukkonen's famous diagonal diff algorithm from
 * Information and Control 1985), the pixel-widths in the page image can be
 * optimally aligned with those represented by the text. This gives us the
 * desired word-text alignment at the word level. However, since many
 * shapes may align with one word, or vice versa, many words may be found
 * in one shape, simply aligning shapes to words won't work in practice. To
 * overcome this Ukkonen's algorithm was extended to allow not only
 * insertions, deletions and exchanges, but also h-extensions and
 * v-extensions, whereby <em>several</em> words or several shapes can align
 * with <em>one</em> of the other kind. When this happens shapes are merged
 * or split as required.</p>
 * <p>Shapes can also be aligned with words that split over a line. This happens
 * either when there is a misalignment, or legitimately if a merged word 
 * (hyphen removed) is aligned with two halves at line-end and line start in 
 * a page image. In this case the word in the source text ill be split at a 
 * nice hyphenation point, and each half aligned with the correct polygon.</p>
 * <h4>API</h4>
 * <p>TILT is designed as a REST service with some simple GET and POST methods. 
 * The primary TILT service is the context /tilt/. The GET and POST requests 
 * are handled as follows:</p>
 * <ul><li>GET<ol> <li>test &ndash; this sub-service gets combined with the
 * following path-component to form the name of a class that should be a
 * sub-class of tilt.test.Test. So the path /tilt/test/post will invoke the
 * {@link tilt.test.Post#handle} method when a GET request is directed to
 * that URL.</li> <li>image &ndash; this will be directed to the {@link
 * tilt.handler.TiltImageHandler} class. Required parameters are:
 * <ul><li>docid: the URL of a suitable image file</li> <li>pictype: this
 * <em>must</em> be one of the values in the {@link
 * tilt.constants.ImageType} enumeration (currently original, greyscale,
 * twotone, cleaned, baselines, words, link). Asking for any one of these
 * will cause the required image-types to be generated. The last one
 * generates the text-to-image links and so calls all the others. The
 * service will then return an image of the appropriate type. Hence this
 * method is usually not called directly. It is rather used in HTML via a
 * statement like &lt;img
 * src="http://ecdosis.net/tilt/?docid=http:%2F%2Fecdosis.net%2Fimages%2F%2F00000005.jpg&amp;pictype=original"
 * width="500"&gt;, which will generate a GET request for that
 * image.</li></ul> <li>geojson &ndash; this will call the GeoJsonHandler
 * class and return a GeoJson document containing all the text to image 
 * alignments at the word-level. A POST of the image and its text must have 
 * been done first or a TiltExxception will be raised.</li></ol> 
 * <li>POST<br>Posting a basic GeoJson document to
 * /tilt/ will load that image, making it available for later linking (via
 * GET). This method will return a HTML &lt;img&gt; element containing a
 * suitable image as confirmation. This can be embedded in the source
 * document or ignored. Parameters for POST are:<ul> <li>geojson: the
 * GeoJson document (required). The GeoJson document should contain a basic
 * page description, including coordinates expressed as percentages of the
 * image area, e.g.:<br> <code>{<br>&nbsp;&nbsp;"type":
 * "Feature",<br>&nbsp;&nbsp;"geometry":
 * {<br>&nbsp;&nbsp;&nbsp;&nbsp;"type": "Polygon",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"coordinates": [<br>&nbsp;&nbsp;&nbsp;&nbsp;[
 * [0.0, 0.0], [100.0, 0.0], [100.0, 100.0], [0.0, 100.0]
 * ]<br>&nbsp;&nbsp;&nbsp;&nbsp;]<br>&nbsp;&nbsp;},<br>&nbsp;&nbsp;"properties":
 * {<br>&nbsp;&nbsp;&nbsp;&nbsp;"url":
 * "http://ecdosis.net/images/00000013.jpg"<br>&nbsp;&nbsp;}<br>}</code>.<br>
 * The "url" property is required. In future a full description of the page 
 * will be possible, specifying shapes whose alignment with the text should 
 * be fixed.</li> <li>encoding: the GeoJson document's encoding (optional)</li> 
 * <li>pictype: the desired pictype for the returned image (optional)</li> 
 * <li>text: the text to align to. This should be in HTML (required).</li> </ul>
 * </li></ul>
 * <h4>GeoJson response</h4>
 * <p><code>{"bbox":<br>
 * &nbsp;&nbsp;[0.0,0.0,100.0,100.0],<br>
 * &nbsp;&nbsp;"features":<br>
 * &nbsp;&nbsp;[<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;{<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"type":"FeatureCollection",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"bbox":[445,65,472,81],<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"name":"Line",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"features":<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"type":"Feature",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"geometry":<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"type":"Polygon",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"coordinates":<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[0.005,4.9736246E-4],<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[0.0051123598,4.8982666E-4],<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[0.005247191,4.8982666E-4],<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;]<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;},<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"properties":<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"offset":21,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"hyphen:5<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;]<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;},<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;....<br>
 * &nbsp;&nbsp;]<br>
 * }</code></p>
 * <h4>TILT Service</h4>
 * <p>TILT runs as a HTTP service on port 8082. The default URL is: 
 * http://localhost:8082/tilt, but this can be simplified to 
 * http://localhost/tilt/ by using mod-proxy. TILT uses Jetty to produce a 
 * standalone service, but it can be easily converted to run in an application-
 * server like Tomcat. TILT can be started via the tilt-start.sh script and 
 * similarly stopped via tilt-stop.sh. These scripts set up the required jars 
 * and also the path to the aesespeller library.</p>
 */
package tilt;
