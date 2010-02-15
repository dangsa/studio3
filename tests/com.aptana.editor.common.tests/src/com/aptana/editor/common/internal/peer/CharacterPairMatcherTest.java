package com.aptana.editor.common.internal.peer;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.ICharacterPairMatcher;

public class CharacterPairMatcherTest extends TestCase
{
	private ICharacterPairMatcher matcher;

	@Override
	protected void setUp() throws Exception
	{
		char[] pairs = new char[] { '(', ')', '{', '}', '[', ']', '`', '`', '\'', '\'', '"', '"' };
		matcher = new CharacterPairMatcher(pairs);
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception
	{
		if (matcher != null)
		{
			matcher.dispose();
		}
		matcher = null;
		super.tearDown();
	}

	public void testPairMatching()
	{
		String source = "( { [ `ruby command`, 'single quoted string', \"double quoted string\" ] } )";
		IDocument document = new Document(source);
		assertMatch(document, source, 0); // ()
		assertMatch(document, source, 2); // {}
		assertMatch(document, source, 4); // []
		assertMatch(document, source, 6, 19); // ``
		assertMatch(document, source, 22, 43); // ''
		assertMatch(document, source, 46, 67); // ""
	}

	public void testPairMatching2()
	{
		String source = "<?xml version=\"1.0\"\n encoding=\"ISO-8859-1\"?>";
		IDocument document = new Document(source);
		assertMatch(document, source, 14, 18);
		assertMatch(document, source, 30, 41);
	}

	public void testMatchesCharToRightIfNothingOnLeft()
	{
		String source = "( )";
		IDocument document = new Document(source);
		assertRawMatch(document, 0, 2, 0, 3);
	}

	public void testMatchesCharToRightIfNothingOnLeft2()
	{
		String source = "( { [ `ruby command`, 'single quoted string', \"double quoted string\" ] } )";
		IDocument document = new Document(source);
		assertRawMatch(document, 0, source.length() - 1, 0, source.length()); // ()
		assertRawMatch(document, 2, source.length() - 3, 2, source.length() - 4); // {}
		assertRawMatch(document, 4, source.length() - 5, 4, source.length() - 8); // []
		assertRawMatch(document, 6, 19, 6, 14); // ``
		assertRawMatch(document, 22, 43, 22, 22); // ''
		assertRawMatch(document, 46, 67, 46, 22); // ""
	}

	private void assertRawMatch(IDocument document, int leftOffsetToMatch, int rightOffsetToMatch, int offset,
			int length)
	{
		// left
		IRegion region = matcher.match(document, leftOffsetToMatch);
		assertNotNull(region);
		assertEquals("offset", offset, region.getOffset());
		assertEquals("length", length, region.getLength());
		assertEquals(ICharacterPairMatcher.LEFT, matcher.getAnchor());
		// right
		region = matcher.match(document, rightOffsetToMatch);
		assertNotNull(region);
		assertEquals("offset", offset, region.getOffset());
		assertEquals("length", length, region.getLength());
		assertEquals(ICharacterPairMatcher.RIGHT, matcher.getAnchor());
	}

	private void assertMatch(IDocument document, String source, int i)
	{
		int j = source.length() - i - 1;
		assertMatch(document, source, i, j);
	}

	private void assertMatch(IDocument document, String source, int i, int j)
	{
		int length = (j - i) + 1;
		// left
		IRegion region = matcher.match(document, i + 1);
		assertNotNull(region);
		assertEquals("offset", i, region.getOffset());
		assertEquals("length", length, region.getLength());
		assertEquals(ICharacterPairMatcher.LEFT, matcher.getAnchor());
		// right
		region = matcher.match(document, j + 1);
		assertNotNull(region);
		assertEquals("offset", i, region.getOffset());
		assertEquals("length", length, region.getLength());
		assertEquals(ICharacterPairMatcher.RIGHT, matcher.getAnchor());
	}
}
