/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.html;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
// @formatter:off
@SuiteClasses({
	HTMLDoctypeScannerTest.class,
	HTMLEditorTest.class,
	HTMLFoldingComputerTest.class,
	HTMLOpenTagCloserTest.class,
	HTMLScannerTest.class,
	HTMLSourcePartitionScannerTest.class,
	HTMLTagScannerTest.class,
	HTMLTagUtilTest.class,
})
// @formatter:on
public class HTMLEditorTests
{
}
