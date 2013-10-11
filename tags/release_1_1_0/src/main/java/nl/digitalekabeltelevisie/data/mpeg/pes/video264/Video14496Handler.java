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

package nl.digitalekabeltelevisie.data.mpeg.pes.video264;



import static nl.digitalekabeltelevisie.util.Utils.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import nl.digitalekabeltelevisie.controller.ChartLabel;
import nl.digitalekabeltelevisie.controller.KVP;
import nl.digitalekabeltelevisie.data.mpeg.PesPacketData;
import nl.digitalekabeltelevisie.data.mpeg.pes.GeneralPesHandler;
import nl.digitalekabeltelevisie.gui.ImageSource;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.general.DefaultKeyedValues2DDataset;

/**
 * @author Eric Berendsen
 *
 */
public class Video14496Handler extends GeneralPesHandler implements ImageSource{


	/**
	 * Meta Iterator to iterate over all NALUnits in this PES stream, regardless of grouping in PES Packets
	 *
	 * In general this does not work for streams with no alignment, So every NALUnit should be contained in a PES packet.
	 *
	 * @author Eric
	 *
	 */
	private class NALUnitIterator{

		Iterator<PesPacketData> pesIterator = null;
		NALUnit nextSection = null;
		private Iterator<NALUnit> sectionIter;

		public NALUnitIterator() {
			pesIterator = pesPackets.iterator();
			sectionIter = getNextSectionIter();
			if(sectionIter!=null){
				nextSection = sectionIter.next();
			}
		}

		private Iterator<NALUnit> getNextSectionIter(){

			Iterator<NALUnit> result = null;
			do {
				Video14496PESDataField pesPacket = (Video14496PESDataField )pesIterator.next();
				result = pesPacket.getNalUnits().iterator();

			} while (((result==null)||!result.hasNext())&&(pesIterator.hasNext()));
			return result;

		}

		public NALUnit next() {
			NALUnit result = nextSection;
			if((sectionIter!=null)&&sectionIter.hasNext()){
				nextSection = sectionIter.next();
			}else if(pesIterator.hasNext()){
				sectionIter= getNextSectionIter();
				if(sectionIter.hasNext()){
					nextSection = sectionIter.next();
				}else{
					nextSection = null;
				}
			}else{
				nextSection = null;
			}

			return result;
		}


	}


	/* (non-Javadoc)
	 * @see nl.digitalekabeltelevisie.data.mpeg.pes.GeneralPesHandler#processPesDataBytes(int, byte[], int, int)
	 */
	@Override
	public void processPesDataBytes(final PesPacketData pesData){
		pesPackets.add(new Video14496PESDataField(pesData));

	}



	/* (non-Javadoc)
	 * @see nl.digitalekabeltelevisie.data.mpeg.pes.GeneralPesHandler#getJTreeNode(int)
	 */
	public DefaultMutableTreeNode getJTreeNode(final int modus) {
		final DefaultMutableTreeNode s=new DefaultMutableTreeNode(new KVP("PES Data",this));
		addListJTree(s,pesPackets,modus,"PES Packets");

		return s;
	}


	/* (non-Javadoc)
	 * @see nl.digitalekabeltelevisie.gui.HTMLSource#getHTML()
	 */
	@Override
	public BufferedImage getImage() {

		ChartLabel label = null;
		List<int[]> frameSize  = new ArrayList<int[]>();
		List<ChartLabel> labels = new ArrayList<ChartLabel>();

		int[] accessUnitData = new int[6]; // 0= P, 1 = B, 2 = I, 3 = SP, 4 = SI (all mod 5), 5 = Filler data
		int count = 0;

		NALUnitIterator nalIter = new NALUnitIterator();
		NALUnit unit = nalIter.next();
		while(unit!=null){
			RBSP rbsp = unit.getRbsp();
			if(rbsp!=null){
				if(( rbsp instanceof Access_unit_delimiter_rbsp) ||
						( rbsp instanceof Seq_parameter_set_rbsp) ||
						( rbsp instanceof Pic_parameter_set_rbsp) ||
						( rbsp instanceof Sei_rbsp)){

					if(notZero(accessUnitData)){
						label =new ChartLabel(""+count, (short)count);
						labels.add(label);
						frameSize.add(accessUnitData);
						count++;
					}
					accessUnitData = new int[6];
				}else if( rbsp instanceof Slice_layer_without_partitioning_rbsp){
					Slice_header header = ((Slice_layer_without_partitioning_rbsp)rbsp).getSlice_header();
					int slice_type = header.getSlice_type();
					int size = unit.getNumBytesInRBSP();
					accessUnitData[slice_type%5] += size;
				}else if( rbsp instanceof Filler_data_rbsp){
					int size = unit.getNumBytesInRBSP();
					accessUnitData[5] += size;
				}
			}
			unit =  nalIter.next();
		}

		DefaultKeyedValues2DDataset dataset = new DefaultKeyedValues2DDataset();


		for (int i = 0; i < 6; i++) {
			String type=getSlice_typeString(i);
			Iterator<int[]> frameSizeIter = frameSize.iterator();
			for(ChartLabel l:labels){
				if(frameSizeIter.hasNext()){
					int[] v = frameSizeIter.next();
					dataset.setValue(v[i], type,l);
				}
			}
		}

		StackedBarRenderer renderer = new StackedBarRenderer();
		renderer.setShadowVisible(false);
		renderer.setDrawBarOutline(false);
		renderer.setItemMargin(0.0);

		renderer.setSeriesPaint(0, Color.BLUE); // p
		renderer.setSeriesPaint(1, Color.GREEN); // b
		renderer.setSeriesPaint(2, Color.RED); // i
		renderer.setSeriesPaint(3, Color.YELLOW); //SP
		renderer.setSeriesPaint(4, Color.PINK); //SI
		renderer.setSeriesPaint(5, Color.GRAY); // filler

		final CategoryAxis categoryAxis = new CategoryAxis("time");
		categoryAxis.setCategoryMargin(0.0);
		categoryAxis.setUpperMargin(0.0);
		categoryAxis.setLowerMargin(0.0);
		categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
		final ValueAxis valueAxis = new NumberAxis("frame size (bytes)");

		final CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, renderer);
		String title = getPID().getShortLabel()+" (Access Units, Transmission Order)";
		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT,plot, true	);

		return chart.createBufferedImage(( frameSize.size()*18)+100, 640);
	}


	/**
	 * @param accessUnitData
	 * @return
	 */
	private boolean notZero(int[] accessUnitData) {
		for (int i : accessUnitData) {
			if(i!=0){
				return true;
			}
		}

		return false;
	}


	private static String getSlice_typeString(int slice_type){
		switch (slice_type) {
		case  0: return "P";
		case  1 : return "B";
		case  2 : return "I";
		case  3 : return "SP";
		case  4 : return "SI";
		case  5 : return "Filler Data";

		default:
			return "??";
		}

	}


}
