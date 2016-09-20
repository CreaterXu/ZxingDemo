package com.creater.zxingdemo.utils;

/**
 * 事件类
 *
 * */
public class SubEvent {
    private int mMsg;
    private int mContent;
    private Object mObject;

    public SubEvent(int msgs){
        this.mMsg=msgs;
    }

    public SubEvent(int msgs,int content) {
        // TODO Auto-generated constructor stub
        this.mMsg=msgs;
        this.mContent=content;
    }
    public int getMsg(){
        return mMsg;
    }
    public void setMsg(int msg){
        mMsg=msg;
    }

    public int getContent() {
        return mContent;
    }

    public void setContent(int mContent) {
        this.mContent = mContent;
    }

    public Object getmObject() {
        return mObject;
    }

    public void setmObject(Object mObject) {
        this.mObject = mObject;
    }
}
