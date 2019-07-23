package mclachlan.brewday.ui.swing;

import com.toedter.calendar.JDateChooser;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;
import javax.swing.*;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class BatchesPanel extends EditorPanel
{
	private JTabbedPane tabs;
	private JDateChooser date;
	private JComboBox<String> recipe;
	private JTextArea description;

	public BatchesPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected Container getEditControls()
	{
		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));

		tabs = new JTabbedPane();

		tabs.add(StringUtils.getUiString("batch.tab.details"), getDetailsTab());
		tabs.add(StringUtils.getUiString("batch.tab.measurements"), new JPanel());

		result.add(tabs);

		initForeignKeys();

		return result;
	}

	private Container getDetailsTab()
	{
		JPanel result = new JPanel();
		result.setLayout(new MigLayout());

		date = new JDateChooser();
		date.setPreferredSize(
			new Dimension(
				date.getPreferredSize().width+30, date.getPreferredSize().height));
		date.addMouseListener(this);

		result.add(new JLabel(StringUtils.getUiString("batch.date")), "wrap");
		result.add(date, "span");

		recipe = new JComboBox<>();
		recipe.addActionListener(this);
		result.add(new JLabel(StringUtils.getUiString("batch.recipe")));
		result.add(recipe, "wrap");

		description = new JTextArea(8, 30);
		description.setWrapStyleWord(true);
		description.setLineWrap(true);
		description.addKeyListener(this);
		result.add(new JLabel(StringUtils.getUiString("batch.description")), "wrap");
		result.add(new JScrollPane(description), "span");

		return result;
	}

	@Override
	public void refresh(String name)
	{
		refresh(Database.getInstance().getBatches().get(name));
	}

	@Override
	public void initForeignKeys()
	{
		DefaultComboBoxModel recipeModel = getRecipeComboModel();
		recipe.setModel(recipeModel);
	}

	private DefaultComboBoxModel getRecipeComboModel()
	{
		Vector vec = new Vector(Database.getInstance().getRecipes().keySet());
		vec.sort(Comparator.comparing(String::toString));
		return new DefaultComboBoxModel<>(vec);
	}

	private void refresh(Batch batch)
	{
		recipe.removeActionListener(this);
		description.removeKeyListener(this);

		description.setText(batch.getDescription());
		date.setDate(batch.getDate());
		recipe.setSelectedItem(batch.getRecipe());

		recipe.addActionListener(this);
		description.addKeyListener(this);
	}

	@Override
	public void commit(String name)
	{
		Batch current = Database.getInstance().getBatches().get(name);
		current.setDescription(description.getText());
	}

	@Override
	public Collection<String> loadData()
	{
		List<String> result = new ArrayList<>(
			Database.getInstance().getBatches().keySet());

		Collections.sort(result);

		return result;
	}

	@Override
	public void createNewItem()
	{
		NewBatchDialog dialog = new NewBatchDialog();
		dialog.setVisible(true);

		Batch newBatch = dialog.result;
		if (newBatch != null)
		{
			if (getCurrentName() != null)
			{
				commit(getCurrentName());
			}

			Database.getInstance().getBatches().put(newBatch.getName(), newBatch);

			refreshNames(newBatch.getName());
			refresh(newBatch.getName());
			SwingUi.instance.setDirty(getDirtyFlag());
		}
	}

	@Override
	public void newItem(String name)
	{
		// no op: the heavy lifting is done in createNewItem
	}

	@Override
	public void renameItem(String newName)
	{
		// not supported
	}

	@Override
	public void copyItem(String newName)
	{
		// todo
	}

	@Override
	public void deleteItem()
	{
		Database.getInstance().getBatches().remove(currentName);
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() == date)
		{
			SwingUi.instance.setDirty(getDirtyFlag());
		}
	}

	class NewBatchDialog extends JDialog implements ActionListener
	{
		private JComboBox<String> recipe;
		private JDateChooser date;
		private JButton ok, cancel;

		private Batch result;

		@Override
		protected void dialogInit()
		{
			setModal(true);

			this.recipe = new JComboBox<>();
			this.recipe.setModel(getRecipeComboModel());

			date = new JDateChooser(new Date());
			date.setPreferredSize(
				new Dimension(date.getPreferredSize().width+30, date.getPreferredSize().height));

			this.setTitle(StringUtils.getUiString("batch.new.dialog"));

			this.setLayout(new BorderLayout());

			JPanel controls = new JPanel(new MigLayout());



			controls.add(new JLabel(StringUtils.getUiString("batch.recipe")));
			controls.add(recipe, "wrap");

			controls.add(new JLabel(StringUtils.getUiString("batch.date")));
			controls.add(date, "wrap");

			this.add(controls, BorderLayout.CENTER);

			JPanel buttons = new JPanel();

			ok = new JButton(StringUtils.getUiString("ui.ok"));
			ok.addActionListener(this);

			cancel = new JButton(StringUtils.getUiString("ui.cancel"));
			cancel.addActionListener(this);

			buttons.add(ok);
			buttons.add(cancel);

			this.add(buttons, BorderLayout.SOUTH);

			pack();
			this.setLocationRelativeTo(SwingUi.instance);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() == ok)
			{
				result = Brewday.getInstance().createNewBatch(
					(String)recipe.getSelectedItem(),
					date.getDate());
				setVisible(false);
			}
			else if (e.getSource() == cancel)
			{
				result = null;
				setVisible(false);
			}
		}
	}
}
