package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.*
import java.util.regex.Pattern

data class SyntaxScheme(
    val tagColor: Color,
    val attrColor: Color,
    val valueColor: Color,
    val commentColor: Color,
    val entityColor: Color,
    val punctuationColor: Color,
    val defaultColor: Color,
    val highlightColor: Color
)

object SyntaxThemes {
    val Dark = SyntaxScheme(
        tagColor = SyntaxTag,
        attrColor = SyntaxAttribute,
        valueColor = SyntaxValue,
        commentColor = SyntaxComment,
        entityColor = SyntaxEntity,
        punctuationColor = SyntaxPunctuation,
        defaultColor = SyntaxContent,
        highlightColor = Color(0xFFFFD54F).copy(alpha = 0.45f)
    )

    val Light = SyntaxScheme(
        tagColor = SyntaxTagLight,
        attrColor = SyntaxAttributeLight,
        valueColor = SyntaxValueLight,
        commentColor = SyntaxCommentLight,
        entityColor = SyntaxEntityLight,
        punctuationColor = SyntaxPunctuationLight,
        defaultColor = SyntaxContentLight,
        highlightColor = Color(0xFFFFD54F).copy(alpha = 0.55f)
    )
}

object SyntaxHighlighter {
    private val COMMENT_REGEX = Pattern.compile("<!--[\\s\\S]*?-->")
    private val DECL_REGEX = Pattern.compile("<\\?[\\s\\S]*?\\?>|<!DOCTYPE[\\s\\S]*?>", Pattern.CASE_INSENSITIVE)
    private val TAG_REGEX = Pattern.compile("</?([a-zA-Z0-9_:-]+)((?:\\s+[a-zA-Z0-9_:-]+(?:\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+))?)*)\\s*(/?)>")
    private val ATTR_REGEX = Pattern.compile("([a-zA-Z0-9_:-]+)(?:\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+))?")
    private val ENTITY_REGEX = Pattern.compile("&[a-zA-Z0-9#]+;")

    /**
     * Expands literal tab characters ('\t') and adjusts space-based indentation levels
     * to visually match the selected tabSize (2, 4, or 8 spaces).
     * If a file uses 2 or 4 space indentation, scaling ensures it displays according to user preference.
     */
    fun expandTabs(text: String, tabSize: Int): String {
        val targetSpaces = tabSize.coerceAtLeast(1)
        val tabReplaced = if (text.contains('\t')) {
            text.replace("\t", " ".repeat(targetSpaces))
        } else {
            text
        }

        // Also scale leading indentation spaces if tabSize is changed from 2 (e.g. 4 or 8)
        if (targetSpaces != 2 && tabReplaced.startsWith("  ")) {
            val leadingSpacesCount = tabReplaced.takeWhile { it == ' ' }.length
            // Check if leading spaces are multiples of 2
            if (leadingSpacesCount >= 2 && leadingSpacesCount % 2 == 0) {
                val indentLevel = leadingSpacesCount / 2
                val adjustedSpaces = " ".repeat(indentLevel * targetSpaces)
                return adjustedSpaces + tabReplaced.substring(leadingSpacesCount)
            }
        }

        return tabReplaced
    }

    /**
     * Highlights an individual line or small chunk.
     * Takes tabSize into account to accurately display indentation.
     */
    fun highlightLine(
        rawLine: String,
        isDark: Boolean,
        tabSize: Int = 2,
        searchQuery: String = ""
    ): AnnotatedString {
        val line = expandTabs(rawLine, tabSize)
        if (line.isEmpty()) return AnnotatedString("")
        val scheme = if (isDark) SyntaxThemes.Dark else SyntaxThemes.Light

        if (line.length > 5000) {
            return buildAnnotatedString {
                append(line)
                addStyle(SpanStyle(color = scheme.defaultColor, fontFamily = FontFamily.Monospace), 0, line.length)
                if (searchQuery.isNotEmpty()) {
                    applySearchHighlight(this, line, searchQuery, scheme.highlightColor)
                }
            }
        }

        return buildAnnotatedString {
            append(line)
            addStyle(SpanStyle(color = scheme.defaultColor, fontFamily = FontFamily.Monospace), 0, line.length)

            // 1. Comments
            val commentMatcher = COMMENT_REGEX.matcher(line)
            while (commentMatcher.find()) {
                addStyle(
                    SpanStyle(
                        color = scheme.commentColor,
                        fontStyle = FontStyle.Italic
                    ),
                    commentMatcher.start(),
                    commentMatcher.end()
                )
            }

            // 2. XML / DOCTYPE Declarations
            val declMatcher = DECL_REGEX.matcher(line)
            while (declMatcher.find()) {
                addStyle(
                    SpanStyle(color = scheme.entityColor, fontWeight = FontWeight.SemiBold),
                    declMatcher.start(),
                    declMatcher.end()
                )
            }

            // 3. Tags & Attributes
            val tagMatcher = TAG_REGEX.matcher(line)
            while (tagMatcher.find()) {
                val tagStart = tagMatcher.start()
                val tagEnd = tagMatcher.end()

                addStyle(SpanStyle(color = scheme.punctuationColor), tagStart, tagStart + if (line.startsWith("</", tagStart)) 2 else 1)
                addStyle(SpanStyle(color = scheme.punctuationColor), tagEnd - 1, tagEnd)

                val tagNameStart = tagMatcher.start(1)
                val tagNameEnd = tagMatcher.end(1)
                if (tagNameStart != -1 && tagNameEnd != -1) {
                    addStyle(
                        SpanStyle(color = scheme.tagColor, fontWeight = FontWeight.Bold),
                        tagNameStart,
                        tagNameEnd
                    )
                }

                val attrsStart = tagMatcher.start(2)
                val attrsEnd = tagMatcher.end(2)
                if (attrsStart != -1 && attrsEnd > attrsStart) {
                    val attrsSubstring = line.substring(attrsStart, attrsEnd)
                    val attrMatcher = ATTR_REGEX.matcher(attrsSubstring)
                    while (attrMatcher.find()) {
                        val aNameStart = attrsStart + attrMatcher.start(1)
                        val aNameEnd = attrsStart + attrMatcher.end(1)
                        addStyle(SpanStyle(color = scheme.attrColor), aNameStart, aNameEnd)

                        val valGroup = attrMatcher.group(2)
                        if (valGroup != null) {
                            val aValStart = attrsStart + attrMatcher.start(2)
                            val aValEnd = attrsStart + attrMatcher.end(2)
                            addStyle(SpanStyle(color = scheme.valueColor), aValStart, aValEnd)
                        }
                    }
                }
            }

            // 4. Entities
            val entityMatcher = ENTITY_REGEX.matcher(line)
            while (entityMatcher.find()) {
                addStyle(SpanStyle(color = scheme.entityColor), entityMatcher.start(), entityMatcher.end())
            }

            // 5. Search highlighting overlay
            if (searchQuery.isNotEmpty()) {
                applySearchHighlight(this, line, searchQuery, scheme.highlightColor)
            }
        }
    }

    private fun applySearchHighlight(
        builder: AnnotatedString.Builder,
        text: String,
        query: String,
        highlightColor: Color
    ) {
        var index = text.indexOf(query, 0, ignoreCase = true)
        while (index >= 0) {
            builder.addStyle(
                SpanStyle(
                    background = highlightColor,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                index,
                index + query.length
            )
            index = text.indexOf(query, index + query.length, ignoreCase = true)
        }
    }
}
