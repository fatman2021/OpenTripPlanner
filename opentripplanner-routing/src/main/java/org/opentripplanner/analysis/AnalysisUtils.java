/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.analysis;

import static org.opentripplanner.common.IterableLibrary.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opentripplanner.common.DisjointSet;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.util.MapUtils;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class AnalysisUtils {

    private static final double PRECISION = 1E6;

    /**
     * Get polygons covering the components of the graph. The largest component (in terms of number
     * of nodes) will not overlap any other components (it will have holes); the others may overlap
     * each other.
     * 
     * @param dateTime
     */
    public static List<Geometry> getComponentPolygons(Graph graph, TraverseOptions options,
            long time) {
        DisjointSet<Vertex> components = getConnectedComponents(graph);

        Map<Integer, List<Coordinate>> componentCoordinates = new HashMap<Integer, List<Coordinate>>();
        for (Vertex v : graph.getVertices()) {
            for (Edge e : v.getOutgoing()) {
                State s0 = new State(time, v, options);
                State s1 = e.traverse(s0);
                if (s1 != null) {
                    Integer component = components.find(e.getFromVertex());
                    Geometry geometry = s1.getBackEdgeNarrative().getGeometry();
                    if (geometry != null) {
                        List<Coordinate> coordinates = new ArrayList<Coordinate>(Arrays.asList(geometry.getCoordinates()));
                        for (int i = 0; i < coordinates.size(); ++i) {
                            Coordinate coordinate = new Coordinate(coordinates.get(i));
                            coordinate.x = Math.round(coordinate.x * PRECISION) / PRECISION;
                            coordinate.y = Math.round(coordinate.y * PRECISION) / PRECISION;
                            coordinates.set(i, coordinate);
                        }
                        MapUtils.addToMapList(componentCoordinates, component, coordinates);
                    }
                }
            }
        }

        // generate convex hull of each component
        List<Geometry> geoms = new ArrayList<Geometry>();
        GeometryFactory gf = new GeometryFactory();
        int mainComponentSize = 0;
        int mainComponentIndex = -1;
        int component = 0;
        for (List<Coordinate> coords : componentCoordinates.values()) {
            Coordinate[] coordArray = new Coordinate[coords.size()];
            ConvexHull hull = new ConvexHull(coords.toArray(coordArray), gf);
            Geometry geom = hull.getConvexHull();
            // buffer components which are mere lines so that they do not disappear.
            if (geom instanceof LineString) {
                geom = geom.buffer(0.01); // ~10 meters
            } else if (geom instanceof Point) {
                geom = geom.buffer(0.05); // ~50 meters, so that it shows up
            }

            geoms.add(geom);
            if (mainComponentSize < coordArray.length) {
                mainComponentIndex = component;
                mainComponentSize = coordArray.length;
            }
            ++component;
        }

        // subtract small components out of main component
        // (small components are permitted to overlap each other)
        Geometry mainComponent = geoms.get(mainComponentIndex);
        for (int i = 0; i < geoms.size(); ++i) {
            Geometry geom = geoms.get(i);
            if (i == mainComponentIndex) {
                continue;
            }
            mainComponent = mainComponent.difference(geom);
        }
        geoms.set(mainComponentIndex, mainComponent);

        return geoms;

    }

    public static DisjointSet<Vertex> getConnectedComponents(Graph graph) {
        DisjointSet<Vertex> components = new DisjointSet<Vertex>();

        for (Vertex v : graph.getVertices()) {
            for (Edge e : v.getOutgoing()) {
                components.union(e.getFromVertex(), e.getToVertex());
            }
        }
        return components;
    }

}
