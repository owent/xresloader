package org.xresloader.core.engine;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.src.DataSrcImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.eventusermodel.EventWorkbookBuilder.SheetRecordCollectingListener;
import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.eventusermodel.MissingRecordAwareHSSFListener;
import org.apache.poi.hssf.eventusermodel.dummyrecord.LastCellOfRowDummyRecord;
import org.apache.poi.hssf.eventusermodel.dummyrecord.MissingCellDummyRecord;
import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * @see https://svn.apache.org/repos/asf/poi/trunk/src/examples/src/org/apache/poi/examples/hssf/eventusermodel/XLS2CSVmra.java
 */
@SuppressWarnings({ "java:S106", "java:S4823" })
public class ExcelHSSFStreamSheetHandle implements HSSFListener {
    private int maxRowNumber = 0;
    private int maxColumnNumber = 0;
    private boolean currentActive = false;
    private ExcelEngine.CustomDataRowIndex currentRowIndex = null;
    private ExcelEngine.CustomDataTableIndex currentTableIndex = null;

    private final List<BoundSheetRecord> boundSheetRecords = new ArrayList<>();
    private BoundSheetRecord[] orderedBSRs = null;
    private int sheetIndex = -1;

    private SSTRecord lastSSTRecord = null;
    private FormatTrackingHSSFListener formatListener = null;

    // For handling formulas with string results
    private int nextRow = -1;
    private int nextColumn = -1;
    private boolean outputNextStringRecord = false;

    private ExcelHSSFStreamSheetHandle(ExcelEngine.CustomDataTableIndex table) {
        if (table != null) {
            currentTableIndex = table;
        } else {
            currentTableIndex = new ExcelEngine.CustomDataTableIndex("", "");
        }
    }

    private void setFormatListener(FormatTrackingHSSFListener input) {
        formatListener = input;
    }

