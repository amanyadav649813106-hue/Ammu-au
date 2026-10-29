package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.NeonAmber

@Composable
fun MarkdownFormattedText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val sections = parseMarkdownSections(text)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (section in sections) {
            when (section) {
                is MarkdownSection.Header -> {
                    Text(
                        text = section.content,
                        style = when (section.level) {
                            1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = CyberCyan)
                            2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CyberCyan)
                            else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NeonAmber)
                        }
                    )
                }
                is MarkdownSection.CodeBlock -> {
                    CodeBlockCard(
                        code = section.code,
                        language = section.language,
                        onCopy = {
                            copyToClipboard(context, section.code, "Code copied to clipboard")
                        }
                    )
                }
                is MarkdownSection.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        FormattedInlineText(text = section.content, textColor = textColor)
                    }
                }
                is MarkdownSection.Paragraph -> {
                    FormattedInlineText(text = section.content, textColor = textColor)
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    code: String,
    language: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberNavyDark)
            .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = (if (language.isNotBlank()) language else "code").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    fontSize = 11.sp
                )
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = CyberCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = code,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        )
    }
}

@Composable
fun FormattedInlineText(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val annotated = buildAnnotatedString {
        var cursor = 0
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        val matches = boldRegex.findAll(text).toList()

        for (match in matches) {
            if (match.range.first > cursor) {
                withStyle(SpanStyle(color = textColor)) {
                    append(text.substring(cursor, match.range.first))
                }
            }
            val boldContent = match.groupValues[1]
            withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Bold)) {
                append(boldContent)
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            withStyle(SpanStyle(color = textColor)) {
                append(text.substring(cursor))
            }
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        modifier = modifier
    )
}

@Composable
fun MessageActionRow(
    messageText: String,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSpeak,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Speak message",
                tint = CyberCyan,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = {
                copyToClipboard(context, messageText, "Message copied to clipboard")
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy message",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share Nikhil AI Response")
                context.startActivity(shareIntent)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share message",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

fun copyToClipboard(context: Context, text: String, toastMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Nikhil AI", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
}

sealed class MarkdownSection {
    data class Header(val level: Int, val content: String) : MarkdownSection()
    data class CodeBlock(val code: String, val language: String) : MarkdownSection()
    data class BulletItem(val content: String) : MarkdownSection()
    data class Paragraph(val content: String) : MarkdownSection()
}

fun parseMarkdownSections(raw: String): List<MarkdownSection> {
    val lines = raw.lines()
    val result = mutableListOf<MarkdownSection>()

    var inCodeBlock = false
    var codeLang = ""
    val codeBuffer = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                result.add(MarkdownSection.CodeBlock(code = codeBuffer.toString().trimEnd(), language = codeLang))
                codeBuffer.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            codeBuffer.append(line).append("\n")
            continue
        }

        if (trimmed.isEmpty()) continue

        when {
            trimmed.startsWith("### ") -> {
                result.add(MarkdownSection.Header(3, trimmed.removePrefix("### ")))
            }
            trimmed.startsWith("## ") -> {
                result.add(MarkdownSection.Header(2, trimmed.removePrefix("## ")))
            }
            trimmed.startsWith("# ") -> {
                result.add(MarkdownSection.Header(1, trimmed.removePrefix("# ")))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                val bulletText = if (trimmed.startsWith("- ")) trimmed.removePrefix("- ") else trimmed.removePrefix("* ")
                result.add(MarkdownSection.BulletItem(bulletText))
            }
            else -> {
                result.add(MarkdownSection.Paragraph(trimmed))
            }
        }
    }

    if (inCodeBlock && codeBuffer.isNotEmpty()) {
        result.add(MarkdownSection.CodeBlock(code = codeBuffer.toString().trimEnd(), language = codeLang))
    }

    return result
}
