/**
 * 
 *  http://www.digitalekabeltelevisie.nl/dvb_inspector
 * 
 *  This code is Copyright 2009-2012 by Eric Berendsen (e_berendsen@digitalekabeltelevisie.nl)
 * 
 *  This file is part of DVB Inspector.
 * 
 *  DVB Inspector is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  DVB Inspector is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with DVB Inspector.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  The author requests that he be notified of any application, applet, or
 *  other binary that makes use of this code, but that's more out of curiosity
 *  than anything and is not required.
 * 
 */

package nl.digitalekabeltelevisie.util;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.tree.DefaultMutableTreeNode;

import nl.digitalekabeltelevisie.controller.KVP;
import nl.digitalekabeltelevisie.controller.TreeNode;
import nl.digitalekabeltelevisie.gui.DVBtree;
import au.com.bytecode.opencsv.CSVReader;

/**
 * @author Eric Berendsen
 *
 */
public final class Utils {

	/**
	 * 
	 */
	private Utils() {
		// private to avoid instantion
	}


	private static final Logger	logger	= Logger.getLogger(Utils.class.getName());
	private static final ClassLoader classL = Utils.class.getClassLoader();


	public static final int MASK_1BIT=0x01;
	public static final int MASK_2BITS=0x03;
	public static final int MASK_3BITS=0x07;
	public static final int MASK_4BITS=0x0F;
	public static final int MASK_5BITS=0x1F;
	public static final int MASK_6BITS=0x3F;
	public static final int MASK_7BITS=0x7F;
	public static final int MASK_8BITS=0xFF;
	public static final int MASK_9BITS=0x01FF;
	public static final int MASK_10BITS=0x03FF;
	public static final int MASK_12BITS=0x0FFF;
	public static final int MASK_13BITS=0x1FFF;
	public static final int MASK_14BITS=0x3FFF;
	public static final int MASK_15BITS=0x7FFF;
	public static final int MASK_16BITS=0xFFFF;
	public static final int MASK_20BITS=0xFFFFF;
	public static final int MASK_22BITS=0x3FFFFF;
	public static final int MASK_24BITS=0xFFFFFF;
	public static final int MASK_31BITS=0x7FFFFFFF;

	public static final long MASK_32BITS=0xFFFFFFFFl;

	private static Map<Integer, String>oui = new HashMap<Integer, String>();
	private static RangeHashMap<Integer,String> bat = new RangeHashMap<Integer,String>();
	private static RangeHashMap<Integer,String> dataBroadcast = new RangeHashMap<Integer,String>();
	private static RangeHashMap<Integer,String> ca_system_id = new RangeHashMap<Integer,String>();
	private static RangeHashMap<Integer,String> original_network_id = new RangeHashMap<Integer,String>();
	private static RangeHashMap<Integer,String> platform_id = new RangeHashMap<Integer,String>();
	private static RangeHashMap<Integer,String> cni = new RangeHashMap<Integer,String>();
	private static RangeHashMap<Integer,String> app_type_id = new RangeHashMap<Integer,String>();
	private static RangeHashMap<Long,String> mhp_organisation_id = new RangeHashMap<Long,String>();

	private static RangeHashMap<Long,String> private_data_spec_id = new RangeHashMap<Long,String>();

	private static 	DecimalFormat f2 = new DecimalFormat("00");
	private static 	DecimalFormat f4 = new DecimalFormat("0000");
	private static 	DecimalFormat f6 = new DecimalFormat("000000");


