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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import tlc2.TLC;
import tlc2.TLCGlobals;
import tlc2.TestMPRecorder;
import tlc2.output.EC;
import tlc2.output.MP;
import tlc2.output.MPRecorder;
import tlc2.tool.TLCStateInfo;
import tlc2.util.BitVector;
import tlc2.util.BufferedRandomAccessFile;
import util.ToolIO;

public abstract class ModelCheckerTestCase {

	protected static final String BASE_DIR = System.getProperty("basedir", "");
	protected static final String TEST_MODEL = "test-model" + File.separator;
	
	protected String path = "";
	protected final String spec;
	protected final TestMPRecorder recorder = new TestMPRecorder();
	protected String[] extraArguments = new String[0];


	public ModelCheckerTestCase(String spec) {
		this.spec = spec;
	}

	public ModelCheckerTestCase(String spec, String path) {
		this(spec);
		this.path = path;
	}
	
	public ModelCheckerTestCase(String spec, String path, String[] extraArguments) {
		this(spec, path);
		this.extraArguments  = extraArguments; 
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before
	public void setUp() {
		try {
			// TEST_MODEL is where TLC should look for user defined .tla files
			ToolIO.setUserDir(BASE_DIR + TEST_MODEL + path);
			
			MP.setRecorder(recorder);
			
			// Increase the liveness checking threshold to prevent liveness
			// checking of an incomplete graph. Most tests check that the 
			// state queue is empty and fail if not. This is only given 
			// when liveness checking is executed when all states have been
			// generated.
			TLCGlobals.livenessThreshold = Double.MAX_VALUE;
			
			final TLC tlc = new TLC();
			// * We want *no* deadlock checking to find the violation of the
			// temporal formula
			// * We use (unless overridden) a single worker to simplify
			// debugging by taking out threading
			// * MC is the name of the TLA+ specification to be checked (the file
			// is placed in TEST_MODEL
			final List<String> args = new ArrayList<String>(6);
			
			// *Don't* check for deadlocks. All tests are interested in liveness
			// checks which are shielded away by deadlock checking. TLC finds a
			// deadlock (if it exists) before it finds most liveness violations.
			args.add("-deadlock");
			
			args.add("-workers");
			args.add(Integer.toString(getNumberOfThreads()));
			
			// Never create checkpoints. They distort performance tests and are
			// of no use anyway.
			args.add("-checkpoint");
			args.add("0");

			args.addAll(Arrays.asList(extraArguments));
			
			args.add(spec);
			tlc.handleParameters(args.toArray(new String[args.size()]));
			
			// Run the ModelChecker
			tlc.process();
			
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * @return The number of worker threads TLC should use.
	 */
	protected int getNumberOfThreads() {
		return 1;
	}

	/**
	 * Asserts that the actual trace and the expected error trace are equal.
	 * 
	 * @param actual
	 *            The actual trace as recorded by {@link MPRecorder}.
	 * @param expectedTrace
	 *            The expected trace.
	 */
	protected void assertTraceWith(final List<Object> actual, final List<String> expectedTrace) {
		assertEquals(expectedTrace.size(), actual.size());
		for (int i = 0; i < expectedTrace.size(); i++) {
			final Object[] objs = (Object[]) actual.get(i);
			final TLCStateInfo stateInfo = (TLCStateInfo) objs[0];
			final String info = (String) stateInfo.info;
			if (i == 0) {
				// The first state has to be an initial state.
				"<Initial predicate>".equals(info);
			} else {
				// ... all others are reachable via an action.
				info.startsWith("<Action");
			}
			assertEquals(expectedTrace.get(i), 
					   stateInfo.toString().trim()); // trimmed to remove any newlines or whitespace
			assertEquals(i+1, objs[1]);
		}
	}

	/**
	 * Asserts that the error trace ends in stuttering at the given number.
	 * 
	 * @param stateNum
	 *            The number of the stuttering state
	 */
	protected void assertStuttering(int stateNum) {
		assertTrue(recorder.recorded(EC.TLC_STATE_PRINT3));
		List<Object> stutter = recorder.getRecords(EC.TLC_STATE_PRINT3);
		assertTrue(stutter.size() > 0);
		Object[] object = (Object[]) stutter.get(0);
		assertEquals(stateNum, object[1]);
	}

	/**
	 * Asserts that the error trace loops back to the state with the given
	 * number.
	 * 
	 * @param i The loop back state number.
	 */
	protected void assertBackToState(int stateNum) {
		assertTrue(recorder.recorded(EC.TLC_BACK_TO_STATE));
		List<Object> loop = recorder.getRecords(EC.TLC_BACK_TO_STATE);
		assertTrue(loop.size() > 0);
		Object[] object = (Object[]) loop.get(0);
		assertEquals(Integer.toString(stateNum), object[0]);
	}

	/**
	 * Asserts that the error trace loops back to the state with the given
	 * number.
	 * 
	 * @param i The loop back state number.
	 * @param action The action label associated with the loop back marker
	 */
	protected void assertBackToState(int stateNum, final String action) {
		assertTrue(recorder.recorded(EC.TLC_BACK_TO_STATE));
		List<Object> loop = recorder.getRecords(EC.TLC_BACK_TO_STATE);
		assertTrue(loop.size() > 0);
		Object[] object = (Object[]) loop.get(0);
		assertTrue(object.length > 1);
		assertEquals(Integer.toString(stateNum), object[0]);
		assertEquals(action, object[1]);
	}

	/**
	 * Check the file size of the AbstractDiskGraph files to assert that the
	 * expected amount of ptrs and nodes (outgoing arcs) have been written to
	 * disk.
	 * <p>
	 * CAUTION: The order in which the transitions are inserted into the
	 * {@link GraphNode} determines the size of the {@link BitVector}. I.e. if
	 * the truth values of the first N nodes inserted are true, and the
	 * remainder is false, the BitVector's size will correspond to N. However,
	 * if the first N truth values are false, followed by M trues, the
	 * BitVector's size is N + M.
	 * <p>
	 * See {@link GraphNode}'s constructor: it initializes {@link BitVector}
	 * with capacity zero and subsequently grows BV when bits are set to true.
	 * <p>
	 * 
	 * @see BitVector#read(BufferedRandomAccessFile)
	 * @see BitVector#write(BufferedRandomAccessFile)
	 * @see GraphNode#read(BufferedRandomAccessFile)
	 * @see GraphNode#write(BufferedRandomAccessFile)
	 * 
	 * @param nodesSize
	 * @param ptrsSize
	 */
	protected void assertNodeAndPtrSizes(final long nodesSize, final long ptrsSize) {
		final String metadir = TLCGlobals.mainChecker.metadir;
		assertNotNull(metadir);
		
		final File nodes = new File(metadir + File.separator + "nodes_0");
		assertTrue(nodes.exists());
		assertEquals(nodesSize, nodes.length());

		final File ptrs =  new File(metadir + File.separator + "ptrs_0");
		assertTrue(ptrs.exists());
		assertEquals(ptrsSize, ptrs.length());
	}
}
