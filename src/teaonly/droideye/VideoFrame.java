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

    public reset() {
        currentLength = 0;
        flag = 0;
    }

    public void releaseByteInputStream() {
        synchronized {
            flag--;
        }
    }

    public ByteArrayInputStream getByteInputStream() {
        ByteArrayInputStream bin = new ByteArrayInputStream(buffer, 0, currentLength);
        synchronized {
            flag++;
        }
        return bin;
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

