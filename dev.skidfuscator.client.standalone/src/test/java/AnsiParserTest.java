// AnsiParserTest.java

import dev.skidfuscator.obfuscator.gui.ansi.AnsiParser;
import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AnsiParserTest {

    @Test
    void testPlainTextParsing() {
        String input = "Hello, World!";
        List<AnsiParser.AnsiSegment> segments = AnsiParser.parse(input);

        assertEquals(1, segments.size());
        AnsiParser.AnsiSegment segment = segments.get(0);

        assertEquals("Hello, World!", segment.getText());
        assertNull(segment.getForeground());
        assertNull(segment.getBackground());
        assertFalse(segment.isBold());
        assertFalse(segment.isItalic());
        assertFalse(segment.isUnderline());
        assertFalse(segment.isStrikethrough());
    }

    @Test
    void testBoldFormatting() {
        String input = "\u001B[1mBold Text\u001B[0m";
        List<AnsiParser.AnsiSegment> segments = AnsiParser.parse(input);

        assertEquals(1, segments.size());
        AnsiParser.AnsiSegment segment = segments.get(0);

        assertEquals("Bold Text", segment.getText());
        assertTrue(segment.isBold());
        assertFalse(segment.isItalic());
    }

    @Test
    void testItalicFormatting() {
        String input = "\u001B[3mItalic Text\u001B[0m";
        List<AnsiParser.AnsiSegment> segments = AnsiParser.parse(input);

        assertEquals(1, segments.size());
        AnsiParser.AnsiSegment segment = segments.get(0);

        assertEquals("Italic Text", segment.getText());
        assertFalse(segment.isBold());
        assertTrue(segment.isItalic());
    }

    @Test
    void testColorFormatting() {
        String input = "\u001B[31mRed Text\u001B[0m";
        List<AnsiParser.AnsiSegment> segments = AnsiParser.parse(input);

        assertEquals(1, segments.size());
        AnsiParser.AnsiSegment segment = segments.get(0);

        assertEquals("Red Text", segment.getText());
        assertEquals(new Color(205, 49, 49), segment.getForeground());
        assertNull(segment.getBackground());
    }

    @Test
    void testBackgroundColorFormatting() {
        String input = "\u001B[41mRed Background\u001B[0m";
        List<AnsiParser.AnsiSegment> segments = AnsiParser.parse(input);

        assertEquals(1, segments.size());
        AnsiParser.AnsiSegment segment = segments.get(0);

        assertEquals("Red Background", segment.getText());
        assertEquals(new Color(205, 49, 49), segment.getBackground());
        assertNull(segment.getForeground());
    }

    @Test
    void testMultipleSegments() {
        String input = "\u001B[1;31mBold Red\u001B[0m Normal \u001B[32mGreen\u001B[0m";
        List<AnsiParser.AnsiSegment> segments = AnsiParser.parse(input);

        assertEquals(3, segments.size());

        // Check first segment (Bold Red)
        AnsiParser.AnsiSegment segment1 = segments.get(0);
        assertEquals("Bold Red", segment1.getText());
        assertEquals(new Color(205, 49, 49), segment1.getForeground());
        assertTrue(segment1.isBold());

        // Check second segment (Normal)
        AnsiParser.AnsiSegment segment2 = segments.get(1);
        assertEquals(" Normal ", segment2.getText());
        assertNull(segment2.getForeground());
        assertFalse(segment2.isBold());

        // Check third segment (Green)
        AnsiParser.AnsiSegment segment3 = segments.get(2);
        assertEquals("Green", segment3.getText());
        assertEquals(new Color(13, 188, 121), segment3.getForeground());
    }

    @Test
    void testNestedStyles() {
        String input = "\u001B[1m\u001B[32mBold Green\u001B[31m and Red\u001B[0m";
        List<AnsiParser.AnsiSegment> segments = AnsiParser.parse(input);

        assertEquals(2, segments.size());

        // Check first segment (Bold Green)
        AnsiParser.AnsiSegment segment1 = segments.get(0);
        assertEquals("Bold Green", segment1.getText());
        assertEquals(new Color(13, 188, 121), segment1.getForeground());
        assertTrue(segment1.isBold());

        // Check second segment (Bold Red)
        AnsiParser.AnsiSegment segment2 = segments.get(1);
        assertEquals(" and Red", segment2.getText());
        assertEquals(new Color(205, 49, 49), segment2.getForeground());
        assertTrue(segment2.isBold());
    }

    @Test
    void testComplexFormatting() {
        String input = "\u001B[1;3;4;31mBold Italic Underlined Red\u001B[0m";
        List<AnsiParser.AnsiSegment> segments = AnsiParser.parse(input);

        assertEquals(1, segments.size());
        AnsiParser.AnsiSegment segment = segments.get(0);

        assertEquals("Bold Italic Underlined Red", segment.getText());
        assertEquals(new Color(205, 49, 49), segment.getForeground());
        assertTrue(segment.isBold());
        assertTrue(segment.isItalic());
        assertTrue(segment.isUnderline());
    }

    @Test
    void testBrightColors() {
        String input = "\u001B[91mBright Red\u001B[0m\u001B[96mBright Cyan\u001B[0m";
        List<AnsiParser.AnsiSegment> segments = AnsiParser.parse(input);

        System.out.println(segments);
        assertEquals(2, segments.size());

        // Check bright red
        AnsiParser.AnsiSegment segment1 = segments.get(0);
        assertEquals("Bright Red", segment1.getText());
        assertEquals(new Color(241, 76, 76), segment1.getForeground());

        // Check bright cyan
        AnsiParser.AnsiSegment segment2 = segments.get(1);
        assertEquals("Bright Cyan", segment2.getText());
        assertEquals(new Color(41, 184, 219), segment2.getForeground());
    }

    @Test
    void testEmptyString() {
        String input = "";
        List<AnsiParser.AnsiSegment> segments = AnsiParser.parse(input);
        assertTrue(segments.isEmpty());
    }

    @Test
    void testResetBetweenSegments() {
        String input = "\u001B[1;31mBold Red\u001B[0m\u001B[32mGreen\u001B[0m";
        List<AnsiParser.AnsiSegment> segments = AnsiParser.parse(input);

        assertEquals(2, segments.size());

        // First segment should be bold and red
        AnsiParser.AnsiSegment segment1 = segments.get(0);
        assertTrue(segment1.isBold());
        assertEquals(new Color(205, 49, 49), segment1.getForeground());

        // Second segment should be just green
        AnsiParser.AnsiSegment segment2 = segments.get(1);
        assertFalse(segment2.isBold());
        assertEquals(new Color(13, 188, 121), segment2.getForeground());
    }
}