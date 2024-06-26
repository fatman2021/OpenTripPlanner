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

package org.opentripplanner.routing.edgetype.loader;

import static org.opentripplanner.common.IterableLibrary.*;
import java.io.File;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.TestUtils;

public class TestHopFactory extends TestCase {

    private Graph graph;

    private GtfsContext context;

    public void setUp() throws Exception {

        context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = new Graph();

        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(graph);

    }

    public void testBoardAlight() throws Exception {

        Vertex stop_a = graph.getVertex("agency_A_depart");
        Vertex stop_b_depart = graph.getVertex("agency_B_depart");

        assertEquals(1, stop_a.getDegreeOut());
        assertEquals(3, stop_b_depart.getDegreeOut());

        for (Edge e : stop_a.getOutgoing()) {
            assertEquals(PatternBoard.class, e.getClass());
        }

        PatternBoard pb = (PatternBoard) stop_a.getOutgoing().iterator().next();
        Vertex journey_a_1 = pb.getToVertex();

        assertEquals(1, journey_a_1.getDegreeIn());

        for (Edge e : journey_a_1.getOutgoing()) {
            if (e.getToVertex() instanceof TransitStop) {
                assertEquals(PatternAlight.class, e.getClass());
            } else {
                assertEquals(PatternHop.class, e.getClass());
            }
        }
    }

    public void testDwell() throws Exception {
        Vertex stop_a = graph.getVertex("agency_A_depart");
        Vertex stop_c = graph.getVertex("agency_C_arrive");

        TraverseOptions options = new TraverseOptions(context);

        ShortestPathTree spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_c
                .getLabel(),
                TestUtils.dateInSeconds(2009, 8, 7, 8, 0, 0), options);

        GraphPath path = spt.getPath(stop_c, false);
        assertNotNull(path);
        assertEquals(6, path.states.size());
        long endTime = TestUtils.dateInSeconds(2009, 8, 7, 8, 30, 0);
        assertEquals(endTime, path.getEndTime());

    }

    public void testRouting() throws Exception {

        Vertex stop_a = graph.getVertex("agency_A_depart");
        Vertex stop_b = graph.getVertex("agency_B_arrive");
        Vertex stop_c = graph.getVertex("agency_C_arrive");
        Vertex stop_d = graph.getVertex("agency_D_arrive");
        Vertex stop_e = graph.getVertex("agency_E_arrive");

        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        ShortestPathTree spt;
        GraphPath path;

        // A to B
        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_b.getLabel(), 
                TestUtils.dateInSeconds(2009, 8, 7, 0, 0, 0), options);

        path = spt.getPath(stop_b, false);
        assertNotNull(path);
        assertEquals(4, path.states.size());

        // A to C
        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_c.getLabel(), 
                TestUtils.dateInSeconds(2009, 8, 7, 0, 0, 0), options);

        path = spt.getPath(stop_c, false);
        assertNotNull(path);
        assertEquals(6, path.states.size());

        // A to D
        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_d.getLabel(), 
                TestUtils.dateInSeconds(2009, 8, 7, 0, 0, 0), options);

        path = spt.getPath(stop_d, false);
        assertNotNull(path);
        assertTrue(path.states.size() <= 11);
        long endTime = TestUtils.dateInSeconds(2009, 8, 7, 0, 0, 0) + 40 * 60;
        assertEquals(endTime, path.getEndTime());

        // A to E
        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_e.getLabel(),
                TestUtils.dateInSeconds(2009, 8, 7, 0, 0, 0), options);

        path = spt.getPath(stop_e, false);
        assertNotNull(path);
        assertTrue(path.states.size() <= 12);
        endTime = TestUtils.dateInSeconds(2009, 8, 7, 0, 0, 0) + 70 * 60;
        assertEquals(endTime, path.getEndTime());
    }
}
