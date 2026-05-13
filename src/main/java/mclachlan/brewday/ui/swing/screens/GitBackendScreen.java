package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.backends.git.GitBackend;
import mclachlan.brewday.ui.swing.app.SwingScreen;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing port of JFX {@code GitBackendPane}: enable/disable Git backend, remote URL,
 * commit/push and overwrite-from-remote, with command log.
 * Blocking {@link Database} git calls run on a {@link SwingWorker} background thread;
 * log output is marshalled onto the EDT. Enable-with-cancel does not erroneously persist
 * {@code GIT_BACKEND_ENABLED} (unfixed JFX {@code GitBackendPane} regression).
 */
public class GitBackendScreen extends JPanel implements SwingScreen
{
	private static final int LEFT_FORM_PREF_WIDTH_PX = 500;

	private boolean refreshing;
	private volatile boolean gitTaskRunning;

	private final JTextArea intro = new JTextArea();
	private final JToggleButton enableToggle = new JToggleButton();
	private final JTextField remoteUrlField = new JTextField(40);
	private final JButton commitPushButton = new JButton(getUiString("settings.git.commit.and.push"));
	private final JButton pullOverwriteButton = new JButton(getUiString("settings.git.restore.from.remote"));
	private final JTextArea commandLog = new JTextArea();

	public GitBackendScreen()
	{
		super(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel leftForm = buildLeftPanel();
		JPanel leftWrap = new JPanel(new BorderLayout(0, 0));
		leftWrap.add(leftForm, BorderLayout.NORTH);

		Dimension leftPref = leftWrap.getPreferredSize();
		Dimension cappedLeft = new Dimension(LEFT_FORM_PREF_WIDTH_PX, leftPref.height);
		leftWrap.setPreferredSize(cappedLeft);
		leftWrap.setMinimumSize(new Dimension(
			Math.min(LEFT_FORM_PREF_WIDTH_PX, leftPref.width),
			leftPref.height));

		add(leftWrap, BorderLayout.WEST);
		add(buildRightPanel(), BorderLayout.CENTER);
		refresh();
		wireActions();
	}

	private JPanel buildLeftPanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(new EmptyBorder(4, 4, 8, 8));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		intro.setEditable(false);
		intro.setOpaque(false);
		intro.setLineWrap(true);
		intro.setWrapStyleWord(true);
		intro.setText(getUiString("settings.git.intro"));
		intro.setColumns(54);
		intro.setBorder(BorderFactory.createEmptyBorder());
		panel.add(intro, gbc);

		gbc.gridy++;
		panel.add(enableToggle, gbc);

		gbc.gridy++;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		panel.add(new JLabel(getUiString("settings.git.remote.url")), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		remoteUrlField.setColumns(28);
		panel.add(remoteUrlField, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		row.add(commitPushButton);
		panel.add(row, gbc);

		gbc.gridy++;
		row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		row.add(pullOverwriteButton);
		panel.add(row, gbc);

		return panel;
	}

	private JPanel buildRightPanel()
	{
		JPanel panel = new JPanel(new BorderLayout(4, 4));
		panel.add(new JLabel(getUiString("settings.git.command.log")), BorderLayout.NORTH);
		commandLog.setEditable(false);
		commandLog.setRows(8);
		commandLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, commandLog.getFont().getSize()));
		JScrollPane scroll = new JScrollPane(commandLog);
		panel.add(scroll, BorderLayout.CENTER);
		return panel;
	}

	private Window windowAncestor()
	{
		return SwingUtilities.getWindowAncestor(this);
	}

	private GitBackend.OutputCollector logCollector()
	{
		return s ->
		{
			EventQueue.invokeLater(() -> commandLog.append(s));
		};
	}

	private void setGitTaskRunning(boolean running)
	{
		this.gitTaskRunning = running;
		updateControlsEnabledState();
	}

	private void updateControlsEnabledState()
	{
		boolean busy = gitTaskRunning;
		boolean idle = !busy;
		enableToggle.setEnabled(idle && !refreshing);
		remoteUrlField.setEnabled(idle && !refreshing);
		String enabledStr = Database.getInstance().getSettings().get(Settings.GIT_BACKEND_ENABLED);
		boolean gitOn = idle && "true".equalsIgnoreCase(enabledStr);
		commitPushButton.setEnabled(gitOn);
		pullOverwriteButton.setEnabled(gitOn);
	}

