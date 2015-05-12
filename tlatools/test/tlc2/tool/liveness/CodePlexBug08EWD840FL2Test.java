/*******************************************************************************
 * Copyright (c) 2015 Microsoft Research. All rights reserved. 
 *
 * The MIT License (MIT)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *   Markus Alexander Kuppe - initial API and implementation
 ******************************************************************************/

package tlc2.tool.liveness;

import java.io.File;
import java.util.List;

import tlc2.TLCGlobals;
import tlc2.output.EC;

/**
 * see http://tlaplus.codeplex.com/workitem/8
 */
public class CodePlexBug08EWD840FL2Test extends ModelCheckerTestCase {

	public CodePlexBug08EWD840FL2Test() {
		super("EWD840MC2", "CodePlexBug08");
	}
	
	public void testSpec() {
		// ModelChecker has finished and generated the expected amount of states
		assertTrue(recorder.recorded(EC.TLC_FINISHED));
		assertTrue(recorder.recordedWithStringValues(EC.TLC_STATS, "15986", "1566"));
		
		// Assert it has found the temporal violation and also a counter example
		assertTrue(recorder.recorded(EC.TLC_TEMPORAL_PROPERTY_VIOLATED));
		assertTrue(recorder.recorded(EC.TLC_COUNTER_EXAMPLE));
		
		// Assert the error trace
		assertTrue(recorder.recorded(EC.TLC_STATE_PRINT2));
		
		// last state points back to state 1
		assertTrue(recorder.recorded(EC.TLC_BACK_TO_STATE));
		List<Object> stutter = recorder.getRecords(EC.TLC_BACK_TO_STATE);
		assertTrue(stutter.size() > 0);
		Object[] object = (Object[]) stutter.get(0);
		assertEquals("1", object[0]);
		
		// Check the file size of the AbstractDiskGraph files to check if the
		// expected amount of ptrs and nodes (outgoing arcs) have been written
		// to disk.
		final String metadir = TLCGlobals.mainChecker.metadir;
		assertNotNull(metadir);
		final File nodes = new File(metadir + File.separator + "nodes_0");
		final File ptrs =  new File(metadir + File.separator + "ptrs_0");
		assertTrue(nodes.exists());
		assertTrue(ptrs.exists());
		assertEquals(53958732L, nodes.length());
		assertEquals(831296L, ptrs.length());
	}
}
