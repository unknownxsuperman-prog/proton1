package com.xbit.proton.engine

import android.content.Context
import android.net.Uri
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.xbit.proton.data.model.PriorityOption

/**
 * Parses a KEA (Karnataka Examinations Authority) Option Entry PDF
 * and extracts the priority list as structured [PriorityOption] entries.
 *
 * The KEA PDF typically has rows like:
 * [slno]  [college_code] [college_name]  [branch_code] [branch_name]  [category]
 */
object PdfPriorityParser {

    fun parse(context: Context, uri: Uri): List<PriorityOption> {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
        val options = mutableListOf<PriorityOption>()

        inputStream.use { stream ->
            val reader = PdfReader(stream)
            val doc = PdfDocument(reader)
            val sb = StringBuilder()

            for (page in 1..doc.numberOfPages) {
                sb.append(PdfTextExtractor.getTextFromPage(doc.getPage(page)))
                sb.append("\n")
            }
            doc.close()

            val text = sb.toString()
            options.addAll(parseText(text))
        }

        return options
    }

    /**
     * Parses the extracted PDF text into priority options.
     * KEA format: lines starting with a number followed by college code.
     */
    private fun parseText(text: String): List<PriorityOption> {
        val options = mutableListOf<PriorityOption>()
        // Pattern: slno followed by college code (4 chars), college name, branch code, branch name, category
        val lineRegex = Regex(
            """^(\d+)\s+([A-Z0-9]{3,6})\s+(.+?)\s+([A-Z]{2,4})\s+(.+?)\s+(GM|SC|ST|OBC|2A|2B|3A|3B|HK\w*)$"""
        )

        for (line in text.lines()) {
            val trimmed = line.trim()
            lineRegex.matchEntire(trimmed)?.let { m ->
                options.add(
                    PriorityOption(
                        slno = m.groupValues[1].toIntOrNull() ?: 0,
                        collegeCode = m.groupValues[2],
                        collegeName = m.groupValues[3].trim(),
                        branchCode = m.groupValues[4],
                        branchName = m.groupValues[5].trim(),
                        category = m.groupValues[6]
                    )
                )
            }
        }

        // Fallback: simpler token-based parse if regex yields nothing
        if (options.isEmpty()) {
            return parseTextFallback(text)
        }
        return options
    }

    private fun parseTextFallback(text: String): List<PriorityOption> {
        val options = mutableListOf<PriorityOption>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val slno = line.split(Regex("\\s+")).firstOrNull()?.toIntOrNull()
            if (slno != null && line.length > 5) {
                val parts = line.split(Regex("\\s{2,}"))
                if (parts.size >= 4) {
                    options.add(
                        PriorityOption(
                            slno = slno,
                            collegeCode = parts.getOrElse(1) { "" },
                            collegeName = parts.getOrElse(2) { "" },
                            branchCode = parts.getOrElse(3) { "" },
                            branchName = parts.getOrElse(4) { "" },
                            category = parts.lastOrNull() ?: "GM"
                        )
                    )
                }
            }
            i++
        }
        return options
    }
}
