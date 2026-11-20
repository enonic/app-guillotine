package com.enonic.app.guillotine.mcp.tools;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class RichTextTest
{
    @Test
    public void testUnclosedTagsAreLinearAndDroppedMarkupLeavesNoText()
    {
        final List<String> inputs = List.of( "<a href=x>t ".repeat( 40000 ), "<script>x ".repeat( 40000 ), "<!-- x".repeat( 40000 ),
                                             "<".repeat( 200000 ), "<a ".repeat( 100000 ), "<li ".repeat( 100000 ),
                                             "<img src=x ".repeat( 40000 ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 5 ), () -> inputs.forEach( RichText::convert ) );

        final Map<String, Object> converted =
            RichText.convert( "<p><a href=\"https://a.example\">one</a> <a href=x>open <a href=\"https://b.example\">two</a></p>" +
                                  "<script>s<p>x</p></script><!-- c --><ul><li>first</li><li>second</li></ul><p>tail&nbsp;&#x41;</p>" );
        assertEquals( "one open two\n- first\n- second\ntail A", converted.get( "text" ) );
        assertEquals( List.of( Map.of( "type", "external", "url", "https://a.example", "text", "one" ),
                               Map.of( "type", "external", "url", "https://b.example", "text", "two" ) ), converted.get( "links" ) );
    }

    @Test
    public void testNamedEntitiesAreDecodedAndUnknownOnesKept()
    {
        assertEquals( "café © ½ – “quoted” € &nosuch;",
                      RichText.text( "<p>caf&eacute; &copy; &frac12; &ndash; &ldquo;quoted&rdquo; &euro; &nosuch;</p>" ) );
    }

    @Test
    public void testImagesAreLinksWithTheirAltText()
    {
        assertEquals( List.of( Map.of( "type", "external", "url", "https://cdn.example/a.png", "text", "remote" ),
                               Map.of( "type", "image", "id", "img-1", "text", "local" ), Map.of( "type", "image", "id", "media-1" ) ),
                      RichText.convert( "<img src=\"https://cdn.example/a.png\" alt=\"remote\"><img src=\"image://img-1\" alt=\"local\">" +
                                            "<img src=\"media://media-1\">" ).get( "links" ) );
    }

    @Test
    public void testManyLinksAreCapped()
    {
        final Map<String, Object> converted =
            RichText.convert( "<p>" + "<a href=\"https://example.com\">x</a>".repeat( RichText.MAX_LINKS + 3 ) + "</p>" );

        assertEquals( RichText.MAX_LINKS, ( (List<?>) converted.get( "links" ) ).size() );
        assertEquals( true, converted.get( "linksTruncated" ) );
        assertNull( RichText.convert( "<a href=\"https://example.com\">x</a>" ).get( "linksTruncated" ) );
    }
}