    /**
     * Main HSSFListener method, processes events, and outputs the CSV as the file
     * is processed.
     */
    @Override
    public void processRecord(org.apache.poi.hssf.record.Record record) {
        int thisRow = -1;
        int thisColumn = -1;
        String thisStr = null;

        switch (record.getSid()) {
            case BoundSheetRecord.sid:
                boundSheetRecords.add((BoundSheetRecord) record);
                break;
            case BOFRecord.sid:
                BOFRecord br = (BOFRecord) record;
                if (br.getType() == BOFRecord.TYPE_WORKSHEET) {
                    // Output the worksheet name
                    // Works by ordering the BSRs by the location of
                    // their BOFRecords, and then knowing that we
                    // process BOFRecords in byte offset order
                    sheetIndex++;
                    if (orderedBSRs == null) {
                        orderedBSRs = BoundSheetRecord.orderByBofPosition(boundSheetRecords);
                    }

                    currentActive = orderedBSRs[sheetIndex].getSheetname().equals(currentTableIndex.getSheetName());
                }
                break;

            case SSTRecord.sid:
                lastSSTRecord = (SSTRecord) record;
                break;

            case BlankRecord.sid:
                BlankRecord brec = (BlankRecord) record;

                thisRow = brec.getRow();
                thisColumn = brec.getColumn();
                thisStr = "";
                break;
            case BoolErrRecord.sid:
                BoolErrRecord berec = (BoolErrRecord) record;

                thisRow = berec.getRow();
                thisColumn = berec.getColumn();
                thisStr = berec.toString();
                break;

            case FormulaRecord.sid:
                FormulaRecord frec = (FormulaRecord) record;

                if (Double.isNaN(frec.getValue())) {
                    // Formula result is a string
                    // This is stored in the next record
                    outputNextStringRecord = true;
                    nextRow = frec.getRow();
                    nextColumn = frec.getColumn();
                } else {
                    thisRow = frec.getRow();
                    thisColumn = frec.getColumn();
                    thisStr = formatListener.formatNumberDateCell(frec);
                }
                break;
            case StringRecord.sid:
                if (outputNextStringRecord) {
                    // String for formula
                    StringRecord srec = (StringRecord) record;
                    thisStr = srec.getString();
                    thisRow = nextRow;
                    thisColumn = nextColumn;
                    outputNextStringRecord = false;
                }
                break;

            case LabelRecord.sid:
                LabelRecord lrec = (LabelRecord) record;

                thisRow = lrec.getRow();
                thisColumn = lrec.getColumn();
                thisStr = lrec.getValue();
                break;
            case LabelSSTRecord.sid:
                LabelSSTRecord lsrec = (LabelSSTRecord) record;

                thisRow = lsrec.getRow();
                thisColumn = lsrec.getColumn();
                if (lastSSTRecord == null) {
                    thisStr = "No SST Record, can't identify string";
                } else {
                    thisStr = lastSSTRecord.getString(lsrec.getSSTIndex()).toString();
                }
                break;
            case NoteRecord.sid:
                // Ignore comment
                // NoteRecord nrec = (NoteRecord) record;
                //
                // thisRow = nrec.getRow();
                // thisColumn = nrec.getColumn();
                // thisStr = '"' + "(TODO)" + '"';
                break;
            case NumberRecord.sid:
                NumberRecord numrec = (NumberRecord) record;

                thisRow = numrec.getRow();
                thisColumn = numrec.getColumn();

                // Format
                thisStr = formatListener.formatNumberDateCell(numrec);
                break;
            case RKRecord.sid:
                RKRecord rkrec = (RKRecord) record;

                thisRow = rkrec.getRow();
                thisColumn = rkrec.getColumn();
                thisStr = String.valueOf(rkrec.getRKNumber());
                break;
            default:
                break;
        }

        // Not target sheet
        if (!currentActive) {
            return;
        }

        // Ignore MissingCellDummyRecord
        if (record instanceof MissingCellDummyRecord) {
            return;
        }

        if (thisRow >= DataSrcImpl.LOG_PROCESS_BOUND && thisRow % DataSrcImpl.LOG_PROCESS_BOUND == 0
                && currentTableIndex != null) {
            ProgramOptions.getLoger().info("  > File: %s, Table: %s, indexes %d rows", currentTableIndex.getFilePath(),
                    currentTableIndex.getSheetName(), thisRow);
        }

        // Has data, mutable row and cell
        if (thisRow >= 0 && thisColumn >= 0 && thisStr != null && null != currentTableIndex) {
            if (maxRowNumber < thisRow) {
                maxRowNumber = thisRow;
            }

            if (maxColumnNumber < thisColumn) {
                maxColumnNumber = thisColumn;
            }

            if (currentRowIndex == null) {
                currentRowIndex = currentTableIndex.getRow(thisRow);
            }

            if (currentRowIndex == null) {
                currentRowIndex = new ExcelEngine.CustomDataRowIndex(thisRow, currentTableIndex);
                currentRowIndex.getColumns().ensureCapacity(maxColumnNumber + 1);
            }

            while (currentRowIndex.getColumns().size() <= thisColumn) {
                currentRowIndex.getColumns().add(null);
            }

            currentRowIndex.getColumns().set(thisColumn, thisStr);
        }

        // Handle end of row
        if (record instanceof LastCellOfRowDummyRecord) {
            if (null != currentTableIndex) {
                currentTableIndex.addRow(currentRowIndex);
            }
            currentRowIndex = null;
        }
    }

    static public ExcelEngine.CustomDataTableIndex buildCustomTableIndex(String file_path, String sheet_name) {
        if (file_path == null || sheet_name == null) {
            return null;
        }

        try (POIFSFileSystem xls_fs = new POIFSFileSystem(new FileInputStream(file_path))) {
            ExcelEngine.CustomDataTableIndex tableIndex = new ExcelEngine.CustomDataTableIndex(file_path, sheet_name);

            ExcelHSSFStreamSheetHandle handle = new ExcelHSSFStreamSheetHandle(tableIndex);

            MissingRecordAwareHSSFListener listener = new MissingRecordAwareHSSFListener(handle);
            FormatTrackingHSSFListener formatListener = new FormatTrackingHSSFListener(listener);
            handle.setFormatListener(formatListener);

            HSSFEventFactory factory = new HSSFEventFactory();
            HSSFRequest request = new HSSFRequest();

            request.addListenerForAllRecords(formatListener);
            factory.processWorkbookEvents(request, xls_fs);

            if (handle.maxRowNumber > DataSrcImpl.LOG_PROCESS_BOUND
                    && handle.maxRowNumber % DataSrcImpl.LOG_PROCESS_BOUND != 0 && handle.currentTableIndex != null) {
                ProgramOptions.getLoger().info("  > File: %s, Table: %s, indexes %d rows",
                        handle.currentTableIndex.getFilePath(), handle.currentTableIndex.getSheetName(),
                        handle.maxRowNumber);
            }

            return tableIndex;
        } catch (java.io.IOException e) {
            ProgramOptions.getLoger().error("Open source file \"%s\" and parse sheet \"%s\" failed, %s.", file_path,
                    sheet_name, e.getMessage());
        }

        return null;
    }
}
