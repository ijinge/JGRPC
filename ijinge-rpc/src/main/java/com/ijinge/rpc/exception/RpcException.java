package com.ijinge.rpc.exception;

public class RpcException extends RuntimeException{
    public RpcException(){
        super();
    }

    public RpcException(String msg){
        super(msg);
    }

    public RpcException(String msg,Exception e){
        super(msg,e);
    }
}
