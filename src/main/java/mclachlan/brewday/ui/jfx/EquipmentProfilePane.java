package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.converter.DoubleStringConverter;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.equipment.EquipmentProfile;
import org.tbee.javafx.scene.layout.MigPane;

import static mclachlan.brewday.StringUtils.getUiString;

/**
 *
 */
public class EquipmentProfilePane extends MigPane
{
	private final ListView<String> list;
	private final TextField mashEfficiency, mashTunVol, mashTunWeight,
		mashTunSpecificHeat, boilKettleVolume, evapouration, hopUtilisation,
		fermenterVolume, lauterLoss, trubChillerLoss;

	private final TextArea description;

	private Model<EquipmentProfile> model;
	private FormController formController;
	private ListController listController;
	private String dirtyFlag;

	public EquipmentProfilePane(String dirtyFlag)
	{
		this.setPadding(new Insets(5, 5, 5, 5));

		this.dirtyFlag = dirtyFlag;
		this.list = new ListView<>();

		list.setCellFactory(param -> new ListCell<>()
		{
			private ImageView imageView = JfxUi.getImageView(JfxUi.equipmentIcon, 24);

			@Override
			public void updateItem(String name, boolean empty)
			{
				super.updateItem(name, empty);
				if (empty)
				{
					setText(null);
					setGraphic(null);
				}
				else
				{
					setText(name);
					setGraphic(imageView);
				}
			}
		});

		GridPane form = new GridPane();
		form.setAlignment(Pos.TOP_LEFT);
		form.setHgap(5);
		form.setVgap(5);
		form.setPadding(new Insets(5, 5, 5, 5));

		mashEfficiency = getTextField();
		addRow(form, 0, mashEfficiency, "equipment.mash.efficiency");

		mashTunVol = getTextField();
		addRow(form, 1, mashTunVol, "equipment.mash.tun.volume");

		mashTunWeight = getTextField();
		addRow(form, 2, mashTunWeight, "equipment.mash.tun.weight");

		mashTunSpecificHeat = getTextField();
		addRow(form, 3, mashTunSpecificHeat, "equipment.mash.tun.specific.heat");

		boilKettleVolume = getTextField();
		addRow(form, 4, boilKettleVolume, "equipment.boil.kettle.volume");

		evapouration = getTextField();
		addRow(form, 5, evapouration, "equipment.evapouration");

		hopUtilisation = getTextField();
		addRow(form, 6, hopUtilisation, "equipment.hop.utilisation");

		fermenterVolume = getTextField();
		addRow(form, 7, fermenterVolume, "equipment.fermenter.volume");

		lauterLoss = getTextField();
		addRow(form, 8, lauterLoss, "equipment.lauter.loss");

		trubChillerLoss = getTextField();
		addRow(form, 9, trubChillerLoss, "equipment.trub.chiller.loss");

		description = new TextArea("");
		description.setWrapText(true);
		description.setCache(false);
		description.setCacheShape(false);

		addRow(form, 10, description, "equipment.description");

		this.add(list, "dock west");
		this.add(form, "dock center, gap 5");
	}

	private TextField getTextField()
	{
		TextField result = new TextField();

		result.setMaxWidth(100);
		TextFormatter textFormatter = new TextFormatter(new DoubleStringConverter(), 0d);

		result.setTextFormatter(textFormatter);

		return result;
	}

	private void addRow(GridPane form, int rowIndex, Control control, String s)
	{
		form.addRow(rowIndex, new Label(getUiString(s)), control);
	}

	public void refresh(Database db)
	{
		JfxUi.getInstance().setDetectDirty(false);

		model = new Model(db.getEquipmentProfiles(), null, dirtyFlag);
		formController = new FormController(model, this);
		listController = new ListController(list, model);

		if (model.getItems().size() > 0)
		{
			list.getSelectionModel().select(model.getItems().get(0));
		}

		JfxUi.getInstance().setDetectDirty(true);
	}