	private void runGitBackendTask(Runnable backgroundWork)
	{
		if (gitTaskRunning)
		{
			return;
		}
		setGitTaskRunning(true);
		SwingWorker<Void, Void> worker = new SwingWorker<>()
		{
			@Override
			protected Void doInBackground()
			{
				try
				{
					backgroundWork.run();
				}
				catch (Exception ex)
				{
					throw new RuntimeException(ex);
				}
				return null;
			}

			@Override
			protected void done()
			{
				setGitTaskRunning(false);
				try
				{
					get();
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					System.out.println("[Swing Git UI] task interrupted");
				}
				catch (ExecutionException e)
				{
					Throwable c = e.getCause();
					if (c instanceof BrewdayException brewdayException)
					{
						appendError(brewdayException);
					}
					else if (c instanceof RuntimeException re && re.getCause() != null)
					{
						appendError(re.getCause());
					}
					else
					{
						appendError(c != null ? c : e);
					}
				}
				refresh();
			}
		};
		worker.execute();
	}

	private void appendError(Throwable t)
	{
		if (t == null)
		{
			return;
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.flush();
		String full = sw.toString();
		EventQueue.invokeLater(() ->
		{
			commandLog.append(full);
			if (!full.endsWith("\n"))
			{
				commandLog.append("\n");
			}
		});
	}

	private void wireActions()
	{
		enableToggle.addActionListener(e ->
		{
			if (refreshing)
			{
				return;
			}

			boolean turnOn = enableToggle.isSelected();
			if (turnOn)
			{
				String trimmedRemote = remoteUrlField.getText() != null
					? remoteUrlField.getText().trim() : "";
				boolean isRemote = !trimmedRemote.isEmpty();

				String dialogBody = isRemote
					? getUiString("settings.git.enable.dialog.text.remote")
					: getUiString("settings.git.enable.dialog.text");

				int opt = JOptionPane.showConfirmDialog(
					windowAncestor(),
					dialogBody,
					getUiString("settings.git.enable.dialog.title"),
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE);

				if (opt != JOptionPane.OK_OPTION)
				{
					refreshing = true;
					enableToggle.setSelected(false);
					enableToggle.setText(getUiString("settings.git.enable"));
					refreshing = false;
					updateControlsEnabledState();
					return;
				}

				Settings settings = Database.getInstance().getSettings();
				if (isRemote)
				{
					settings.set(Settings.GIT_REMOTE_REPO, trimmedRemote);
				}

				runGitBackendTask(() -> Database.getInstance().enableGitBackend(logCollector()));
				enableToggle.setText(getUiString("settings.git.disable"));
			}
			else
			{
				int opt = JOptionPane.showConfirmDialog(
					windowAncestor(),
					getUiString("settings.git.disable.dialog.text"),
					getUiString("settings.git.disable.dialog.title"),
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE);

				if (opt != JOptionPane.OK_OPTION)
				{
					refreshing = true;
					enableToggle.setSelected(true);
					enableToggle.setText(getUiString("settings.git.disable"));
					refreshing = false;
					updateControlsEnabledState();
					return;
				}

				runGitBackendTask(() -> Database.getInstance().disableGitBackend(logCollector()));
				enableToggle.setText(getUiString("settings.git.enable"));
			}
		});

		commitPushButton.addActionListener(e ->
			runGitBackendTask(() -> Database.getInstance().syncToGitBackend(logCollector())));

		pullOverwriteButton.addActionListener(e ->
			runGitBackendTask(() -> Database.getInstance().syncFromGitBackend(logCollector())));
	}

	@Override
	public void refresh()
	{
		refreshing = true;
		try
		{
			if (!gitTaskRunning)
			{
				Settings settings = Database.getInstance().getSettings();

				boolean enabled = "true".equalsIgnoreCase(settings.get(Settings.GIT_BACKEND_ENABLED));

				enableToggle.setSelected(enabled);
				enableToggle.setText(enabled
					? getUiString("settings.git.disable")
					: getUiString("settings.git.enable"));

				String repo = settings.get(Settings.GIT_REMOTE_REPO);
				remoteUrlField.setText(repo != null ? repo : "");
			}
		}
		finally
		{
			refreshing = false;
		}
		updateControlsEnabledState();
	}
}
