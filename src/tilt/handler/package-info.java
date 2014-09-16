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
 * These classes handle the various types of REST request (PUT,DELETE,GET,POST)
 * and also specific services, such as returning images referred to on HTMLpages
 * ({@link tilt.handler.TiltImageHandler}) and GeoJson 
 * ({@link tilt.handler.GeoJsonHandler}). Text can be posted in PLAIN and HTML
 * formats.
 */
package tilt.handler;
