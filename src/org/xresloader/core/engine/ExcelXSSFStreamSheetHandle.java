package org.xresloader.core.engine;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.src.DataSrcImpl;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.model.StylesTable;

import org.apache.poi.util.XMLHelper;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.InputStream;

/**
 * @see https://svn.apache.org/repos/asf/poi/trunk/src/examples/src/org/apache/poi/examples/xssf/eventusermodel/XLSX2CSV.java
 */
public class ExcelXSSFStreamSheetHandle implements XSSFSheetXMLHandler.SheetContentsHandler {

    private int maxRowNumber = 0;
    private int maxColumnNumber = 0;
    private int currentRow = -1;
    private int currentCol = -1;
    private ExcelEngine.CustomDataRowIndex currentRowIndex = null;
    private ExcelEngine.CustomDataTableIndex currentTableIndex = null;

    private ExcelXSSFStreamSheetHandle(ExcelEngine.CustomDataTableIndex table) {
        if (table != null) {
            currentTableIndex = table;
        } else {
            currentTableIndex = new ExcelEngine.CustomDataTableIndex("", "");
        }
    }

    @Override
    public void startRow(int rowNum) {
        if (this.maxRowNumber < rowNum) {
            this.maxRowNumber = rowNum;
        }
        this.currentRow = rowNum;
        this.currentCol = -1;
        this.currentRowIndex = new ExcelEngine.CustomDataRowIndex(rowNum, currentTableIndex);
        this.currentRowIndex.getColumns().ensureCapacity(this.maxColumnNumber + 1);

        if (rowNum >= DataSrcImpl.LOG_PROCESS_BOUND && rowNum % DataSrcImpl.LOG_PROCESS_BOUND == 0
                && currentTableIndex != null) {
            ProgramOptions.getLoger().info("  > File: %s, Table: %s, indexes %d rows", currentTableIndex.getFilePath(),
                    currentTableIndex.getSheetName(), rowNum);
        }
    }

    @Override
    public void endRow(int rowNum) {
        if (currentTableIndex != null) {
            currentTableIndex.addRow(currentRowIndex);
        }
    }

    @Override
    public void endSheet() {
        if (this.maxRowNumber > DataSrcImpl.LOG_PROCESS_BOUND && this.maxRowNumber % DataSrcImpl.LOG_PROCESS_BOUND != 0
                && this.currentTableIndex != null) {
            ProgramOptions.getLoger().info("  > File: %s, Table: %s, indexes %d rows",
                    this.currentTableIndex.getFilePath(), this.currentTableIndex.getSheetName(), this.maxRowNumber);
        }
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment) {
        // gracefully handle missing CellRef here in a similar way as XSSFCell does
        if (cellReference == null) {
            cellReference = new CellAddress(currentRow, currentCol).formatAsString();
        }

        // Did we miss any cells?
        int thisCol = (new CellReference(cellReference)).getCol();
        while (currentRowIndex.getColumns().size() <= thisCol) {
            currentRowIndex.getColumns().add(null);
        }
        currentCol = thisCol;
        if (maxColumnNumber < currentCol) {
            maxColumnNumber = currentCol;
        }

        currentRowIndex.getColumns().set(thisCol, formattedValue);
    }

    static public ExcelEngine.CustomDataTableIndex buildCustomTableIndex(File file, String sheet_name) {
        if (file == null || sheet_name == null) {
            return null;
        }

        try (OPCPackage xlsx_package = OPCPackage.open(file, PackageAccess.READ)) {
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsx_package);
            XSSFReader xssf_reader = new XSSFReader(xlsx_package);
            StylesTable styles = xssf_reader.getStylesTable();
            XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssf_reader.getSheetsData();
            while (iter.hasNext()) {
                try (InputStream stream = iter.next()) {
                    if (!sheet_name.equals(iter.getSheetName())) {
                        continue;
                    }

                    ExcelEngine.CustomDataTableIndex tableIndex = new ExcelEngine.CustomDataTableIndex(
                            file.getCanonicalPath(), sheet_name);

                    DataFormatter formatter = new DataFormatter();
                    InputSource sheet_source = new InputSource(stream);
                    try {
                        XMLReader sheet_parser = XMLHelper.newXMLReader();
                        ContentHandler handler = new XSSFSheetXMLHandler(styles, null, strings,
                                new ExcelXSSFStreamSheetHandle(tableIndex), formatter, false);
                        sheet_parser.setContentHandler(handler);
                        sheet_parser.parse(sheet_source);

                        return tableIndex;
                    } catch (org.xml.sax.SAXException e) {
                        ProgramOptions.getLoger().error(
                                "Open source file \"%s\" and parse sheet \"%s\" failed, SAX engine appears to be broken - %s.",
                                file.getPath(), sheet_name, e.getMessage());
                    } catch (ParserConfigurationException e) {
                        ProgramOptions.getLoger().error(
                                "Open source file \"%s\" and parse sheet \"%s\" failed, SAX parser appears to be broken - %s.",
                                file.getPath(), sheet_name, e.getMessage());
                    } catch (java.io.IOException e) {
                        ProgramOptions.getLoger().error("open source file \"%s\" and parse sheet \"%s\" failed, %s.",
                                file.getPath(), sheet_name, e.getMessage());
                    }
                }
            }
        } catch (org.apache.poi.openxml4j.exceptions.OpenXML4JException e) {
            ProgramOptions.getLoger().error(
                    "Open source file \"%s\" and parse sheet \"%s\" failed, OpenXML4J engine appears to be broken - %s.",
                    file.getPath(), sheet_name, e.getMessage());
        } catch (org.xml.sax.SAXException e) {
            ProgramOptions.getLoger().error(
                    "Open source file \"%s\" and parse sheet \"%s\" failed, SAX engine appears to be broken - %s.",
                    file.getPath(), sheet_name, e.getMessage());
        } catch (java.io.IOException e) {
            ProgramOptions.getLoger().error("Open source file \"%s\" and parse sheet \"%s\" failed, %s.",
                    file.getPath(), sheet_name, e.getMessage());
        } catch (Exception e) {
            ProgramOptions.getLoger().error("Open source file \"%s\" and parse sheet \"%s\" failed, %s.",
                    file.getPath(), sheet_name, e.getMessage());
        }

        return null;
    }
}
