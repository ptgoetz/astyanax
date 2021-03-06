package com.netflix.astyanax.serializers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.cassandra.db.marshal.UTF8Type;

public class GzipStringSerializer extends AbstractSerializer<String> {

    private static final String UTF_8 = "UTF-8";
    private static final StringSerializer instance = new StringSerializer();
    private static final Charset charset = Charset.forName(UTF_8);

    public static StringSerializer get() {
        return instance;
    }

    @Override
    public ByteBuffer toByteBuffer(String obj) {
        if (obj == null) {
            return null;
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(obj.getBytes());
            gzip.close();
            return ByteBuffer.wrap(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error compressing column data", e);
        }        
    }

    @Override
    public String fromByteBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        
        GZIPInputStream gzipInputStream = null;
        ByteArrayOutputStream baos = null;
        try {
            gzipInputStream = new GZIPInputStream(
                    new ByteArrayInputStream(byteBuffer.array(), 0,
                            byteBuffer.limit()));
            
            baos = new ByteArrayOutputStream();
            for (int value = 0; value != -1;) {
                value = gzipInputStream.read();
                if (value != -1) {
                    baos.write(value);
                }
            }
            gzipInputStream.close();
            baos.close();
            return new String(baos.toByteArray(), charset);
        } catch (IOException e) {
            throw new RuntimeException("Error decompressing column data", e);
        } finally {
            if (gzipInputStream != null) {
                try {
                    gzipInputStream.close();
                } catch (IOException e) {
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public ComparatorType getComparatorType() {
        return ComparatorType.BYTESTYPE;
    }

    @Override
    public ByteBuffer fromString(String str) {
        return UTF8Type.instance.fromString(str);
    }

    @Override
    public String getString(ByteBuffer byteBuffer) {
        return UTF8Type.instance.getString(byteBuffer);
    }
}
