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

package nl.digitalekabeltelevisie.gui;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import nl.digitalekabeltelevisie.data.mpeg.TransportStream;
import nl.digitalekabeltelevisie.main.DVBinspector;

public class FileOpenAction extends AbstractAction {

	private static final Logger logger = Logger.getLogger(FileOpenAction.class.getName());

	private static final String DIR = "stream_directory";

	private JFileChooser	fileChooser;
	private JFrame			frame;
	private DVBinspector	contr;

	public FileOpenAction(final JFileChooser jf, final JFrame fr, final DVBinspector controller) {
		super();
		fileChooser = jf;
		frame = fr;
		contr = controller;
	}

	public void actionPerformed(final ActionEvent e) {

		final Preferences prefs = Preferences.userNodeForPackage(FileOpenAction.class);

		final String defaultDir = prefs.get(DIR, null);
		if(defaultDir!=null){
			final File defDir = new File(defaultDir);
			fileChooser.setCurrentDirectory(defDir);
		}

		final int returnVal = fileChooser.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			contr.setTransportStream(null); // clean up old before creating new, might save some memory
			final File f = fileChooser.getSelectedFile();
			prefs.put(DIR,f.getParent());
			final TransportStream transportStream = new TransportStream(f);
			transportStream.setDefaultPrivateDataSpecifier(contr.getDefaultPrivateDataSpecifier());

			try {
				transportStream.parseStream();
				contr.setTransportStream(transportStream);
			} catch (final Throwable t) {
				logger.log(Level.WARNING, "error parsing transport stream",t);
				frame.setCursor(Cursor.getDefaultCursor());
				String message = "Error parsing stream: "+((t.getMessage()!=null)?(t.getMessage()):t.getClass().getName());
				JOptionPane.showMessageDialog(frame,
						message,
						"Error DVB Inspector",
						JOptionPane.ERROR_MESSAGE);
			}


			frame.setCursor(Cursor.getDefaultCursor());
		}
	}
}