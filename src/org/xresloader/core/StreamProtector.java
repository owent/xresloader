package org.xresloader.core;

import java.io.FilterOutputStream;
import java.io.PrintStream;

/**
 * Utility class that must be loaded BEFORE any Log4j2 classes.
 * It wraps System.out and System.err with streams that ignore close() calls,
 * preventing Log4j2's ConsoleAppender lifecycle from breaking console output.
 */
public class StreamProtector {
  private static boolean initialized = false;

  /**
   * Initialize the stream protection. Safe to call multiple times.
   * This method must be called before any Log4j2 LogManager is accessed.
   */
  public static synchronized void init() {
    if (initialized) {
      return;
    }
    initialized = true;

    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;

    try {
      System.setOut(createNonClosingPrintStream(originalOut));
    } catch (Exception ignored) {
    }

    try {
      System.setErr(createNonClosingPrintStream(originalErr));
    } catch (Exception ignored) {
    }
  }

  private static PrintStream createNonClosingPrintStream(final PrintStream delegate) {
    if (delegate == null) {
      return null;
    }
    return new PrintStream(new FilterOutputStream(delegate) {
      @Override
      public void close() {
        // Swallow close to prevent Log4j2 from breaking the stream
      }

      @Override
      public void write(byte[] b, int off, int len) throws java.io.IOException {
        delegate.write(b, off, len);
      }

      @Override
      public void write(int b) throws java.io.IOException {
        delegate.write(b);
      }

      @Override
      public void flush() throws java.io.IOException {
        delegate.flush();
      }
    }, true);
  }
}
