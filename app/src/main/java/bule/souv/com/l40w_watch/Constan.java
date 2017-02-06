package bule.souv.com.l40w_watch;

/**
 * 描述：
 * 作者：dc on 2016/12/29 15:24
 * 邮箱：597210600@qq.com
 */
public class Constan {

    public static final int MINDATA = 6; //最小数据长度

    public static final byte CLIENTQUERYBYTE = 0X70;//上位机查询

    public static final byte WATCHBREAKBYTE = (byte) 0X80; //下位机返回

    public static final byte CLIENTSETBYTE = (byte) 0X71; //下位机设置

    public static final byte WATCHRESPONSEBYTE = (byte) 0X81; //下位机响应

    public static final byte STARTBYTE = 0X6F;  //开始符

    public static final byte ENDBYTE = (byte) 0x8F;  //结束符

    public static final byte DATARESPONSE = 0X01; //数据的基本响应

    public static final byte WATCHID = 0X02; //设备WatchID

    public static final byte WATCHTIME = 0X04; //设置手环时间

    public static final byte WATCHSPORTDATACOUNT = 0X52; //获取运动总数

    public static final byte DELECTWATCHSPORTRECORD = 0X53; //删除手环运动记录

    public static final byte WATCHSPORT = 0X54; //获取手环运动信息

    public static final byte WATCHDISPLAYSPORTDATA = 0X57; //获取手环显示运动信息
}
