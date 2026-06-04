package com.insightflow.dataingestion.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
public class DynamicFileParser {

    /**
     * HÀM TỔNG ĐIỀU PHỐI: Tự động nhận diện đuôi file để điều hướng
     */
    public List<Map<String, String>> parseFile(InputStream inputStream, String fileName) throws Exception {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
            log.info("📂 Nhận diện file Excel, tiến hành quét toàn bộ các Sheet...");
            return parseExcel(inputStream);
        } else if (lowerName.endsWith(".csv")) {
            log.info("📄 Nhận diện file CSV, tiến hành bóc tách an toàn...");
            return parseCsv(inputStream);
        }
        throw new IllegalArgumentException("Hệ thống chỉ hỗ trợ định dạng .csv, .xlsx, .xls. Định dạng không hợp lệ: " + fileName);
    }

    /**
     * 1. CỖ MÁY ĐỌC CSV (Sử dụng Commons-CSV chuẩn Enterprise, bỏ Regex thủ công)
     */
    private List<Map<String, String>> parseCsv(InputStream inputStream) throws Exception {
        List<Map<String, String>> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build())) {

            // Lấy danh sách header gốc và tiến hành chuẩn hóa (normalize)
            List<String> rawHeaders = csvParser.getHeaderNames();
            if (rawHeaders == null || rawHeaders.isEmpty()) {
                log.warn("⚠️ Tệp CSV tải lên trống rỗng hoặc thiếu dòng tiêu đề!");
                return records;
            }

            Map<String, String> normalizedHeaderMap = new HashMap<>();
            for (String raw : rawHeaders) {
                normalizedHeaderMap.put(raw, normalizeHeader(raw));
            }

            // Quét từng dòng dữ liệu
            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> rowData = new HashMap<>();
                boolean isEmptyRow = true;

                for (String rawHeader : rawHeaders) {
                    String colName = normalizedHeaderMap.get(rawHeader);
                    String rawValue = cleanQuotes(csvRecord.get(rawHeader));

                    if (rawValue != null && !rawValue.isEmpty()) isEmptyRow = false;

                    // Khử trùng và ép kiểu an toàn
                    rowData.put(colName, sanitizeValue(colName, rawValue));
                }

                if (!isEmptyRow) records.add(rowData);
            }
            log.info("✅ Parse CSV thành công {} dòng. Đã khử trùng và chuẩn hóa biến.", records.size());
        }
        return records;
    }

    /**
     * 2. CỖ MÁY ĐỌC EXCEL (Đọc TẤT CẢ các Sheet, ép kiểu mọi loại Cell)
     */
    private List<Map<String, String>> parseExcel(InputStream inputStream) throws Exception {
        List<Map<String, String>> records = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter dataFormatter = new DataFormatter(); // Định dạng an toàn cho cả ngày tháng, số thập phân

            int totalSheets = workbook.getNumberOfSheets();
            for (int sheetIndex = 0; sheetIndex < totalSheets; sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                Iterator<Row> rowIterator = sheet.iterator();

                if (!rowIterator.hasNext()) continue; // Bỏ qua nếu Sheet trắng

                // Đọc và chuẩn hóa Header của Sheet hiện tại
                Row headerRow = rowIterator.next();
                List<String> normalizedHeaders = new ArrayList<>();
                for (Cell cell : headerRow) {
                    String rawHeader = dataFormatter.formatCellValue(cell);
                    normalizedHeaders.add(normalizeHeader(rawHeader));
                }

                // Đọc dữ liệu từng dòng
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Map<String, String> rowData = new HashMap<>();
                    boolean isEmptyRow = true;

                    for (int i = 0; i < normalizedHeaders.size(); i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        String colName = normalizedHeaders.get(i);
                        String rawValue = cleanQuotes(dataFormatter.formatCellValue(cell));

                        if (!rawValue.isEmpty()) isEmptyRow = false;

                        rowData.put(colName, sanitizeValue(colName, rawValue));
                    }
                    if (!isEmptyRow) records.add(rowData);
                }
            }
            log.info("✅ Parse Excel thành công {} dòng từ {} Sheet.", records.size(), totalSheets);
        }
        return records;
    }

    // ================== CÁC HÀM VŨ KHÍ BỔ TRỢ (Giữ nguyên logic cực mượt của Bro) ==================

    /**
     * Biến mọi thể loại tên cột về 1 chuẩn: "Giá Bán" -> "gia_ban", "Size (EU)" -> "size_eu"
     */
    private String normalizeHeader(String header) {
        if (header == null || header.trim().isEmpty()) return "unknown_column_" + UUID.randomUUID().toString().substring(0,5);

        String temp = cleanQuotes(header).toLowerCase().trim();

        // Cạo sạch dấu Tiếng Việt
        temp = Normalizer.normalize(temp, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        temp = pattern.matcher(temp).replaceAll("");
        temp = temp.replace("đ", "d").replace("đ", "d");

        // Xóa ký tự đặc biệt, thay khoảng trắng bằng dấu gạch dưới
        temp = temp.replaceAll("[^a-z0-9_\\s]", "");
        return temp.replaceAll("\\s+", "_");
    }

    /**
     * Bóc lớp vỏ ngoặc kép thừa (nếu có)
     */
    private String cleanQuotes(String value) {
        if (value != null && value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value != null ? value.trim() : "";
    }

    /**
     * Khử trùng dữ liệu: Cạo sạch chữ trong cột số, biến chuỗi rỗng thành Null
     */
    private String sanitizeValue(String colName, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // Nếu AI nhận diện cột này chứa tiền hoặc số lượng, nó sẽ gọt sạch chữ
        // VD: "150.000 VNĐ" -> "150000"
        if (colName.contains("gia") || colName.contains("price") || colName.contains("cost") ||
                colName.contains("soluong") || colName.contains("quantity") || colName.contains("ton")) {

            String sanitized = value.replaceAll("[^0-9]", ""); // Chỉ giữ lại đúng chữ số nguyên
            return sanitized.isEmpty() ? null : sanitized;
        }

        return value;
    }
}