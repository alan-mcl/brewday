package mclachlan.brewday.ui.jfx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import mclachlan.brewday.db.v2.V2DataObject;

/**
 *
 */
class DirtyList<T extends V2DataObject> extends ListView<Label>
{
	private final Map<T, Label> nodes = new HashMap<>();
	private final Map<Label, T> values = new HashMap<>();

	public void add(T t, Image graphic)
	{
		Label label = new DirtyLabel<>(t, t.getName(), JfxUi.getImageView(graphic, 24));
		nodes.put(t, label);
		values.put(label, t);
		super.getItems().add(label);
	}

	public void addAll(List<T> ts, Image graphic)
	{
		for (T t : ts)
		{
			add(t, graphic);
		}
	}

	public T getValue(Label label)
	{
		return values.get(label);
	}

	public void removeAll()
	{
		this.getChildren().clear();
	}

	public void select(T t)
	{
		getSelectionModel().select(nodes.get(t));
	}

	public void setDirty(T t)
	{
		Label label = nodes.get(t);
		label.setStyle("-fx-font-weight: bold;");
	}

	public void clearAllDirty()
	{
		for (Label l : nodes.values())
		{
			l.setStyle("");
		}
	}

	private static class DirtyLabel<T> extends Label
	{
		private T t;

		public DirtyLabel(T t, String text, Node graphic)
		{
			super(text, graphic);
			this.t = t;
		}
	}

}