	private static char[] hexChars = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'A', 'B', 'C', 'D', 'E', 'F' };


	static{

		BufferedReader in;
		try {
			//in = new BufferedReader(new FileReader("res/oui_lines_final.txt"));//open a bufferedReader to file hellowrold.txt
			final InputStream fileInputStream = classL.getResourceAsStream("res/oui_lines_final.txt");
			in = new BufferedReader(new InputStreamReader(fileInputStream));


			String line;
			while ((line = in.readLine()) != null) {
				if((line.length()>6) && (line.charAt(0)!='#')){
					final int id=Integer.parseInt(line.substring(0,6),16);

					final int start = line.indexOf('"');
					final int last = line.lastIndexOf('"');
					if((last>start)&&(start>6)){
						final String name=line.substring(start+1,last);
						if(oui.get(id)!=null){
							logger.warning("duplicate entrie, key="+Integer.toHexString(id)+", old value="+oui.get(id)+", new line="+name);
						}
						oui.put(id, name);
					}
				}
			}
			in.close();//safely close the BufferedReader after use
		}catch(final IOException e){
			logger.severe("There was a problem reading oui table:" + e);
		}

		readCSVIdString("res/bouquet_id.csv",bat);
		readCSVIdString("res/data_broadcast_id.csv",dataBroadcast);
		readCSVIdString("res/ca_system_id.csv",ca_system_id);
		readCSVIdString("res/original_network_id.csv",original_network_id);
		readCSVIdString("res/platform_id.csv",platform_id);
		readCSVIdString("res/ni.csv",cni);
		readCSVIdString("res/app_type_id.csv",app_type_id);
		readCSVIdLongString("res/mhp_organisation_id.csv",mhp_organisation_id);
		readCSVIdLongString("res/private_data_spec_id.csv", private_data_spec_id);

	}


	private static void readCSVIdString(final String fileName, final RangeHashMap<Integer,String> m) {
		try {
			//CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file),Charset.forName("UTF-16")));
			final InputStream fileInputStream = classL.getResourceAsStream(fileName);
			final CSVReader reader = new CSVReader(new InputStreamReader(fileInputStream,Charset.forName("UTF-16")));

			String [] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				// nextLine[] is an array of values from the line
				final int lower = Integer.decode(nextLine[0]);
				final int upper = Integer.decode(nextLine[1]);

				m.put(lower, upper, nextLine[2]);

			}
			reader.close();

		}catch(final IOException e){
			logger.severe("There was a problem reading file: \""+fileName +"\", exception:"+ e);
		}
	}



	private static void readCSVIdLongString(final String fileName, final RangeHashMap<Long,String> m) {
		try {
			//CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file),Charset.forName("UTF-16")));
			final InputStream fileInputStream = classL.getResourceAsStream(fileName);
			final CSVReader reader = new CSVReader(new InputStreamReader(fileInputStream,Charset.forName("UTF-16")));
			String [] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				// nextLine[] is an array of values from the line
				final long lower = Long.decode(nextLine[0]);
				final long upper = Long.decode(nextLine[1]);

				m.put(lower, upper, nextLine[2]);

			}
			reader.close();

		}catch(final IOException e){
			logger.severe("There was a problem reading file: \""+fileName +"\", exception:"+ e);
		}
	}

	public static Image readIconImage(final String fileName) {
		Image image = null;
		try {
			final InputStream fileInputStream = classL.getResourceAsStream(fileName);
			image = ImageIO.read(fileInputStream);
		} catch (final Exception e) {
			logger.log(Level.WARNING, "Error reading icon image: exception:{0}", e);
		}
		return image;
	}

	public static String getOUIString(final int i) {
		String r = oui.get(i);
		if (r==null){
			r="unknown";
		}
		return r;
	}

	public static String getBouquetIDString(final int i) {
		String r = bat.find(i);
		if (r==null){
			r="unknown";
		}
		return r;
	}


	public static String getDataBroadCastIDString(final int i) {
		String r = dataBroadcast.find(i);
		if (r==null){
			r="unknown";
		}
		return r;
	}

	public static String getCASystemIDString(final int i) {
		String r = ca_system_id.find(i);
		if (r==null){
			r="unknown";
		}
		return r;
	}

	public static String getOriginalNetworkIDString(final int i) {
		String r = original_network_id.find(i);
		if (r==null){
			r="unknown";
		}
		return r;
	}

	public static String getPlatformIDString(final int i) {
		String r = platform_id.find(i);
		if (r==null){
			r="unknown";
		}
		return r;
	}

	public static String getPrivateDataSpecString(final long l) {
		String r = private_data_spec_id.find(l);
		if (r==null){
			r="unknown";
		}
		return r;
	}

	public static String getNIString(final int l) {
		String r = cni.find(l);
		if (r==null){
			r="unknown";
		}
		return r;
	}

	public static String getAppTypeIDString(final int i) {
		String r = app_type_id.find(i);
		if (r==null){
			r="unknown";
		}
		return r;
	}

	public static String getMHPOrganistionIdString(final long i) {
		String r = mhp_organisation_id.find(i);
		if (r==null){
			r="unknown";
		}
		return r;
	}

	public static String getActionTypeString(final int actionType) {

		switch (actionType) {
		case 0x00: return "reserved";
		case 0x01: return "location of IP/MAC streams in DVB networks";
		default: return "reserved for future use";
		}
	}

	public static String getUNTActionTypeString(final int actionType) {

		switch (actionType) {
		case 0x00: return "reserved";
		case 0x01: return "System Software Update";
		default: return "reserved for future use";
		}
	}
	public static String getUNTProcessingOrderString(final int p) {

		if((p>=0x01)&&(p<=0xfe)){
			return "subsequent actions (ascending)";
		}

		switch (p) {
		case 0x00: return "first action";

		default: return "reserved for future use";
		}
	}
	/**
	 * Converts a byte array to hex string
	 */
	public static String toHexString(final byte[] block) {
		final StringBuilder buf = new StringBuilder();
		if(block==null){
			return buf.toString();
		}
		final int len = block.length;
		int high = 0;
		int low = 0;
		for (int i = 0; i < len; i++) {
			high = ((block[i] & 0xf0) >> 4);
			low = (block[i] & 0x0f);
			buf.append(hexChars[high]);
			buf.append(hexChars[low]);
		}
		return buf.toString();
	}

	public static String toHexString(final long l, final int pos){
		String r ="0000000000000000"+ Long.toHexString(l);
		r="0x"+ r.substring(r.length()-pos);
		return r;
	}

	public static String toHexStringUnformatted(final long l, final int pos){
		String r ="0000000000000000"+ Long.toHexString(l);
		r = r.substring(r.length()-pos);
		return r;
	}

	public static String toHexString(final byte[] block, final int l) {
		final StringBuilder buf = new StringBuilder();
		if(block==null){
			return buf.toString();
		}
		int high = 0;
		int low = 0;
		for (int i = 0; i < l; i++) {
			high = ((block[i] & 0xf0) >> 4);
			low = (block[i] & 0x0f);
			buf.append(hexChars[high]);
			buf.append(hexChars[low]);
		}
		return buf.toString();
	}

	public static String toHexString(final byte[] block,final int offset, final int l) {
		final StringBuilder buf = new StringBuilder();
		if(block==null){
			return buf.toString();
		}
		int high = 0;
		int low = 0;
		final int end = Math.min(block.length, offset+l);
		for (int i = offset; i < end; i++) {
			high = ((block[i] & 0xf0) >> 4);
			low = (block[i] & 0x0f);
			buf.append(hexChars[high]);
			buf.append(hexChars[low]);
		}
		return buf.toString();
	}

	public static int getUnsignedByte(final byte b){
		if(b>=0){
			return b;
		}
		return b+256;
	}

	public static byte getInt2UnsignedByte(final int b){
		if(b<=127){
			return (byte)b;
		}
		return (byte) (b-256);
	}


	public static int getInt(final byte[] bytes, final int offset, final int len, final int mask){
		int r=0;
		for (int i = 0; i < len; i++) {
			r = (r<<8) | getUnsignedByte( bytes[offset+i]);
		}
		return (r&mask);
	}

	/**
	 * @param bytes
	 * @param offset in bytes array where to start
	 * @param len length in bytes (ec 2 bytes if you need 12 bits)
	 * @param mask used to remove unwanted bits
	 * @return
	 */
	public static long getLong(final byte[] bytes, final int offset, final int len, final long mask){
		long r=0;
		for (int i = 0; i < len; i++) {
			r = (r<<8) | getUnsignedByte( bytes[offset+i]);
		}
		return (r&mask);
	}

	public static String getBCD(final byte[] b, final int nibble_no, final int len) {
		final StringBuilder buf =  new StringBuilder();
		for (int i = 0; i < len; i++) {
			final int byteNo=(nibble_no+i)/2;
			final boolean shift=((nibble_no+i)%2)==0;
			int t;
			if(shift){
				t= (getUnsignedByte(b[byteNo]) & 0xF0)>>4;
			}else{
				t= (getUnsignedByte(b[byteNo]) & 0x0F);
			}
			if(t>9){
				return "Error parsing BCD";
			}
			buf.append(Integer.toString(t));
		}
		return buf.toString();
	}

	public static byte[] getBytes(final byte[] b, final int offset, final int len) {
		if(len==0){
			return new byte[0];
		}
		return Utils.copyOfRange(b, offset, offset+len);
	}




	/**
	 * 
	 * Parse an array of bytes into a java String, according to ETSI EN 300 468 V1.11.1 Annex A (normative): Coding of text characters
	 * 
	 * @param b array of source bytes
	 * @param off offset where relevant data starts in array b
	 * @param len number of bytes to be parsed
	 * @return a java String, according to ETSI EN 300 468 V1.11.1 Annex A,
	 */
	public static String getString(final byte b[], final int off, final int len) {
		int length = len;
		int offset = off;
		if(length<=0){
			return "";
		}
		Charset charset = null;
		if((b[offset]<0x20)&&(b[offset]>=0)){ //Selection of character table
			final int selectorByte=b[offset];
			try {
				if((selectorByte>0)&&(selectorByte<=0x0b)){
					charset = Charset.forName("ISO-8859-"+(selectorByte+4));
					offset++;
					length--;
				}else if((selectorByte==0x10)){
					if(b[offset+1]==0x0){
						charset = Charset.forName("ISO-8859-"+b[offset+2]);
						offset+=3;
						length-=3;
					}
				}
			} catch (final IllegalCharsetNameException e) {
				logger.info("IllegalCharsetNameException in getString:"+e);
				charset = Charset.forName("ISO-8859-1");
			} catch (final UnsupportedCharsetException e) {
				charset = Charset.forName("ISO-8859-1");
				logger.info("UnsupportedCharsetException in getString:"+e+", String="+new String(b, offset, length, charset));
			} catch (final IllegalArgumentException e) {
				logger.info("IllegalArgumentException in getString:"+e);
				charset = Charset.forName("ISO-8859-1");
			}
			if(charset==null){
				charset = Charset.forName("ISO-8859-1");
			}
		}
		
		// filter Single byte control codes

		byte[] filteredBytes = new byte[length];  // length is enough, might need less
		int filteredLength=0;
		
		for (int i = offset; i < (offset+length); i++) {
			if(b[i]>-97){  // bytes are signed, what we really mean is if((b[i]<0x80)||(b[i]>0x9f)){
				filteredBytes[filteredLength++]=b[i];
			}
			
		}
		
		if(charset==null){
			return Iso6937ToUnicode.convert(filteredBytes, 0, filteredLength); //default for DVB
		}else{
			return new String(filteredBytes, 0, filteredLength,charset);
		}
	}

	public static String getISO8859_1String(final byte b[], final int offset, final int length) {
		if(length<=0){
			return "";
		}
		return new String(b, offset, length, Charset.forName("ISO-8859-1"));
	}


	public static String toCodePointString(final String in){
		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < in.length(); i++) {
			b.append( Integer.toHexString(in.codePointAt(i))).append(" ");
		}
		return b.toString();
	}


	public static String getUTCFormattedString(final byte[] UTC_time) {
		final long mjd = getLong(UTC_time, 0, 2, 0xFFFF);
		final String hours = getBCD(UTC_time, 4, 2);
		final String minutes = getBCD(UTC_time, 6, 2);
		final String secs= getBCD(UTC_time, 8, 2);

		// algo: ETSI EN 300 468 - ANNEX C

		long y =  (long) ((mjd  - 15078.2) / 365.25);
		long m =  (long) ((mjd - 14956.1 - (long)(y * 365.25) ) / 30.6001);
		final long d =  (mjd - 14956 - (long)(y * 365.25) - (long)(m * 30.6001));
		final long k =  ((m == 14) || (m == 15)) ? 1 : 0;
		y = y + k + 1900;
		m = m - 1 - (k*12);


		return Long.toString(y)+"/"+df2pos.format(m)+"/"+df2pos.format(d)+" "+hours+":"+minutes+":"+secs;
	}

	public static Date getUTCDate(final byte[] UTC_time) {
		return getUTCCalender(UTC_time).getTime();
	}

	public static Calendar getUTCCalender(final byte[] UTC_time) {
		final long mjd = getLong(UTC_time, 0, 2, 0xFFFF);
		final String hours = getBCD(UTC_time, 4, 2);
		final String minutes = getBCD(UTC_time, 6, 2);
		final String secs= getBCD(UTC_time, 8, 2);

		// algo: ETSI EN 300 468 - ANNEX C

		long y =  (long) ((mjd  - 15078.2) / 365.25);
		long m =  (long) ((mjd - 14956.1 - (long)(y * 365.25) ) / 30.6001);
		final long d =  (mjd - 14956 - (long)(y * 365.25) - (long)(m * 30.6001));
		final long k =  ((m == 14) || (m == 15)) ? 1 : 0;
		y = y + k + 1900;
		m = m - 1 - (k*12);

		return new GregorianCalendar((int)y, (int)m-1, (int)d, Integer.parseInt(hours), Integer.parseInt(minutes), Integer.parseInt(secs));
	}

	public static long getUTCmillis(final byte[] UTC_time) {
		return getUTCCalender(UTC_time).getTimeInMillis();
	}

	public static String getStreamTypeString(final int tag){
		if((0x20<=tag)&&(tag<=0x7e)){
			return "ITU-T Rec. H.222.0 | ISO/IEC 13818-1 Reserved";
		}

		if((0x80<=tag)&&(tag<=0xFF)){
			return "User Private";
		}

		switch (tag) {
		case 0x00: return"ITU-T | ISO/IEC Reserved";
		case 0x01: return"ISO/IEC 11172 Video";
		case 0x02: return"ITU-T Rec. H.262 | ISO/IEC 13818-2 Video or ISO/IEC 11172-2 constrained parameter video stream";
		case 0x03: return"ISO/IEC 11172 Audio";
		case 0x04: return"ISO/IEC 13818-3 Audio";
		case 0x05: return"ITU-T Rec. H.222.0 | ISO/IEC 13818-1 private_sections";
		case 0x06: return"ITU-T Rec. H.222.0 | ISO/IEC 13818-1 PES packets containing private data";
		case 0x07: return"ISO/IEC 13522 MHEG";
		case 0x08: return"ITU-T Rec. H.222.0 | ISO/IEC 13818-1 Annex A DSM-CC";
		case 0x09: return"ITU-T Rec. H.222.1";
		case 0x0A: return"ISO/IEC 13818-6 type A";
		case 0x0B: return"ISO/IEC 13818-6 type B";
		case 0x0C: return"ISO/IEC 13818-6 type C";
		case 0x0D: return"ISO/IEC 13818-6 type D";
		case 0x0E: return"ITU-T Rec. H.222.0 | ISO/IEC 13818-1 auxiliary";
		case 0x0F: return"ISO/IEC 13818-7 Audio with ADTS transport syntax";
		case 0x10: return"ISO/IEC 14496-2 Visual";
		case 0x11: return"ISO/IEC 14496-3 Audio with the LATM transport syntax as defined in ISO/IEC 14496-3 / AMD 1";
		case 0x12: return"ISO/IEC 14496-1 SL-packetized stream or FlexMux stream carried in PES packets";
		case 0x13: return"ISO/IEC 14496-1 SL-packetized stream or FlexMux stream carried in ISO/IEC14496_sections.";
		case 0x14: return"ISO/IEC 13818-6 Synchronized Download Protocol";

		// 29n9184t.doc Text of ISO/IEC 13818-1:2007/FPDAM 3.2 - Transport of Scalable Video over ITU-T Rec H.222.0 | ISO/IEC 13818-1
		// Amendment 3: Transport of Scalable Video over ITU-T Rec H.222.0 | ISO/IEC 13818-1
		// ISO/IEC 13818-1:2007/FPDAM 3.2

		case 0x15: return"Metadata carried in PES packets";
		case 0x16: return"Metadata carried in metadata_sections";
		case 0x17: return"Metadata carried in ISO/IEC 13818-6 Data Carousel";
		case 0x18: return"Metadata carried in ISO/IEC 13818-6 Object Carousel";
		case 0x19: return"Metadata carried in ISO/IEC 13818-6 Synchronized Download Protocol";
		case 0x1A: return"IPMP stream (defined in ISO/IEC 13818-11, MPEG-2 IPMP)";
		case 0x1B: return"AVC video stream as defined in ITU-T Rec. H.264 | ISO/IEC 14496-10 Video";
		case 0x1C: return"ISO/IEC 14496-3 Audio, without using any additional transport syntax, such as DST, ALS and SLS";
		case 0x1D: return"ISO/IEC 14496-17 Text";
		case 0x1E: return"Auxiliary video stream as defined in ISO/IEC 23002-3";
		case 0x1F: return"SVC video sub-bitstream of a video stream as defined in the Annex G of ITU-T Rec. H.264 | ISO/IEC 14496-10 Video";
		case 0x7f: return"IPMP stream";

		default:
			return "illegal/unknown value";
		}
	}


	// ETSI EN 300 706 V1.2.1 §11.3.3 Page Function Coding
	public static String getMIPPageFunctionString(final int pageCode){
		if((0x02<=pageCode)&&(pageCode<=0x4f)){
			return "Normal page, #sub pages "+pageCode;
		}

		if((0x52<=pageCode)&&(pageCode<=0x6f)){
			return "Reserved";
		}


		if((0x70<=pageCode)&&(pageCode<=0x77)){
			return "Subtitle page";
		}
		if((0x82<=pageCode)&&(pageCode<=0xcf)){
			return "TV schedule pages, multi-page set, #sub pages "+(pageCode-0x80);
		}
		if((0xf0<=pageCode)&&(pageCode<=0xf3)){
			return "Systems Pages for Broadcasters use (downstream processing)";
		}
		if((0xf4<=pageCode)&&(pageCode<=0xf6)){
			return "Engineering Test pages";
		}

		switch (pageCode) {
		case 0x00: return"Page not in transmission";
		case 0x01: return"Single normal page";
		case 0x50: return"Normal page, multi-page set Sub-pages in the range 80 to 2^12-1";
		case 0x51: return"Normal page, multi-page set Sub-pages in the range 2^12 to 2^13-2";
		case 0x78: return"Subtitle Menu Page";
		case 0x79: return"Page not following normal sub-code rules";
		case 0x7a: return"TV programme related warning page";
		case 0x7b: return"Current TV Programme information, multi-page set";
		case 0x7c: return"Current TV Programme information, single page";
		case 0x7d: return"\"Now and Next\" TV Programmes";
		case 0x7e: return"Index page to TV-related pages, multi-page set";
		case 0x7f: return"Index page to TV-related pages, single page";
		case 0x80: return"Page transmitted but NOT part of the public service";
		case 0x81: return"Single Page containing TV schedule information";
		case 0xd0: return"TV schedule pages, multi-page set, Sub-pages in the range 80 to 2^12-1";
		case 0xd1: return"TV schedule pages, multi-page set, Sub-pages in the range 2^12 to 2^13-2";
		case 0xe0: return"Page Format - CA - data broadcasting page, Sub-pages in the range 1 to 2^12-1";
		case 0xe1: return"Page Format - CA - data broadcasting page, Sub-pages in the range 2^12 to 2^13-2";
		case 0xe2: return"Page Format - CA - data broadcasting page, Number of sub-pages not defined in packets with Y = 15 to Y = 24";
		case 0xe3: return"Page Format - Clear data broadcasting page including EPG data";
		case 0xe4: return"Page Format - Clear data broadcasting page but not carrying EPG data";
		case 0xe5: return"DRCS page (use not defined)";
		case 0xe6: return"Object page (use not defined)";
		case 0xe7: return"Systems page without displayable element. Function defined by page number";
		case 0xe8: return"DRCS page referenced in the MOT for this magazine";
		case 0xe9: return"DRCS page referenced in the MOT for this magazine but not required by a page in this magazine";
		case 0xea: return"DRCS page referenced in the MOT for a different magazine but not required by a page in this magazine";
		case 0xeb: return"DRCS page not referenced in the MOT for a different magazine and required by a page in another magazine";
		case 0xec: return"Object page referenced in the MOT for this magazine";
		case 0xed: return"Object page referenced in the MOT for this magazine but not required by a page in this magazine";
		case 0xee: return"Object page referenced in the MOT for a different magazine but not required by a page in this magazine";
		case 0xef: return"Object page not referenced in the MOT for a different magazine and required by a page in another magazine";
		case 0xf7: return"Systems page with displayable element. Function defined by page number";
		case 0xf8: return"Keyword Search list page, multi-page set";
		case 0xf9: return"Keyword Search list page, single page";
		case 0xfc: return"Trigger message page";
		case 0xfd: return"Automatic Channel Installation (ACI)";
		case 0xfe: return"TOP page (BTT, AIT, MPT or MPT-EX)";


		default:
			return "Reserved";
		}
	}

	public static String getStreamTypeShortString(final int tag){

		if((0x20<=tag)&&(tag<=0x7e)){
			return "ISO/IEC 13818-1 Reserved";
		}

		if((0x80<=tag)&&(tag<=0xFF)){
			return "User Private";
		}

		switch (tag) {
		case 0x00: return"ITU-T | ISO/IEC Reserved";
		case 0x01: return"Video MPEG1";
		case 0x02: return"Video MPEG2";
		case 0x03: return"Audio MPEG1";
		case 0x04: return"Audio MPEG2";
		case 0x05: return"private_sections MPEG2";
		case 0x06: return"";
		case 0x07: return"MHEG";
		case 0x08: return"ITU-T Rec. H.222.0 | ISO/IEC 13818-1 Annex A DSM-CC";
		case 0x09: return"ITU-T Rec. H.222.1";
		case 0x0A: return"ISO/IEC 13818-6 type A";
		case 0x0B: return"ISO/IEC 13818-6 type B";
		case 0x0C: return"ISO/IEC 13818-6 type C";
		case 0x0D: return"ISO/IEC 13818-6 type D";
		case 0x0E: return"ITU-T Rec. H.222.0 | ISO/IEC 13818-1 auxiliary";
		case 0x0F: return"ISO/IEC 13818-7 Audio with ADTS transport syntax";
		case 0x10: return"ISO/IEC 14496-2 Visual";
		case 0x11: return"ISO/IEC 14496-3 Audio with the LATM transport syntax as defined in ISO/IEC 14496-3 / AMD 1";
		case 0x12: return"ISO/IEC 14496-1 SL-packetized stream or FlexMux stream carried in PES packets";
		case 0x13: return"ISO/IEC 14496-1 SL-packetized stream or FlexMux stream carried in ISO/IEC14496_sections.";
		case 0x14: return"ISO/IEC 13818-6 Synchronized Download Protocol";

		// 29n9184t.doc Text of ISO/IEC 13818-1:2007/FPDAM 3.2 - Transport of Scalable Video over ITU-T Rec H.222.0 | ISO/IEC 13818-1
		// Amendment 3: Transport of Scalable Video over ITU-T Rec H.222.0 | ISO/IEC 13818-1
		// ISO/IEC 13818-1:2007/FPDAM 3.2

		case 0x15: return"Metadata in PES packets";
		case 0x16: return"Metadata in metadata_sections";
		case 0x17: return"Metadata 13818-6 Data Carousel";
		case 0x18: return"Metadata 13818-6 Object Carousel";
		case 0x19: return"Metadata 13818-6 Synchronized Download Protocol";
		case 0x1A: return"IPMP stream (13818-11, MPEG-2 IPMP)";
		case 0x1B: return"H.264 | ISO/IEC 14496-10 Video";
		case 0x1C: return"ISO/IEC 14496-3 Audio, like DST, ALS and SLS";
		case 0x1D: return"ISO/IEC 14496-17 Text";
		case 0x1E: return"ISO/IEC 23002-3 Aux. video stream ";
		case 0x1F: return"ISO/IEC 14496-10 Video sub-bitstream";
		case 0x7f: return"IPMP stream";

		default:
			return "illegal value";
		}
	}


	/**
	 * ETR 162 and EN 300 472 and EN 301 775
	 * based on dvb_str.c
	 * @param dataId
	 * @return
	 */
	public static String getDataIDString(final int dataId){

		if((0x00<=dataId)&&(dataId<=0x0f)){
			return "Reserved";
		}
		if((0x10<=dataId)&&(dataId<=0x1f)){
			return "EBU data EN 300 472/ EN 301 775 (teletext, VPS, WSS)";
		}
		if((0x23<=dataId)&&(dataId<=0x7f)){
			return "Reserved";
		}
		if((0x80<=dataId)&&(dataId<=0x98)){
			return "user defined";
		}
		if((0x99<=dataId)&&(dataId<=0x9b)){
			return "EBU teletext/VPS/WSS/closed caption/VBI sample data";
		}
		if((0x9c<=dataId)&&(dataId<=0xff)){
			return "user defined";
		}

		switch (dataId) {
		case 0x20: return"DVB subtitling EN 300 743";
		case 0x21: return"DVB synchronous data stream";
		case 0x22: return"DVB synchronized data stream";

		default:
			return "illegal value";
		}
	}


	/**
	 * Add a list of DefaultMutableTreeNodes to parent. if modus != simple all items are put below an intermediate node with label 'label'. If modus is simple all items are added directly to parent
	 * @param parent
	 * @param itemList
	 * @param modus
	 * @param label
	 */
	public static <U extends TreeNode>void addListJTree(final DefaultMutableTreeNode parent,final Collection<U> itemList, final int modus, final String label) {
		if((itemList!=null)&&(itemList.size()!=0)){
			if(simpleModus(modus)){ // simple layout
				addToList(parent, itemList, modus);
			}else{
				final DefaultMutableTreeNode descriptorListNode = new DefaultMutableTreeNode(new KVP(label +": "+ itemList.size()+" entries"));
				addToList(descriptorListNode, itemList, modus);
				parent.add(descriptorListNode);
			}
		}
	}



	public static <U> void addToList(final DefaultMutableTreeNode parent,
			final Collection<U> itemList, final int modus) {
		if(countListModus(modus)){
			int count = 0;
			for (final Iterator<U> iter = itemList.iterator(); iter.hasNext();) {
				DefaultMutableTreeNode node = ((TreeNode) iter.next()).getJTreeNode(modus);
				Object userObject = node.getUserObject();
				if (userObject instanceof KVP) {
					KVP kvp = (KVP)userObject;
					kvp.appendLabel(" ["+Integer.toString(count)+"]");
					count++;
				}
				parent.add(node);
			}
		}else{
			for (final Iterator<U> iter = itemList.iterator(); iter.hasNext();) {
				parent.add(((TreeNode) iter.next()).getJTreeNode(modus));
			}
		}
	}

	/**
	 * convert array of bytes into a String, ascii only. replace non ascii character with a dot '.'.
	 * @param block
	 * @return
	 */
	public static String toSafeString(final byte[] block) {
		final StringBuilder buf = new StringBuilder();
		if(block==null){
			return buf.toString();
		}

		final int len = block.length;
		for (int i = 0; i < len; i++) {
			if((32<=getUnsignedByte(block[i]))&&(getUnsignedByte(block[i])<=127)){
				buf.append((char)(block[i]));
			}else{
				buf.append('.');
			}
		}
		return buf.toString();
	}

	/**
	 * convert array of bytes into a String, ascii only. replace non ascii character with a dot '.'.
	 * @param block
	 * @param offset
	 * @param len
	 * @return
	 */
	public static String toSafeString(final byte[] block, final int offset, final int len) {
		final StringBuilder buf = new StringBuilder();
		if(block==null){
			return buf.toString();
		}
		final int end = Math.min(block.length, offset+len);
		for (int i = offset; i < end; i++) {
			final byte b=block[i];
			if((32<=getUnsignedByte(b))&&(getUnsignedByte(b)<=127)){
				buf.append((char)(b));
			}else{
				buf.append('.');
			}
		}
		return buf.toString();
	}

	public static byte[] copyOfRange(final byte[] original, final int from, final int to) {
		final int newLength = to - from;
		if (newLength < 0) {
			throw new IllegalArgumentException(from + " > " + to);
		}
		final byte[] copy = new byte[newLength];
		System.arraycopy(original, from, copy, 0,
				Math.min(original.length - from, newLength));
		return copy;
	}


	/**
	 * replace all 'html'characters in the string with their html-entity
	 * Output is now safe for use in HTML fragments
	 * 
	 * @param s
	 * @return
	 */
	public static String escapeHTML(final String s){
		final StringBuilder sb = new StringBuilder();
		if(s==null){
			return "&nbsp;";
		}
		final int n = s.length();
		for (int i = 0; i < n; i++) {
			final char c = s.charAt(i);
			switch (c) {
			case ' ': sb.append("&nbsp;"); break;
			case '<': sb.append("&lt;"); break;
			case '>': sb.append("&gt;"); break;
			case '&': sb.append("&amp;"); break;
			case '"': sb.append("&quot;"); break;
			case 'à': sb.append("&agrave;");break;
			case 'À': sb.append("&Agrave;");break;
			case 'â': sb.append("&acirc;");break;
			case 'Â': sb.append("&Acirc;");break;
			case 'ä': sb.append("&auml;");break;
			case 'Ä': sb.append("&Auml;");break;
			case 'å': sb.append("&aring;");break;
			case 'Å': sb.append("&Aring;");break;
			case 'æ': sb.append("&aelig;");break;
			case 'Æ': sb.append("&AElig;");break;
			case 'ç': sb.append("&ccedil;");break;
			case 'Ç': sb.append("&Ccedil;");break;
			case 'é': sb.append("&eacute;");break;
			case 'É': sb.append("&Eacute;");break;
			case 'è': sb.append("&egrave;");break;
			case 'È': sb.append("&Egrave;");break;
			case 'ê': sb.append("&ecirc;");break;
			case 'Ê': sb.append("&Ecirc;");break;
			case 'ë': sb.append("&euml;");break;
			case 'Ë': sb.append("&Euml;");break;
			case 'ï': sb.append("&iuml;");break;
			case 'Ï': sb.append("&Iuml;");break;
			case 'ô': sb.append("&ocirc;");break;
			case 'Ô': sb.append("&Ocirc;");break;
			case 'ö': sb.append("&ouml;");break;
			case 'Ö': sb.append("&Ouml;");break;
			case 'ø': sb.append("&oslash;");break;
			case 'Ø': sb.append("&Oslash;");break;
			case 'ß': sb.append("&szlig;");break;
			case 'ù': sb.append("&ugrave;");break;
			case 'Ù': sb.append("&Ugrave;");break;
			case 'û': sb.append("&ucirc;");break;
			case 'Û': sb.append("&Ucirc;");break;
			case 'ü': sb.append("&uuml;");break;
			case 'Ü': sb.append("&Uuml;");break;
			case '®': sb.append("&reg;");break;
			case '©': sb.append("&copy;");break;
			case '€': sb.append("&euro;"); break;
			default:  sb.append(c); break;
			}
		}
		return sb.toString();
	}


	public static boolean simpleModus(final int m){
		return ((m & DVBtree.SIMPLE_MODUS )!=0);
	}

	public static boolean psiOnlyModus(final int m){
		return ((m&DVBtree.PSI_ONLY_MODUS)!=0);
	}

	public static boolean packetModus(final int m){
		return ((m&DVBtree.PACKET_MODUS)!=0);
	}

	public static boolean countListModus(final int m){
		return ((m&DVBtree.COUNT_LIST_ITEMS_MODUS)!=0);
	}
	public static String stripLeadingZeros(final String s) {
		final int len = s.length()-1; // leave at least one zero if that is the only char
		int st = 0;

		while ((st < len) && (s.charAt(st)=='0')&&(s.charAt(st+1)!='.')) {
			st++;
		}

		return s.substring(st);
	}

	public static String formatIPNumber(final byte[] ip){
		final StringBuilder r = new StringBuilder();
		if(ip.length>0){
			r.append(getUnsignedByte(ip[0]));
		}
		for(int i=1;i<ip.length;i++){
			r.append('.').append(getUnsignedByte(ip[i]));
		}
		return r.toString();
	}

	/**
	 * @param program_clock_reference
	 * @return
	 * 
	 * based on DVBsnoop helper.c, which is based on "dvbtextsubs  Dave Chapman"
	 * 
	 */
	public static String printPCRTime(final long program_clock_reference) {

		long  h,m,s;
		long  u;
		final long  p = program_clock_reference/27;
		final long  fa = 1000000;

		//  following lines basically taken from "dvbtextsubs  Dave Chapman"
		h=(p/(fa*60*60));
		m=(p/(fa*60))-(h*60);
		s=(p/fa)-(h*3600)-(m*60);
		u=p-(h*fa*60*60)-(m*fa*60)-(s*fa);

		return h+":"+f2.format(m)+":"+f2.format(s)+"."+f6.format(u);
		//out (v,"  [= PCR-Timestamp: %d:%02d:%02d.%06ld]", h,m,s,u);
	}

	/**
	 * @param ts
	 * @return
	 * 
	 * based on DVBsnoop helper.c, which is based on "dvbtextsubs  Dave Chapman"
	 * 
	 */
	public static String printTimebase90kHz(final long ts) {

		long  h,m,s;
		long  u;
		final long  p = ts/9;

		//  following lines basically taken from "dvbtextsubs  Dave Chapman"
		h=(p/(10000L*60*60));
		m=(p/(10000L*60))-(h*60);
		s=(p/10000L)-(h*3600)-(m*60);
		u=p-(h*10000L*60*60)-(m*10000L*60)-(s*10000L);

		return h+":"+f2.format(m)+":"+f2.format(s)+"."+f4.format(u);
		//out (v,"  [= PCR-Timestamp: %d:%02d:%02d.%06ld]", h,m,s,u);
	}

	public static int getHammingReverseByte(final byte b){
		final int t= getUnsignedByte(b);
		int r = (t & 0x40)>>6;
		r |= (t & 0x10)>>3;
		r |= (t & 0x04);
		r |= (t & 0x01)<<3;
		return r;

	}

	public static int getHammingByte(final byte b){
		final int t= getUnsignedByte(b);
		int r = (t & 0x40)>>3;
		r |= (t & 0x10)>>2;
			r |= (t & 0x04)>>1;
			r |= (t & 0x01);
			return r;

	}


	public static int getHamming24_8Byte(final byte[] b,final int offset){

		final int lsb = ((b[offset] & 0x20)<<2)|((b[offset] & 0x0e)<<3)|((b[offset+1] & 0xF0)>>4); // 8 lowest bits
		final int msb = ((b[offset+1] & 0x0E)<<4)|((b[offset+2] & 0xF8)>>3); // 8 medium bits
		final int hsb = ((b[offset+2] & 0x06)<<5); // 2 highest bits, shift to left, after inv they are to the right.

		return (65536*invtab[hsb])+ (256*invtab[msb])+ invtab[lsb];

	}


	public static int invtab[] = {
		0x00, 0x80, 0x40, 0xc0, 0x20, 0xa0, 0x60, 0xe0,
		0x10, 0x90, 0x50, 0xd0, 0x30, 0xb0, 0x70, 0xf0,
		0x08, 0x88, 0x48, 0xc8, 0x28, 0xa8, 0x68, 0xe8,
		0x18, 0x98, 0x58, 0xd8, 0x38, 0xb8, 0x78, 0xf8,
		0x04, 0x84, 0x44, 0xc4, 0x24, 0xa4, 0x64, 0xe4,
		0x14, 0x94, 0x54, 0xd4, 0x34, 0xb4, 0x74, 0xf4,
		0x0c, 0x8c, 0x4c, 0xcc, 0x2c, 0xac, 0x6c, 0xec,
		0x1c, 0x9c, 0x5c, 0xdc, 0x3c, 0xbc, 0x7c, 0xfc,
		0x02, 0x82, 0x42, 0xc2, 0x22, 0xa2, 0x62, 0xe2,
		0x12, 0x92, 0x52, 0xd2, 0x32, 0xb2, 0x72, 0xf2,
		0x0a, 0x8a, 0x4a, 0xca, 0x2a, 0xaa, 0x6a, 0xea,
		0x1a, 0x9a, 0x5a, 0xda, 0x3a, 0xba, 0x7a, 0xfa,
		0x06, 0x86, 0x46, 0xc6, 0x26, 0xa6, 0x66, 0xe6,
		0x16, 0x96, 0x56, 0xd6, 0x36, 0xb6, 0x76, 0xf6,
		0x0e, 0x8e, 0x4e, 0xce, 0x2e, 0xae, 0x6e, 0xee,
		0x1e, 0x9e, 0x5e, 0xde, 0x3e, 0xbe, 0x7e, 0xfe,
		0x01, 0x81, 0x41, 0xc1, 0x21, 0xa1, 0x61, 0xe1,
		0x11, 0x91, 0x51, 0xd1, 0x31, 0xb1, 0x71, 0xf1,
		0x09, 0x89, 0x49, 0xc9, 0x29, 0xa9, 0x69, 0xe9,
		0x19, 0x99, 0x59, 0xd9, 0x39, 0xb9, 0x79, 0xf9,
		0x05, 0x85, 0x45, 0xc5, 0x25, 0xa5, 0x65, 0xe5,
		0x15, 0x95, 0x55, 0xd5, 0x35, 0xb5, 0x75, 0xf5,
		0x0d, 0x8d, 0x4d, 0xcd, 0x2d, 0xad, 0x6d, 0xed,
		0x1d, 0x9d, 0x5d, 0xdd, 0x3d, 0xbd, 0x7d, 0xfd,
		0x03, 0x83, 0x43, 0xc3, 0x23, 0xa3, 0x63, 0xe3,
		0x13, 0x93, 0x53, 0xd3, 0x33, 0xb3, 0x73, 0xf3,
		0x0b, 0x8b, 0x4b, 0xcb, 0x2b, 0xab, 0x6b, 0xeb,
		0x1b, 0x9b, 0x5b, 0xdb, 0x3b, 0xbb, 0x7b, 0xfb,
		0x07, 0x87, 0x47, 0xc7, 0x27, 0xa7, 0x67, 0xe7,
		0x17, 0x97, 0x57, 0xd7, 0x37, 0xb7, 0x77, 0xf7,
		0x0f, 0x8f, 0x4f, 0xcf, 0x2f, 0xaf, 0x6f, 0xef,
		0x1f, 0x9f, 0x5f, 0xdf, 0x3f, 0xbf, 0x7f, 0xff,
	};


	public static int indexOf(final byte[] source,  final byte[]target, final int fromIndex){
		if (fromIndex >= source.length) {
			return  -1;
		}
		final byte first  = target[0];
		final int max = source.length - target.length;

		for (int i = fromIndex; i < max; i++) {
			/* Look for first byte. */
			if (source[i] != first) {
				while ((++i <= max) && (source[i] != first)){ // NOPMD by Eric on 22-5-12 14:46
					// EMPTY body
				}
			}

			/* Found first byte, now look at the rest of v2 */
			if (i <= max) {
				int j = i + 1;
				final int end = (j + target.length) - 1;
				for (int k =  1; (j < end) && (source[j] ==
						target[k]); j++, k++){
					// EMPTY BLOCK
				}

				if (j == end) {
					/* Found whole string. */
					return i ;
				}
			}
		}
		return -1;


	}


	public static DecimalFormat df2pos = new DecimalFormat("00");


	public static DecimalFormat df3pos = new DecimalFormat("000");


	/**
	 * Compare two byte[] with each other
	 * @param data_block
	 * @param offset
	 * @param len
	 * @param data_block2
	 * @param offset2
	 * @param len2
	 * @return
	 */
	public static boolean equals(final byte[] data_block, final int offset, final int len, final byte[] data_block2, final int offset2, final int len2) {

		if ((data_block==null) || (data_block2==null)) {
			return false;
		}

		if (len != len2) {
			return false;
		}

		for (int i=0; i<len; i++){
			if (data_block[i+offset] != data_block2[i+offset2]){
				return false;
			}
		}

		return true;

	}



	public static String getAspectRatioInformationString(final int s) {

		switch (s) {
		case 0: return "forbidden";
		case 1: return "1,0 (Square Sample)";
		case 2 : return "3÷4";
		case 3 : return "9÷16";
		case 4 : return "1÷2,21";

		default:
			return "reserved";
		}
	}


	public static String getHTML(final byte [] byteValue, final int offset, final int len) {

		final StringBuilder b= new StringBuilder();
		b.append("<code>");
		// header line
		b.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;0001 0203 0405 0607 0809 0A0B 0C0D 0E0F 0123456789ABCDEF<br>");
		final int lines=1+((len-1)/16);
		for (int l = 0; l < lines; l++) {
			final int start=l*16;
			b.append(Utils.toHexString(start,6));
			b.append(' ');
			b.append("<span style=\"background-color: white\">");
			final int lineLen=(l==(lines-1))?(len-(l*16)):16; // if last line calculate bytes left, else 16

			// show byte as hex
			for (int i = 0; i < 16; i++) {
				if(i<lineLen){
					b.append(Utils.toHexString(byteValue,  offset+(l*16)+i, 1));
				}else{
					b.append("&nbsp;&nbsp;");
				}
				// after every second byte insert space
				if((i%2)!=0){
					b.append("&nbsp;");
				}
			}

			// string representation at end of line
			b.append(Utils.escapeHTML(Utils.toSafeString(byteValue, offset+(l*16), lineLen))).append("</span><br>");
		}

		b.append("</code>");
		return b.toString();
	}
}
