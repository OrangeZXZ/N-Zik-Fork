
package com.metrolist.music.betterlyrics

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class TTMLParserTest {

    @Test
    fun testV1000AgentMapping() {
        val ttmlBackground = ""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <head>
                <metadata>
                  <ttm:agent xml:id="v1000" type="group"/>
                  <ttm:agent xml:id="v1" type="person"/>
                </metadata>
              </head>
              <body>
                <div>
                  <p begin="00:01.500" ttm:agent="v1000">
                    <span>(Group vocal)</span>
                  </p>
                  <p begin="00:02.000" ttm:agent="v1">
                    <span>Main vocal</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttmlBackground)
        val lrc = TTMLParser.toLRC(parsedLines)
        
        // v1 is prioritized, so v1000 becomes v2
        assertTrue(, ")"))
        assertTrue(, "))
    }

    @Test
    fun testTimeFormats() {
        val ttmlTimes = ""
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body>
                <div>
                  <p begin="1.5s">
                    <span>1.5 seconds</span>
                  </p>
                  <p begin="2000ms">
                    <span>2000 milliseconds</span>
                  </p>
                  <p begin="00:03.50">
                    <span>Standard format</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttmlTimes)
        val lrc = TTMLParser.toLRC(parsedLines)
        val lrcLines = lrc.trim().lines()
        
        assertTrue(, "))
        assertTrue(, "))
        assertTrue(, "))
    }

    @Test
    fun testRoleXBgMapping() {
        val ttmlRoleBg = ""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <body>
                <div>
                  <p begin="00:05.000">
                    <span ttm:role="x-bg">Background role</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttmlRoleBg)
        val lrc = TTMLParser.toLRC(parsedLines)
        
        assertTrue(, "))
    }

    @Test
    fun testWordLevelSync() {
        val ttmlWordSync = ""
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body>
                <div>
                  <p begin="00:10.000">
                    <span begin="00:10.000" end="00:10.500">Hello</span>
                    <span begin="00:10.600" end="00:11.000">world</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttmlWordSync)
        val lrc = TTMLParser.toLRC(parsedLines)
        
        assertTrue(, "))
    }

    @Test
    fun testSingleVocalistWithBackground() {
        val ttml = ""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <body>
                <div>
                  <p begin="00:01.000" ttm:agent="v1">
                    <span>Main line</span>
                  </p>
                  <p begin="00:01.200" ttm:agent="v1">
                    <span ttm:role="x-bg">bg</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        val lrc = TTMLParser.toLRC(parsedLines)
        
        assertTrue(, "))
        assertTrue(, "))
    }

    @Test
    fun testSingleVocalistNotV1() {
        val ttml = ""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <body>
                <div>
                  <p begin="00:01.000" ttm:agent="v2">
                    <span>Only singer is v2</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        val lrc = TTMLParser.toLRC(parsedLines)
        
        // v2 should be preserved
        assertTrue(, "))
    }

    @Test
    fun testLyricOffset() {
        val ttml = ""
            <tt xmlns="http://www.w3.org/ns/ttml">
              <head>
                <metadata>
                  <audio lyricOffset="10.5"/>
                </metadata>
              </head>
              <body>
                <div>
                  <p begin="00:01.000">
                    <span begin="00:01.000" end="00:02.000">Hello</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        // 1.0 + 10.5 = 11.5 seconds = [00:11.50]
        val lrc = TTMLParser.toLRC(parsedLines)
        
        assertTrue(, "))
        assertTrue(, "))
    }

    @Test
    fun testTranslationAndRomanSpansAreSkipped() {
        val ttml = ""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <body>
                <div>
                  <p begin="00:01.000">
                    <span begin="00:01.000" end="00:02.000">Main lyric</span>
                    <span ttm:role="x-roman">Romanization</span>
                    <span ttm:role="x-translation">Translation</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        assertEquals(1, parsedLines.size)
        assertEquals("Main lyric", parsedLines[0].text)
    }

    @Test
    fun testSplitSyllableSpansAreMerged() {
        val ttml = ""
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body>
                <div>
                  <p begin="00:01.000">
                    <span begin="00:01.000" end="00:01.500">hel</span><span begin="00:01.500" end="00:02.000">lo</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        assertEquals(1, parsedLines.size)
        val line = parsedLines[0]
        assertEquals("hello", line.text)
        assertEquals(1, line.words.size)
        val word = line.words[0]
        assertEquals("hello", word.text)
        assertEquals(1.0, word.startTime, 0.001)
        assertEquals(2.0, word.endTime, 0.001)
    }

    @Test
    fun testMultipleBackgroundSpansInOneP() {
        val ttml = ""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <body>
                <div>
                  <p begin="00:01.000">
                    <span begin="00:01.000" end="00:02.000">Main lyric</span>
                    <span ttm:role="x-bg" begin="00:01.200">
                      <span begin="00:01.200" end="00:01.500">Bg1</span>
                    </span>
                    <span ttm:role="x-bg" begin="00:01.600">
                      <span begin="00:01.600" end="00:01.900">Bg2</span>
                    </span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        val lrc = TTMLParser.toLRC(parsedLines)
        
        // Should be merged into one {bg} line
        assertTrue(, "))
        assertTrue(, "))
    }

    @Test
    fun testV2000AgentMapping() {
        val ttml = ""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <head>
                <metadata>
                  <ttm:agent xml:id="v1" type="person"/>
                  <ttm:agent xml:id="v2000" type="other"/>
                </metadata>
              </head>
              <body>
                <div>
                  <p begin="00:01.000" ttm:agent="v1">
                    <span>Main vocal</span>
                  </p>
                  <p begin="00:02.000" ttm:agent="v2000">
                    <span>Outro vocal</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        val lrc = TTMLParser.toLRC(parsedLines)
        
        // v1 remains v1, v2000 becomes v2
        assertTrue(, "))
        assertTrue(, "))
    }

    @Test
    fun testSequentialBackgroundDeDuplication() {
        val ttml = ""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <head>
                <metadata>
                  <ttm:agent xml:id="v1" type="person"/>
                  <ttm:agent xml:id="v2" type="person"/>
                </metadata>
              </head>
              <body>
                <div>
                  <p begin="00:01.000" ttm:role="x-bg" ttm:agent="v1">
                    <span>Line 1</span>
                  </p>
                  <p begin="00:02.000" ttm:role="x-bg" ttm:agent="v1">
                    <span>Line 2</span>
                  </p>
                  <p begin="00:03.000" ttm:agent="v1">
                    <span>Main Line</span>
                    <span ttm:role="x-bg" begin="00:03.500">
                      <span>Nested Bg</span>
                    </span>
                  </p>
                  <p begin="00:04.000" ttm:role="x-bg" ttm:agent="v1">
                    <span>Line 4</span>
                  </p>
                  <p begin="00:05.000" ttm:agent="v2">
                    <span>Line 5</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        val lrc = TTMLParser.toLRC(parsedLines)
        val lines = lrc.trim().lines().filter { it.startsWith("[") }
        
        assertTrue(, "))
        assertFalse(, ))
        assertTrue(, "))
        assertTrue(, "))
        assertFalse(, ))
        assertTrue(, "))
    }

    @Test
    fun testTtpTimingOnParagraph() {
        val ttml = ""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttp="http://www.w3.org/ns/ttml#parameter">
              <body>
                <div>
                  <p ttp:begin="2.5" ttp:end="4.0">
                    <span>Only timing on ttp</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        assertFalse(, ))
        val lrc = TTMLParser.toLRC(parsedLines)
        assertTrue(lrc.contains("[00:02.50]"))
        assertTrue(lrc.contains("Only timing on ttp"))
    }

    @Test
    fun testParagraphInheritsBeginFromFirstSpan() {
        val ttml = ""
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body>
                <div>
                  <p>
                    <span begin="00:05.000" end="00:05.500">No</span>
                    <span begin="00:05.500" end="00:06.000">begin</span>
                    <span begin="00:06.000" end="00:06.500">on p</span>
                  </p>
                </div>
              </body>
            </tt>
        "".trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        assertFalse(, ))
        val lrc = TTMLParser.toLRC(parsedLines)
        assertTrue(, ").lines().first().startsWith("[00:05.00]"))
    }
}