	private void refresh(V2DataObject selected)
	{
		JfxUi.getInstance().setDetectDirty(false);

		EquipmentProfile ep = (EquipmentProfile)selected;

		model.mashEfficiency.setValue("" + ep.getMashEfficiency());
		model.mashTunVol.setValue("" + ep.getMashTunVolume());
		model.mashTunWeight.setValue("" + ep.getMashTunWeight());
		model.mashTunSpecificHeat.setValue("" + ep.getMashTunSpecificHeat());
		model.boilKettleVolume.setValue("" + ep.getBoilKettleVolume());
		model.evapouration.setValue("" + ep.getBoilEvapourationRate());
		model.hopUtilisation.setValue("" + ep.getHopUtilisation());
		model.fermenterVolume.setValue("" + ep.getFermenterVolume());
		model.lauterLoss.setValue("" + ep.getLauterLoss());
		model.trubChillerLoss.setValue("" + ep.getTrubAndChillerLoss());

		model.description.setValue(ep.getDescription());

		JfxUi.getInstance().setDetectDirty(true);
	}

	/*-------------------------------------------------------------------------*/

	public static class Model<T extends V2DataObject>
	{
		private Map<String, V2DataObject> map;
		private ObjectProperty<String> current;
		private String dirtyFlag;

		private StringProperty mashEfficiency, mashTunVol, mashTunWeight,
			mashTunSpecificHeat, boilKettleVolume, evapouration, hopUtilisation,
			fermenterVolume, lauterLoss, trubChillerLoss, description;

		public Model(Map<String, V2DataObject> map, String currentSelection,
			String dirtyFlag)
		{
			this.map = map;
			this.current = new SimpleObjectProperty<>(currentSelection);
			this.dirtyFlag = dirtyFlag;

			mashEfficiency = new SimpleStringProperty();
			mashTunVol = new SimpleStringProperty();
			mashTunWeight = new SimpleStringProperty();
			mashTunSpecificHeat = new SimpleStringProperty();
			boilKettleVolume = new SimpleStringProperty();
			evapouration = new SimpleStringProperty();
			hopUtilisation = new SimpleStringProperty();
			fermenterVolume = new SimpleStringProperty();
			lauterLoss = new SimpleStringProperty();
			trubChillerLoss = new SimpleStringProperty();
			description = new SimpleStringProperty();

			addDataListeners();
		}

