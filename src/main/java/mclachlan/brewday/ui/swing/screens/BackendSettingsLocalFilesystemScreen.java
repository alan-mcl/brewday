package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import mclachlan.brewday.ui.swing.app.SwingScreen;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Parity placeholder for JavaFX Backend Local File System card (literal "coming soonish" via
 * {@code settings.backend.coming.soonish}).
 */
public class BackendSettingsLocalFilesystemScreen extends JPanel implements SwingScreen
{
	public BackendSettingsLocalFilesystemScreen()
	{
		super(new BorderLayout());
		JLabel msg = new JLabel(getUiString("settings.backend.coming.soonish"), SwingConstants.CENTER);
		msg.setBorder(new EmptyBorder(24, 24, 24, 24));
		add(msg, BorderLayout.CENTER);
	}
}
