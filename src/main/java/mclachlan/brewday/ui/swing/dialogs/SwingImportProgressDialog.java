package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.Frame;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class SwingImportProgressDialog extends JDialog
{
	private final JLabel messageLabel;

	public SwingImportProgressDialog(Frame owner, String title, String message)
	{
		super(owner, title, true);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		JPanel root = new JPanel(new BorderLayout(8, 8));
		root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		messageLabel = new JLabel(message);
		JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);

		root.add(messageLabel, BorderLayout.NORTH);
		root.add(progressBar, BorderLayout.CENTER);
		setContentPane(root);
		pack();
		setResizable(false);
		setLocationRelativeTo(owner);
	}

	public void setMessage(String message)
	{
		messageLabel.setText(message);
	}
}
