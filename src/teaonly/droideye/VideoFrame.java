package teaonly.droideye;

import java.io.*;

class VideoFrame extends OutputStream {
    private byte[] buffer;
    private int bufferLength;
    private int currentLength;
    
    private int flag;

    public VideoFrame (int maxSize) {
        super();
        buffer = new byte[maxSize];
        bufferLength = maxSize;
        currentLength = 0;
        flag = 0;
    }

    public VideoFrame() {
        super();
        final int maxSize = 1024*1024;
        buffer = new byte[maxSize];
        bufferLength = maxSize;
        currentLength = 0;
        flag = 0;
    }

    public boolean acquire() {
        synchronized(this) {
            if ( flag == 0) {
                flag = 1;
                return true;
            } else {
                return false;
            }
        }
    }

    public void release() {
        synchronized(this) {
            if ( flag == 1) {
                flag = 0;
            }
        }
    }

    public ByteArrayInputStream getByteInputStream() {
        ByteArrayInputStream bin = new ByteArrayInputStream(buffer, 0, currentLength);
        return bin;
    }

    public void reset() {
        currentLength = 0;
    }

    @Override 
    public void write(byte[] srcBuffer, int offset, int count) throws IOException {
        IOException ioEx = null;
        try {
            System.arraycopy(srcBuffer, offset, buffer, currentLength, count);
        } catch (IndexOutOfBoundsException ex) {
            ioEx = new IOException("Buffer overflow");
        } catch (ArrayStoreException ex) {
            ioEx = new IOException("Parameter error");
        } catch (NullPointerException ex) {
            ioEx = new IOException("Parameter error");
        }
        if ( ioEx != null)
            throw ioEx;
        currentLength += count;
    }

    @Override
    public void write(int oneByte) throws IOException{
        if ( currentLength >= bufferLength) {
            IOException ex = new IOException("Buffer overflow");
            throw ex;
        } 

        buffer[currentLength] = (byte)(oneByte & 0xFF);
        currentLength++;
    }
    
} 

