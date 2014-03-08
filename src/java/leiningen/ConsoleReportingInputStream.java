package leiningen;

import java.io.InputStream;
import java.io.IOException;

/**
 * Extends java.io.InputStream by reporting to console output
 * the percentage of the stream that has been consumed after each
 * read.  Employs a Decorator pattern so that the capabilities of
 * the original InputStream subclass are preserved.
 */

public class ConsoleReportingInputStream extends InputStream
{

    private final long contentLength;
    private final InputStream stream;
    private long totalBytesRead=0;

    /**
     * @param stream - the InputStream to be wrapped by this object
     * @param contentLength - the size, in bytes, of the content
     * referred to by the stream parameter
     */
    public ConsoleReportingInputStream(InputStream stream, long contentLength)
        throws IOException
    {
        if(stream == null) {
            throw new IOException("InputStream cannot be null");
        }

        if(contentLength <= 0) {
            throw new IOException("InputStream content-length must be greater than zero");
        }
         
        this.stream = stream;
        this.contentLength = contentLength;
    }


    private void updateAndReportProgress(int bytesRead) {
        
        totalBytesRead += bytesRead;
        double progress = (totalBytesRead * 100.0)/contentLength;
        // if progress < 100.0, then end output with carriage return
        // otherwise, end output with newline.  This ensures that 
        // progress will update on the same line until 100% reached
        char lineTerm = '\r';
        if(progress == 100.0) { lineTerm = '\n'; }

        // Show one digit to the right of the decimal
        System.out.printf("%.1f", progress);
        System.out.print("% complete" + lineTerm);        
    }

    
    @Override
    public int read() throws IOException
    {
        byte[] buf = new byte[1];
        int cnt = stream.read(buf);

        if(cnt == -1) { return cnt; }
        else { 
            updateAndReportProgress(cnt);
            return buf[0];
        }
             
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        int cnt = stream.read(b);
        if(cnt >= 0) {
            updateAndReportProgress(cnt);
        }
        return cnt;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        int cnt = stream.read(b, off, len);
        if(cnt >= 0) {
            updateAndReportProgress(cnt);
        }        
        return cnt;
    }
}
