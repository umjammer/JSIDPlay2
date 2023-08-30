package ui.assembly64;

import java.util.ResourceBundle;

import javafx.util.StringConverter;

public class CategoryToStringConverter<T> extends StringConverter<Category> {

	private ResourceBundle bundle;

	public CategoryToStringConverter(ResourceBundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public Category fromString(String repoString) {
		throw new RuntimeException("This function is not supported!");
	}

	@Override
	public String toString(Category subCategory) {
		return !subCategory.equals(Category.ALL) ? subCategory.getName() : bundle.getString("ALL_CONTENT");
	}

}
