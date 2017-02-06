package bule.souv.com.l40w_watch.bean;

import java.io.Serializable;

/**
 * 描述：手环数据类型实体类
 * 作者：dc on 2016/12/29 15:43
 * 邮箱：597210600@qq.com
 */
public class DataTypeBean implements Serializable{

    private int dataType = -1 ;  //数据类型
    private byte[] dataBuffer ; //数据内容

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public byte[] getDataBuffer() {
        return dataBuffer;
    }

    public void setDataBuffer(byte[] dataBuffer) {
        this.dataBuffer = dataBuffer;
    }
}
