/**
 *
 *  http://www.digitalekabeltelevisie.nl/dvb_inspector
 *
 *  This code is Copyright 2009-2022 by Eric Berendsen (e_berendsen@digitalekabeltelevisie.nl)
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

package nl.digitalekabeltelevisie.gui;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import nl.digitalekabeltelevisie.main.DVBinspector;
import nl.digitalekabeltelevisie.util.PreferencesManager;

public class FileOpenAction extends AbstractAction {


	private final FileNameExtensionFilter tsFilter = new FileNameExtensionFilter("MPEG-TS (*.ts;*.mpg;*.mpeg;*.m2ts;*.mts;*.tsa;*.tsv;*.trp)", "ts","mpg","mpeg","m2ts","mts","tsa","tsv","trp");
	private final JFileChooser	fileChooser;
	private final DVBinspector	contr;


	public FileOpenAction(final DVBinspector controller) {
		super("Open");
		fileChooser = new JFileChooser();
		fileChooser.addChoosableFileFilter(tsFilter);
		
		contr = controller;
	}

	public void actionPerformed(final ActionEvent e) {

		final String defaultDir = PreferencesManager.getLastUsedDir(); 
		if(defaultDir!=null){
			final File defDir = new File(defaultDir);
			fileChooser.setCurrentDirectory(defDir);
		}
		if(PreferencesManager.getSelectMpegFileFilter()) {
			fileChooser.setFileFilter(tsFilter);
		}else {
			fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
		}

		final int returnVal = fileChooser.showOpenDialog(contr.getFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			contr.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			final File file = fileChooser.getSelectedFile();
			PreferencesManager.setSelectMpegFileFilter(fileChooser.getFileFilter()==tsFilter);

			final TSLoader tsLoader = new TSLoader(file,contr);
			tsLoader.execute();
		}
	}
}