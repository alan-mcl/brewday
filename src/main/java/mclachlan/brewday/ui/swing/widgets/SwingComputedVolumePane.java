package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code ComputedVolumePane}: shows an output volume name and {@link Volume#describe()}.
 */
public class SwingComputedVolumePane extends JPanel
{
	private final JLabel nameLabel;
	private final JLabel bodyLabel;

	public SwingComputedVolumePane(String title)
	{
		super(new BorderLayout(4, 4));
		setBorder(BorderFactory.createTitledBorder(title));
		nameLabel = new JLabel();
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
		bodyLabel = new JLabel();
		bodyLabel.setVerticalAlignment(JLabel.TOP);
		add(nameLabel, BorderLayout.NORTH);
		add(bodyLabel, BorderLayout.CENTER);
	}

	public void refresh(String volName, Recipe recipe)
	{
		if (volName == null || !recipe.getVolumes().contains(volName))
		{
			nameLabel.setText(getUiString("volumes.error"));
			String msg = volName == null
				? ""
				: getUiString("volumes.volume.does.not.exist", volName);
			bodyLabel.setText("<html><body style='width:240px'>" + escapeHtml(msg).replace("\n", "<br/>") + "</body></html>");
			return;
		}
		Volume volume = recipe.getVolumes().getVolume(volName);
		nameLabel.setText(volName);
		String desc = volume.describe();
		bodyLabel.setText("<html><body style='width:240px'>" + escapeHtml(desc).replace("\n", "<br/>") + "</body></html>");
	}

	private static String escapeHtml(String s)
	{
		if (s == null)
		{
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/*-------------------------------------------------------------------------*/
	/** Package-local for tests. */

	JLabel getNameLabelForTest()
	{
		return nameLabel;
	}

	JLabel getBodyLabelForTest()
	{
		return bodyLabel;
	}
}
