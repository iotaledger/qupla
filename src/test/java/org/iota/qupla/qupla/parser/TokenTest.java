package org.iota.qupla.qupla.parser;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Unit test of {@link Token} class
 */
public class TokenTest {
    private static final int TEST_TOKEN_ID = ThreadLocalRandom.current().nextInt(1);
    private static final int TEST_SYMBOL = ThreadLocalRandom.current().nextInt(1);
    private static final int TEST_COL_NR = ThreadLocalRandom.current().nextInt(1);
    private static final String TEST_TEXT = "token text";
    
    private final Token underTest = new Token(0, 0, Mockito.mock(QuplaSource.class), -1, -1, "");

    @Test
    public void when_resetId_then_newInstanceExpected() {
        // given
        Assert.assertNotEquals(TEST_TOKEN_ID, underTest.id);

        // when
        Token token = underTest.resetId(TEST_TOKEN_ID);

        // then
        Assert.assertNotSame(underTest, token);
        Assert.assertEquals(TEST_TOKEN_ID, token.id);
    }

    @Test
    public void when_resetSymbol_then_newInstanceExpected() {
        // given
        Assert.assertNotEquals(TEST_SYMBOL, underTest.symbol);

        // when
        Token token = underTest.resetSymbol(TEST_SYMBOL);

        // then
        Assert.assertNotSame(underTest, token);
        Assert.assertEquals(TEST_SYMBOL, token.symbol);
    }

    @Test
    public void when_resetText_then_newInstanceExpected() {
        // given
        Assert.assertNotEquals(TEST_TEXT, underTest.text);

        // when
        Token token = underTest.resetText(TEST_TEXT);

        // then
        Assert.assertNotSame(underTest, token);
        Assert.assertEquals(TEST_TEXT, token.text);
    }

    @Test
    public void when_resetText2_then_newInstanceExpected() {
        // given
        Assert.assertNotEquals(TEST_TEXT, underTest.text);

        // when
        Token token = underTest.resetText(TEST_TEXT, TEST_TOKEN_ID, TEST_COL_NR);

        // then
        Assert.assertNotSame(underTest, token);
        Assert.assertEquals(TEST_TEXT, token.text);
    }
}
