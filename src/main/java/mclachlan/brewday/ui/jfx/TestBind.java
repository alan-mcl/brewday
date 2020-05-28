package mclachlan.brewday.ui.jfx;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 */
public class TestBind
{
	public static void main(String[] args) throws Exception
	{
		StringProperty p1 = new SimpleStringProperty();
		StringProperty p2 = new SimpleStringProperty();

		System.out.println("p2.get() = [" + p2.get() + "]");

		p1.bindBidirectional(p2);

		p1.set("2");


		System.out.println("p2.get() = [" + p2.get() + "]");
	}
}
