package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.BatchSparge;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.MashInfusion;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.ui.swing.widgets.SwingWaterBuilderPanel;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing port of JFX {@code WaterBuilderDialog}.
 */
public class SwingWaterBuilderDialog extends JDialog
{
	private final ProcessStep step;
	private final SwingWaterBuilderPanel panel;
	private boolean output;

	public SwingWaterBuilderDialog(Window parent, ProcessStep step)
	{
		super(parent, getUiString("tools.water.builder"), Dialog.ModalityType.APPLICATION_MODAL);
		this.step = step;
		this.panel = new SwingWaterBuilderPanel(step);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		JPanel south = new JPanel();
		JButton ok = new JButton(getUiString("ui.ok"));
		JButton cancel = new JButton(getUiString("ui.cancel"));
		south.add(ok);
		south.add(cancel);
		ok.addActionListener(e ->
		{
			output = true;
			dispose();
		});
		cancel.addActionListener(e -> dispose());
		getRootPane().setDefaultButton(ok);

		setLayout(new BorderLayout());
		add(new JScrollPane(panel), BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);
		setSize(1280, 760);
		setLocationRelativeTo(parent);
	}

	public List<MiscAddition> getWaterAdditions()
	{
		List<MiscAddition> additions = panel.getAdditions();
		for (MiscAddition ma : additions)
		{
			TimeUnit time;
			if (step instanceof Mash)
			{
				time = new TimeUnit(((Mash)step).getDuration());
			}
			else if (step instanceof BatchSparge || step instanceof MashInfusion)
			{
				time = new TimeUnit(0);
			}
			else
			{
				throw new BrewdayException("invalid step type: " + step);
			}
			ma.setTime(time);
		}
		return additions;
	}

	public boolean getOutput()
	{
		return output;
	}
}
