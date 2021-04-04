package org.bestever.bebot;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class KeyValueParserTest {
    @Test
    public void canParseNormalPairs() throws InputException {
        final HashMap<String, String> expected = new HashMap<>();
        expected.put("abc", "jkl");
        expected.put("def", "iop");

        final Map<String, String> result = new KeyValueParser("abc=jkl def=iop").parse();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void canParseQuotedPairs() throws InputException {
        final HashMap<String, String> expected = new HashMap<>();
        expected.put("abc", "j\"kl");
        expected.put("abc1", "foo");
        expected.put("def", "i'op");
        expected.put("def1", "zoo");

        final Map<String, String> result = new KeyValueParser("abc='j\"kl' abc1='foo' def=\"i'op\" def1=\"zoo\"").parse();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void canTrimAndParseMixedPairs() throws InputException {
        final HashMap<String, String> expected = new HashMap<>();
        expected.put("abc", "j\"kl");
        expected.put("abc1", "foo");
        expected.put("def", "i'op");
        expected.put("def1", "zoo");

        final Map<String, String> result = new KeyValueParser(" abc='j\"kl' abc1=foo def=\"i'op\" def1=\"zoo\"  ").parse();
        Assert.assertEquals(expected, result);
    }

    @Test(expected = IllegalStateException.class)
    public void canNotBeCalledTwice() throws InputException {
        final KeyValueParser parser = new KeyValueParser("abc=jkl def=iop");
        parser.parse();
        parser.parse();
    }
}
