package nl.digitalekabeltelevisie.data.mpeg.psi.nonstandard;

/**
*
*  http://www.digitalekabeltelevisie.nl/dvb_inspector
*
*  This code is Copyright 2009-2020 by Eric Berendsen (e_berendsen@digitalekabeltelevisie.nl)
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

import java.nio.charset.*;
import java.util.*;
import java.util.logging.*;

import javax.swing.tree.DefaultMutableTreeNode;

import nl.digitalekabeltelevisie.controller.*;
import nl.digitalekabeltelevisie.data.mpeg.descriptors.*;
import nl.digitalekabeltelevisie.data.mpeg.descriptors.privatedescriptors.m7fastscan.M7LogicalChannelDescriptor;
import nl.digitalekabeltelevisie.data.mpeg.psi.*;
import nl.digitalekabeltelevisie.data.mpeg.psi.nonstandard.FNTsection.TransportStream;
import nl.digitalekabeltelevisie.data.mpeg.psi.nonstandard.FSTsection.Service;
import nl.digitalekabeltelevisie.gui.HTMLSource;
import nl.digitalekabeltelevisie.util.*;

public class OperatorFastscan implements TreeNode, HTMLSource {
	
			
	// encoding is always ISO-8859-9, ignoring character selection information as defined in Annex A of ETSI EN 300 468
	private static Charset charset = Charset.forName("ISO-8859-9");
	
	private static final Logger	logger	= Logger.getLogger(OperatorFastscan.class.getName());

	
	private int pid;
	private int operatorNetworkId = -1;
	private M7Fastscan m7Fastscan;
	
	FNTsection[] fntSections;
	FSTsection[] fstSections;

	public OperatorFastscan(int pid, M7Fastscan m7Fastscan) {
		this.pid = pid;
		this.m7Fastscan = m7Fastscan;
	}

	@Override
	public DefaultMutableTreeNode getJTreeNode(int modus) {
		final KVP networkKVP = new KVP("Pid",pid,getOperatorSubListName());
		networkKVP.setHtmlSource(this);
		final DefaultMutableTreeNode n = new DefaultMutableTreeNode(networkKVP);
		
		if(fstSections!=null) {
			DefaultMutableTreeNode fst = new DefaultMutableTreeNode(new KVP("FST"));
			for (final FSTsection fstSection : fstSections) {
				if(fstSection!= null){
					AbstractPSITabel.addSectionVersionsToJTree(fst, fstSection, modus);
				}
			}
			
			n.add(fst);
		}

		if (fntSections != null) {
			DefaultMutableTreeNode fnt = new DefaultMutableTreeNode(new KVP("FNT"));
			for (final FNTsection fntSection : fntSections) {
				if (fntSection != null) {
					AbstractPSITabel.addSectionVersionsToJTree(fnt, fntSection, modus);
				}
			}

			n.add(fnt);
		}
		return n;
	}

	private String getOperatorSubListName() {
		return m7Fastscan.getOperatorSubListName(operatorNetworkId,pid);
	}

	public void update(FSTsection section) {
		if(operatorNetworkId==-1) {
			operatorNetworkId = section.getOperatorNetworkID();
		}else {
			if(operatorNetworkId != section.getOperatorNetworkID()) {
				logger.log(Level.WARNING, "update::FSTsection, ocurrent peratorNetworkId:"+ operatorNetworkId +" not equal to section.getOperatorNetworkID():"+section.getOperatorNetworkID());
			}
		}

		if(fstSections == null) {
			fstSections = new FSTsection[section.getSectionLastNumber()+1];
		}
		if(fstSections[section.getSectionNumber()]==null){
			fstSections[section.getSectionNumber()] = section;
		}else{
			final TableSection last = fstSections[section.getSectionNumber()];
			AbstractPSITabel.updateSectionVersion(section, last);
		}

	}

	public void update(FNTsection section) {
		if(operatorNetworkId==-1) {
			operatorNetworkId = section.getOperatorNetworkID();
		}else {
			if(operatorNetworkId != section.getOperatorNetworkID()) {
				logger.log(Level.WARNING, "update::FNTsection, ocurrent peratorNetworkId:"+ operatorNetworkId +" not equal to section.getOperatorNetworkID():"+section.getOperatorNetworkID());
			}
		}
		if(fntSections == null) {
			fntSections = new FNTsection[section.getSectionLastNumber()+1];
		}
		if(fntSections[section.getSectionNumber()]==null){
			fntSections[section.getSectionNumber()] = section;
		}else{
			final TableSection last = fntSections[section.getSectionNumber()];
			AbstractPSITabel.updateSectionVersion(section, last);
		}
		
	}

	@Override
	public String getHTML() {
		// build map<map> of streams, by
		//   onid        ts_id       
		Map<Integer,Map<Integer,TransportStream>> fntStreams = new HashMap<>();

		if(fntSections==null){
			return "FNT Missing";
		}
		
		if(fstSections==null){
			return "FST Missing";
		}

		for (FNTsection fnTsection : fntSections) {
			for (FNTsection.TransportStream stream : fnTsection.transportStreamList) {
				final int onid = stream.getOriginalNetworkID();
				int ts_id = stream.getTransportStreamID();

				Map<Integer, TransportStream> netWorkMap = fntStreams.computeIfAbsent(onid, k -> new HashMap<>());
				netWorkMap.put(ts_id, stream);
			}
		}
		
		Map<Integer, FSTsection.Service> channelMap = new HashMap<>();
		for (FSTsection fsTsection : fstSections) {
			if(fsTsection!=null) {
				for (FSTsection.Service service : fsTsection.serviceList) {
					int onid = service.getOriginal_network_id();
					int ts_id = service.getTransport_stream_id();
	
					TransportStream stream = fntStreams.get(onid).get(ts_id);
					List<M7LogicalChannelDescriptor> m7LogicalChannelDescriptorList = Descriptor
							.findGenericDescriptorsInList(stream.descriptorList, M7LogicalChannelDescriptor.class);
					M7LogicalChannelDescriptor m7LogicalChannelDescriptor = m7LogicalChannelDescriptorList.get(0);
					for (M7LogicalChannelDescriptor.LogicalChannel channel : m7LogicalChannelDescriptor.getChannelList()) {
						if (channel.getServiceID() == service.getService_id()) {
							channelMap.put(channel.getLogicalChannelNumber(), service);
						}
					}
				}
			}
		}
		
		StringBuilder sb = new StringBuilder("<code>");
		String header = String.format("%5s %5s %5s %5s %-40s %-30s %-40s<br>","LCN","ONID","TS_ID","SID","Service Name","Service Type", "Service Provider");
		String htmlHeader = header.replace(" ", "&nbsp;");
		sb.append(htmlHeader);
		for (Integer lcn : new TreeSet<Integer>(channelMap.keySet())) {
			Service service = channelMap.get(lcn);
			List<ServiceDescriptor> sdList = Descriptor.findGenericDescriptorsInList(service.getDescriptorList(),ServiceDescriptor.class);
			ServiceDescriptor sd = sdList.get(0);
			String printableServiceName = getDecodedString(sd.getServiceName(), 40,charset);
			String printableServiceProviderName = getDecodedString(sd.getServiceProviderName(), 40,charset);
			String line = String.format("%5d %5d %5d %5d %-40s %-30s %-40s<br>", lcn,service.getOriginal_network_id(), service.getTransport_stream_id(),service.getService_id(),printableServiceName,Descriptor.getServiceTypeString(sd.getServiceType()), printableServiceProviderName);
			String htmlString = line.replace(" ", "&nbsp;");
			sb.append(htmlString);
		}
		sb.append("</code>");
		return sb.toString();
	}

	private String getDecodedString(DVBString dvbString, int targetLen, Charset charset) {
		byte[] data = dvbString.getData();
		int offset = dvbString.getOffset();
		int len = dvbString.getLength();

		String s;
		if (charset != null) {
			s = new String(data, offset + 1, len, charset);
		} else {
			s = Iso6937ToUnicode.convert(data, offset + 1, len);
		}
		if (s.length() > targetLen) {
			s = s.substring(0, targetLen);
		}

		return s;
	}
}
