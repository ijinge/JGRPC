package com.ijinge.rpc.compress;

import com.ijinge.rpc.constants.CompressTypeEnum;
import com.ijinge.rpc.exception.RpcException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipCompress implements Compress{
    @Override
    public String name(byte compressType) {
        return CompressTypeEnum.getName(compressType);
    }

    @Override
    public byte[] compress(byte[] bytes) {
        if (bytes == null){
            throw new NullPointerException("传入的压缩数据为null");
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gzip = new GZIPOutputStream(os);
            gzip.write(bytes);
            gzip.flush();
            gzip.finish();
            return os.toByteArray();
        } catch (IOException e) {
            throw new RpcException("压缩数据出错",e);
        }
    }

    @Override
    public byte[] decompress(byte[] bytes) {
        if (bytes == null){
            throw new NullPointerException("传入的解压缩数据为null");
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
            byte[] buffer = new byte[1024 * 4];
            int n;
            while ((n = gzipInputStream.read(buffer)) > -1){
                os.write(buffer,0,n);
            }
            return os.toByteArray();
        } catch (IOException e) {
            throw new RpcException("解压缩数据出错",e);
        }
    }
}
