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

package nl.digitalekabeltelevisie.data.mpeg.psi;

import static nl.digitalekabeltelevisie.util.Utils.*;

import java.util.*;

import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;

import nl.digitalekabeltelevisie.controller.KVP;
import nl.digitalekabeltelevisie.data.mpeg.PSI;
import nl.digitalekabeltelevisie.util.Utils;
import nl.digitalekabeltelevisie.util.tablemodel.*;

public class BAT extends AbstractPSITabel{

	private Map<Integer, BATsection []> networks = new HashMap<Integer, BATsection []>();

	public BAT(final PSI parent){
		super(parent);
	}

	public void update(final BATsection section){

		final int key = section.getBouqetID();
		BATsection [] sections= networks.get(key);

		if(sections==null){
			sections = new BATsection[section.getSectionLastNumber()+1];
			networks.put(key, sections);
		}
		if(sections[section.getSectionNumber()]==null){
			sections[section.getSectionNumber()] = section;
		}else{
			final TableSection last = sections[section.getSectionNumber()];
			updateSectionVersion(section, last);
		}
	}

	public DefaultMutableTreeNode getJTreeNode(final int modus) {

		KVP kvpBat = new KVP("BAT");
		if(!networks.isEmpty()) {
			kvpBat.setTableSource(this::getTableModel);
		}
		final DefaultMutableTreeNode t = new DefaultMutableTreeNode(kvpBat);
		final TreeSet<Integer> s = new TreeSet<Integer>(networks.keySet());

		final Iterator<Integer> i = s.iterator();
		while(i.hasNext()){
			final Integer bouquetNo=i.next();


			KVP kvp = new KVP("bouqet_id",bouquetNo, Utils.getBouquetIDString(bouquetNo));
			kvp.setTableSource(()->getTableForBouqetID(bouquetNo));
			final DefaultMutableTreeNode n = new DefaultMutableTreeNode(kvp);
			final BATsection [] sections = networks.get(bouquetNo);
			for (final BATsection tsection : sections) {
				if(tsection!= null){
					if(!Utils.simpleModus(modus)){ // show all details
						n.add(tsection.getJTreeNode(modus));
					}else{ // keep it simple
						final BATsection batSection = tsection;
						addListJTree(n,batSection.getNetworkDescriptorList(),modus,"network_descriptors");
						addListJTree(n,batSection.getTransportStreamList(),modus,"transport_stream_loop");
					}
				}
			}
			t.add(n);

		}
		return t;
	}
	
	
	private TableModel getTableForBouqetID(int bouqetNo) {
		return TableUtils.getTableModel(BAT::buildBatTableHeader,()->getRowDataForBouqetID(bouqetNo));
	}

	List<Map<String, Object>> getRowDataForBouqetID(int bouqetNo) {
		List<Map<String, Object>> rowData = new ArrayList<Map<String,Object>>(); 

		final BATsection [] sections = networks.get(bouqetNo);
		
		for (final BATsection tsection : sections) {
			if(tsection!= null){
				rowData.addAll(tsection.getRowData());
			}
		}
		return rowData;
	}

	
	
	public TableModel getTableModel() {
		return TableUtils.getTableModel(BAT::buildBatTableHeader,()->getRowDataForBat());
	}

	List<Map<String, Object>> getRowDataForBat() {
		List<Map<String, Object>> rowData = new ArrayList<Map<String,Object>>(); 
		
		for(BATsection[] batSections:networks.values()) {
			for (final BATsection tsection : batSections) {
				if(tsection!= null){
					rowData.addAll(tsection.getRowData());
				}
			}
		}
		return rowData;
	}

	
	
	
	static TableHeader buildBatTableHeader() {
		TableHeader tableHeader =  new TableHeader.Builder().
				addOptionalColumn("bouquet id", "bouquet_id", Integer.class).
				addOptionalColumn("bouquet id name", "bouquet_id_name", String.class).
				
				addOptionalColumn("bouquet name descriptor", "bouquet.name", String.class).
				
				addOptionalColumn("transport stream id", "transport_stream_id", Integer.class).
				addOptionalColumn("original network id", "original_network_id", Integer.class).
				addOptionalColumn("original network name", "original_network_name", String.class).
				
				
				
				build();
		return tableHeader;
	}



}
