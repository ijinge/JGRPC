package com.ijinge.rpc.netty.codec;

import com.ijinge.rpc.compress.Compress;
import com.ijinge.rpc.constants.CompressTypeEnum;
import com.ijinge.rpc.constants.IjingeRpcConstants;
import com.ijinge.rpc.constants.MessageTypeEnum;
import com.ijinge.rpc.constants.SerializationTypeEnum;
import com.ijinge.rpc.exception.RpcException;
import com.ijinge.rpc.message.Message;
import com.ijinge.rpc.message.Request;
import com.ijinge.rpc.message.Response;
import com.ijinge.rpc.netty.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;

/**
 *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 *   |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 * 6A 67 6E 62 01 00 00 00 B1 01 01 01 00 00 00 02
 *            | |            | |  |  |            |
 */
@Slf4j
public class IjingeRpcDecoder extends LengthFieldBasedFrameDecoder {
    public IjingeRpcDecoder(){
        this(8 * 1024 * 1024,5, 4,-9,0);
    }
    /**
     *
     * @param maxFrameLength 最大帧长度。它决定可以接收的数据的最大长度。如果超过，数据将被丢弃,根据实际环境定义
     * @param lengthFieldOffset 数据长度字段开始的偏移量, magic code+version=长度为5
     * @param lengthFieldLength 消息长度的大小  full length（消息长度） 长度为4
     * @param lengthAdjustment 补偿值 lengthAdjustment+数据长度取值=长度字段之后剩下包的字节数(x + 16=7 so x = -9)
     * @param initialBytesToStrip 忽略的字节长度，如果要接收所有的header+body 则为0，如果只接收body 则为header的长度 ,我们这为0
     */
    public IjingeRpcDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf dup = in.duplicate();
        if (dup.readableBytes() >= 16) {
            byte[] tmp = new byte[16];
            dup.readBytes(tmp);
            log.info("收到的前16字节: {}", bytesToHex(tmp));
        }
        Object decode = super.decode(ctx, in);
        if (decode instanceof ByteBuf frame){
            if (frame.readableBytes() < IjingeRpcConstants.HEAD_LENGTH){
                throw new RpcException("数据长度不符,格式有误");
            }
            return decodeFrame(frame);
        }
        return decode;
    }

    private Object decodeFrame(ByteBuf in) {
        //顺序读取
        //1. 先读取魔法数，确定是我们自定义的协议
        checkMagicNumber(in);
        //2. 检查版本
        checkVersion(in);
        //3.数据长度
        int fullLength = in.readInt();
        //4.messageType 消息类型
        byte messageType = in.readByte();
        //5.序列化类型
        byte codecType = in.readByte();
        //6.压缩类型
        byte compressType = in.readByte();
        //7. 请求id
        int requestId = in.readInt();
        //8. 读取数据
        int bodyLength = fullLength - IjingeRpcConstants.HEAD_LENGTH;
        if (bodyLength > 0){
            //有数据,读取body的数据
            byte[] bodyData = new byte[bodyLength];
            in.readBytes(bodyData);
            //解压缩 使用gzip
            Compress compress = loadCompress(compressType);
            bodyData = compress.decompress(bodyData);
            // 反序列化
            Serializer serializer = loadSerializer(codecType);
            // 请求类型数据反序列化
            if(messageType  == MessageTypeEnum.REQUEST.getCode()){
                Request request = (Request)serializer.deserialize(bodyData, Request.class);
                return Message.builder()
                        .codec(codecType)
                        .requestId(requestId)
                        .messageType(MessageTypeEnum.REQUEST.getCode())
                        .compress(compressType)
                        .data(request)
                        .build();
            }
            // 响应数据类型反序列化
            if(messageType == MessageTypeEnum.RESPONSE.getCode()){
                Response response = (Response)serializer.deserialize(bodyData, Response.class);
                return Message.builder()
                        .codec(codecType)
                        .messageType(MessageTypeEnum.RESPONSE.getCode())
                        .compress(compressType)
                        .data(response)
                        .build();
            }
        }
        return null;
    }

    private void checkVersion(ByteBuf in) {
        byte b = in.readByte();
        if (b != IjingeRpcConstants.VERSION){
            throw new RpcException("未知的version");
        }
    }

    private void checkMagicNumber(ByteBuf in) {
        byte[] tmp = new byte[IjingeRpcConstants.MAGIC_NUMBER.length];
        in.readBytes(tmp);
        for (int i = 0;i< tmp.length;i++) {
            if (tmp[i] != IjingeRpcConstants.MAGIC_NUMBER[i]){
                throw new RpcException("未知的magic number");
            }
        }
    }

    // SPI ，全称为 Service Provider Interface，是一种服务发现机制。它通过在ClassPath路径下的META-INF/services文件夹查找文件，自动加载文件里所定义的类。
    private Compress loadCompress(byte compressType) {
        String compressName = CompressTypeEnum.getName(compressType);
        ServiceLoader<Compress> load = ServiceLoader.load(Compress.class);
        for (Compress compress : load) {
            if (compress.name(compressType).equals(compressName)) {
                return compress;
            }
        }
        throw new RpcException("无对应的压缩类型");
    }

    private Serializer loadSerializer(byte codecType) {
        String serializerName = SerializationTypeEnum.getName(codecType);
        ServiceLoader<Serializer> load = ServiceLoader.load(Serializer.class);
        for (Serializer serializer : load) {
            if (serializer.name(codecType).equals(serializerName)) {
                return serializer;
            }
        }
        throw new RpcException("无对应的序列化类型");
    }
}
