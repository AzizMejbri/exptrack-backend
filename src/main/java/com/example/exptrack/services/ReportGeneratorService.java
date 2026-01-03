package com.example.exptrack.services;

import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportGeneratorService {

  // ========== CSV GENERATION ==========
  public byte[] generateCsv(String title, List<Map<String, Object>> data, List<String> headers) {
    StringWriter writer = new StringWriter();

    // Write headers
    writer.write(String.join(",", headers));
    writer.write("\n");

    // Write data
    for (Map<String, Object> row : data) {
      List<String> values = new ArrayList<>();
      for (String header : headers) {
        Object value = row.get(header);
        String stringValue = value != null ? escapeCsvValue(value.toString()) : "";
        values.add(stringValue);
      }
      writer.write(String.join(",", values));
      writer.write("\n");
    }

    return writer.toString().getBytes(StandardCharsets.UTF_8);
  }

  private String escapeCsvValue(String value) {
    if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  // ========== HTML GENERATION ==========
  public byte[] generateHtml(String title, Map<String, Object> reportData) {
    String html = buildHtmlReport(title, reportData);
    return html.getBytes(StandardCharsets.UTF_8);
  }

  private String buildHtmlReport(String title, Map<String, Object> data) {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>%s</title>
            <style>
                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    margin: 40px;
                    line-height: 1.6;
                    color: #333;
                }
                .report-container {
                    max-width: 1200px;
                    margin: 0 auto;
                    background: white;
                    padding: 40px;
                    border-radius: 12px;
                    box-shadow: 0 4px 20px rgba(0,0,0,0.1);
                }
                h1 {
                    color: #2563eb;
                    margin-bottom: 8px;
                    border-bottom: 3px solid #2563eb;
                    padding-bottom: 10px;
                }
                .meta {
                    color: #666;
                    margin-bottom: 32px;
                    font-size: 14px;
                }
                .summary-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 20px;
                    margin-bottom: 40px;
                }
                .stat-card {
                    background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
                    padding: 24px;
                    border-radius: 10px;
                    border-left: 4px solid #2563eb;
                    transition: transform 0.2s ease;
                }
                .stat-card:hover {
                    transform: translateY(-2px);
                }
                .stat-label {
                    font-size: 12px;
                    color: #64748b;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                    margin-bottom: 8px;
                }
                .stat-value {
                    font-size: 28px;
                    font-weight: bold;
                    color: #1e293b;
                }
                .positive { color: #10b981; font-weight: bold; }
                .negative { color: #ef4444; font-weight: bold; }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 24px 0;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
                }
                th {
                    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
                    color: white;
                    padding: 16px;
                    text-align: left;
                    font-weight: 600;
                }
                td {
                    padding: 16px;
                    border-bottom: 1px solid #e2e8f0;
                    vertical-align: top;
                }
                tr:nth-child(even) { background: #f8fafc; }
                tr:hover { background: #f1f5f9; }
                .footer {
                    margin-top: 40px;
                    padding-top: 20px;
                    border-top: 1px solid #e2e8f0;
                    color: #64748b;
                    font-size: 14px;
                    text-align: center;
                }
            </style>
        </head>
        <body>
            <div class="report-container">
                <h1>📊 %s</h1>
                <div class="meta">
                    Generated on: %s | Report ID: %s
                </div>
                %s
                <div class="footer">
                    Generated by Expense Tracker | %s
                </div>
            </div>
        </body>
        </html>
        """.formatted(
        title,
        title,
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
        UUID.randomUUID().toString().substring(0, 8),
        generateHtmlContent(data),
        LocalDate.now().getYear());
  }

  private String generateHtmlContent(Map<String, Object> data) {
    StringBuilder content = new StringBuilder();

    // Add summary if available
    if (data.containsKey("summary")) {
      content.append("<div class='summary-grid'>\n");
      Map<String, Object> summary = (Map<String, Object>) data.get("summary");
      for (Map.Entry<String, Object> entry : summary.entrySet()) {
        String label = entry.getKey();
        Object value = entry.getValue();
        content.append(String.format(
            "<div class='stat-card'>\n" +
                "  <div class='stat-label'>%s</div>\n" +
                "  <div class='stat-value'>%s</div>\n" +
                "</div>\n",
            formatLabel(label),
            formatValue(value)));
      }
      content.append("</div>\n");
    }

    // Add tables if available
    if (data.containsKey("tables")) {
      List<Map<String, Object>> tables = (List<Map<String, Object>>) data.get("tables");
      for (Map<String, Object> table : tables) {
        content.append(generateHtmlTable(table));
      }
    }

    return content.toString();
  }

  private String generateHtmlTable(Map<String, Object> table) {
    String title = (String) table.getOrDefault("title", "Data");
    List<String> headers = (List<String>) table.getOrDefault("headers", List.of());
    List<List<Object>> rows = (List<List<Object>>) table.getOrDefault("rows", List.of());

    StringBuilder tableHtml = new StringBuilder();
    tableHtml.append("<h3>").append(title).append("</h3>\n");
    tableHtml.append("<table>\n<thead>\n<tr>\n");

    for (String header : headers) {
      tableHtml.append("<th>").append(header).append("</th>\n");
    }
    tableHtml.append("</tr>\n</thead>\n<tbody>\n");

    for (List<Object> row : rows) {
      tableHtml.append("<tr>\n");
      for (Object cell : row) {
        tableHtml.append("<td>").append(formatValue(cell)).append("</td>\n");
      }
      tableHtml.append("</tr>\n");
    }

    tableHtml.append("</tbody>\n</table>\n");
    return tableHtml.toString();
  }

  private String formatLabel(String label) {
    return label.replaceAll("([A-Z])", " $1").replaceAll("^\\s+", "").toLowerCase();
  }

  private String formatValue(Object value) {
    if (value instanceof Number) {
      double num = ((Number) value).doubleValue();
      if (Math.abs(num) >= 1000) {
        return String.format("$%,.0f", num);
      } else if (Math.abs(num) >= 100) {
        return String.format("$%,.1f", num);
      } else {
        return String.format("$%,.2f", num);
      }
    }
    return value.toString();
  }

  // ========== MARKDOWN GENERATION ==========
  public byte[] generateMarkdown(String title, Map<String, Object> data) {
    StringBuilder md = new StringBuilder();

    md.append("# ").append(title).append("\n\n");
    md.append("**Generated:** ").append(LocalDate.now()).append("\n\n");

    // Add summary
    if (data.containsKey("summary")) {
      md.append("## Summary\n\n");
      Map<String, Object> summary = (Map<String, Object>) data.get("summary");
      for (Map.Entry<String, Object> entry : summary.entrySet()) {
        md.append("- **").append(formatLabel(entry.getKey())).append(":** ")
            .append(formatValue(entry.getValue())).append("\n");
      }
      md.append("\n");
    }

    // Add tables
    if (data.containsKey("tables")) {
      List<Map<String, Object>> tables = (List<Map<String, Object>>) data.get("tables");
      for (Map<String, Object> table : tables) {
        md.append(generateMarkdownTable(table));
      }
    }

    return md.toString().getBytes(StandardCharsets.UTF_8);
  }

  private String generateMarkdownTable(Map<String, Object> table) {
    StringBuilder md = new StringBuilder();
    String title = (String) table.getOrDefault("title", "Data");
    List<String> headers = (List<String>) table.getOrDefault("headers", List.of());
    List<List<Object>> rows = (List<List<Object>>) table.getOrDefault("rows", List.of());

    md.append("## ").append(title).append("\n\n");

    // Header
    md.append("| ");
    for (String header : headers) {
      md.append(header).append(" | ");
    }
    md.append("\n");

    // Separator
    md.append("|");
    for (int i = 0; i < headers.size(); i++) {
      md.append("---|");
    }
    md.append("\n");

    // Rows
    for (List<Object> row : rows) {
      md.append("| ");
      for (Object cell : row) {
        md.append(formatValue(cell)).append(" | ");
      }
      md.append("\n");
    }

    md.append("\n");
    return md.toString();
  }

  // ========== JSON GENERATION ==========
  public byte[] generateJson(Object data) {
    // Simple JSON serialization
    String json = convertToJson(data, 0);
    return json.getBytes(StandardCharsets.UTF_8);
  }

  private String convertToJson(Object data, int indent) {
    StringBuilder json = new StringBuilder();
    String indentStr = "  ".repeat(indent);

    if (data == null) {
      return "null";
    } else if (data instanceof String) {
      return "\"" + escapeJson((String) data) + "\"";
    } else if (data instanceof Number || data instanceof Boolean) {
      return data.toString();
    } else if (data instanceof Map) {
      json.append("{\n");
      Map<?, ?> map = (Map<?, ?>) data;
      int i = 0;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (i > 0)
          json.append(",\n");
        json.append(indentStr).append("  \"")
            .append(escapeJson(entry.getKey().toString()))
            .append("\": ")
            .append(convertToJson(entry.getValue(), indent + 1));
        i++;
      }
      json.append("\n").append(indentStr).append("}");
      return json.toString();
    } else if (data instanceof List) {
      json.append("[\n");
      List<?> list = (List<?>) data;
      for (int i = 0; i < list.size(); i++) {
        if (i > 0)
          json.append(",\n");
        json.append(indentStr).append("  ")
            .append(convertToJson(list.get(i), indent + 1));
      }
      json.append("\n").append(indentStr).append("]");
      return json.toString();
    } else {
      return "\"" + escapeJson(data.toString()) + "\"";
    }
  }

  private String escapeJson(String str) {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  // ========== SIMPLE PDF/TEXT GENERATION ==========
  public byte[] generateSimplePdf(String title, String content) {
    // Simple text-based "PDF" - for real PDF, use Option 2
    String pdfContent = """
        %s
        %s

        Generated: %s
        Page 1 of 1
        """.formatted(
        title,
        "=".repeat(title.length()),
        LocalDate.now(),
        content);

    return pdfContent.getBytes(StandardCharsets.UTF_8);
  }
}
