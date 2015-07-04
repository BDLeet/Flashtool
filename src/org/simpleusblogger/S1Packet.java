package org.simpleusblogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.ta.parsers.TAUnit;
import org.util.BytesUtil;
import org.util.HexDump;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.Bin;

public class S1Packet {
	@Bin byte[] command;
	@Bin byte[] flag;
	@Bin byte[] length;
	@Bin byte headercksum;
	byte[] data = null;
	byte[] crc = null;
	int startrecord=0;
	int datalength=0;
	String direction="";
	String sinname="";

	public boolean isHeaderOK() {
		byte computed = calcSum(BytesUtil.concatAll(command, flag, length));
		return (computed == headercksum) && (getCommandName().length() >0);
	}
	
	public int getCommand() {
		return BytesUtil.getInt(command);
	}

	public int getFlag() {
		return BytesUtil.getInt(flag);
	}

	public String getCommandName() {
		if (getCommand() == 0x01) return "Get Loader Infos";
		if (getCommand() == 0x09) return "Open TA";
		if (getCommand() == 0x0A) return "Close TA";
		if (getCommand() == 0x0C) return "Read TA";
		if (getCommand() == 0x0D) return "Write TA";
		if (getCommand() == 0x05) return "Send sin header";
		if (getCommand() == 0x06) return "Send sin data";
		if (getCommand() == 0x19) return "Set loader config";
		if (getCommand() == 0x04) return "End flashing";
		if (getCommand() == 0x07) return "Get Error";
		//System.out.println(HexDump.toHex(BytesUtil.concatAll(command, flag, length)));
		return "";
	}
	
	public String getDirection() {
		return direction;
	}
	
	public int getLength() {
		return BytesUtil.getInt(length);
	}
	
	private byte calcSum(byte paramArray[])
    {
        byte byte0 = 0;
        if(paramArray.length < 12)
            return 0;
        for(int i = 0; i < 12; i++)
            byte0 ^= paramArray[i];

        byte0 += 7;
        return byte0;
    }
	
	public void finalise() throws IOException {
		if (data!=null) {
			JBBPBitInputStream dataStream = new JBBPBitInputStream(new ByteArrayInputStream(data));
			if (data.length >4)
				data = dataStream.readByteArray(data.length-4);
			else data = null;
			crc = dataStream.readByteArray(4);
		}
		if (data==null) datalength=0;
		else datalength = data.length;		
	}
	
	public void addData(byte[] pdata) {
		if (data==null)
			data = pdata;
		else
			data = BytesUtil.concatAll(data, pdata);
	}
	
	public void setRecord(int recnum) {
		startrecord=recnum;
	}
	
	public void setDirection(int dir) {
		if (dir==0) direction = "WRITE";
		else direction = "READ REPLY";
	}

	public TAUnit getTA() {
		try {
		JBBPBitInputStream taStream = new JBBPBitInputStream(new ByteArrayInputStream(data));
		int unit=taStream.readInt(JBBPByteOrder.BIG_ENDIAN);
		int talength = taStream.readInt(JBBPByteOrder.BIG_ENDIAN);
		if (talength>0) {
			TAUnit u = new TAUnit(unit, taStream.readByteArray(talength));
			return u;
		}
	    taStream.close();
		} catch (Exception e) {
		}
		return null;
	}

	public void setFileName(String name) {
		sinname = name;
	}
	
	public String getInfo() {
		TAUnit ta = null;
		if (this.getCommand()==0x0D) ;
			ta=getTA();
		if (ta!=null) return ta.toString();
		if (getCommand()==5)
			return sinname;
		if (getCommand()==0x09)
			return "Partition : "+BytesUtil.getInt(data);
		if (getCommand()==0x0C) {
			if (direction.equals("READ REPLY"))
				return "Value : "+HexDump.toHex(data).replace("[", "").replace("]", "").replace(", ", "");
			else
				return "Unit : "+HexDump.toHex(data).replace("[", "").replace("]", "").replace(", ", "");
		}
		return "";
	}
	
	public String toString() {	
		return direction + " : " + getCommandName()+" "+getInfo();
	}
}