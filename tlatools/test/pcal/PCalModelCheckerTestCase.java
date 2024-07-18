/*******************************************************************************
 * Copyright (c) 2018 Microsoft Research. All rights reserved. 
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
package pcal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;

import tlc2.tool.CommonTestCase;
import tlc2.tool.liveness.ModelCheckerTestCase;
import util.ToolIO;

public abstract class PCalModelCheckerTestCase extends ModelCheckerTestCase {

	private final List<String> pcalArgs = new ArrayList<String>();

	public PCalModelCheckerTestCase(final String spec, final String path) {
		super(spec, path);
		this.pcalArgs.add("-unixEOL");
	}
	
	public PCalModelCheckerTestCase(final String spec, final String path, final String[] extraPcalArgs) {
		this(spec, path);
		this.pcalArgs.addAll(Arrays.asList(extraPcalArgs));
	}
	
	@Before
	@Override
	public void setUp() {
		this.pcalArgs.add(CommonTestCase.BASE_PATH + File.separator + path + File.separator + spec + ".tla");
		
		// Run PCal translator
		final TLAtoPCalMapping pcal2tla = trans.runMe(pcalArgs.toArray(new String[pcalArgs.size()]));
		assertNotNull(pcal2tla); // successfully translated PCal to TLA+
		
		final String[] messages = ToolIO.getAllMessages();
		assertTrue(Arrays.toString(messages), messages.length == 0);

		// Run TLC
		super.setUp();
	}
}