		public void addDataListeners()
		{
			this.mashEfficiency.addListener((observable, oldValue, newValue) -> {
				EquipmentProfile ep = (EquipmentProfile)map.get(current.getValue());
				if (ep != null)
				{
					ep.setMashEfficiency(Double.valueOf(newValue));
					JfxUi.getInstance().setDirty(dirtyFlag);
				}
			});

			this.mashTunVol.addListener((observable, oldValue, newValue) -> {
				EquipmentProfile ep = (EquipmentProfile)map.get(current.getValue());
				if (ep != null)
				{
					ep.setMashTunVolume(Double.valueOf(newValue));
					JfxUi.getInstance().setDirty(dirtyFlag);
				}
			});

			this.mashTunWeight.addListener((observable, oldValue, newValue) -> {
				EquipmentProfile ep = (EquipmentProfile)map.get(current.getValue());
				if (ep != null)
				{
					ep.setMashTunWeight(Double.valueOf(newValue));
					JfxUi.getInstance().setDirty(dirtyFlag);
				}
			});

			this.mashTunSpecificHeat.addListener((observable, oldValue, newValue) -> {
				EquipmentProfile ep = (EquipmentProfile)map.get(current.getValue());
				if (ep != null)
				{
					ep.setMashTunSpecificHeat(Double.valueOf(newValue));
					JfxUi.getInstance().setDirty(dirtyFlag);
				}
			});

			this.boilKettleVolume.addListener((observable, oldValue, newValue) -> {
				EquipmentProfile ep = (EquipmentProfile)map.get(current.getValue());
				if (ep != null)
				{
					ep.setBoilKettleVolume(Double.valueOf(newValue));
					JfxUi.getInstance().setDirty(dirtyFlag);
				}
			});

			this.evapouration.addListener((observable, oldValue, newValue) -> {
				EquipmentProfile ep = (EquipmentProfile)map.get(current.getValue());
				if (ep != null)
				{
					ep.setBoilEvapourationRate(Double.valueOf(newValue));
					JfxUi.getInstance().setDirty(dirtyFlag);
				}
			});

			this.hopUtilisation.addListener((observable, oldValue, newValue) -> {
				EquipmentProfile ep = (EquipmentProfile)map.get(current.getValue());
				if (ep != null)
				{
					ep.setHopUtilisation(Double.valueOf(newValue));
					JfxUi.getInstance().setDirty(dirtyFlag);
				}
			});

			this.fermenterVolume.addListener((observable, oldValue, newValue) -> {
				EquipmentProfile ep = (EquipmentProfile)map.get(current.getValue());
				if (ep != null)
				{
					ep.setFermenterVolume(Double.valueOf(newValue));
					JfxUi.getInstance().setDirty(dirtyFlag);
				}
			});

			this.lauterLoss.addListener((observable, oldValue, newValue) -> {
				EquipmentProfile ep = (EquipmentProfile)map.get(current.getValue());
				if (ep != null)
				{
					ep.setLauterLoss(Double.valueOf(newValue));
					JfxUi.getInstance().setDirty(dirtyFlag);
				}
			});

			this.trubChillerLoss.addListener((observable, oldValue, newValue) -> {
				EquipmentProfile ep = (EquipmentProfile)map.get(current.getValue());
				if (ep != null)
				{
					ep.setTrubAndChillerLoss(Double.valueOf(newValue));
					JfxUi.getInstance().setDirty(dirtyFlag);
				}
			});

			this.description.addListener((observable, oldValue, newValue) -> {
				EquipmentProfile ep = (EquipmentProfile)map.get(current.getValue());
				if (ep != null)
				{
					ep.setDescription(newValue);
					JfxUi.getInstance().setDirty(dirtyFlag);
				}
			});
		}

		public ObservableList<String> getItems()
		{
			ArrayList<String> keys = new ArrayList<>(map.keySet());
			keys.sort(String::compareTo);

			return FXCollections.observableList(keys);
		}

		public void setCurrent(String newSelection)
		{
			this.current.setValue(newSelection);
		}

		public Map<String, V2DataObject> getMap()
		{
			return map;
		}
	}

	/*-------------------------------------------------------------------------*/
	public static class FormController
	{
		private Model model;
		private EquipmentProfilePane form;

		public FormController(Model model, EquipmentProfilePane form)
		{
			this.model = model;
			this.form = form;

			form.mashEfficiency.textProperty().bindBidirectional(model.mashEfficiency);
			form.mashTunVol.textProperty().bindBidirectional(model.mashTunVol);
			form.mashTunWeight.textProperty().bindBidirectional(model.mashTunWeight);
			form.mashTunSpecificHeat.textProperty().bindBidirectional(model.mashTunSpecificHeat);
			form.boilKettleVolume.textProperty().bindBidirectional(model.boilKettleVolume);
			form.evapouration.textProperty().bindBidirectional(model.evapouration);
			form.hopUtilisation.textProperty().bindBidirectional(model.hopUtilisation);
			form.fermenterVolume.textProperty().bindBidirectional(model.fermenterVolume);
			form.lauterLoss.textProperty().bindBidirectional(model.lauterLoss);
			form.trubChillerLoss.textProperty().bindBidirectional(model.trubChillerLoss);
			form.description.textProperty().bindBidirectional(model.description);

			model.current.addListener((observable, oldValue, newValue) ->
			{
				V2DataObject selected = (V2DataObject)model.getMap().get(newValue);
				form.refresh(selected);
			});
		}
	}

	/*-------------------------------------------------------------------------*/
	public static class ListController
	{
		private ListView<String> list;
		private Model model;

		public ListController(ListView<String> list, Model model)
		{
			this.list = list;
			this.model = model;

			this.list.setItems(model.getItems());

			list.getSelectionModel().selectedItemProperty().addListener(
				(obs, oldSelection, newSelection) -> model.setCurrent(newSelection));
		}
	}
}
