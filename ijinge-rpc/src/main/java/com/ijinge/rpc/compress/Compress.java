package com.ijinge.rpc.compress;

public interface Compress {
    /**
     * 压缩方法名称
     * @return
     */
    String name(byte compressType);
    /**
     * 压缩
     * @param bytes
     * @return
     */
    byte[] compress(byte[] bytes);

    /**
     * 解压缩
     * @param bytes
     * @return
     */
    byte[] decompress(byte[] bytes);
}
