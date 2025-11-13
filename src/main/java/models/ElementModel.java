package models;

import org.openqa.selenium.WebElement;

public class ElementModel {
    private static int idCounter = 1; // static counter shared across all instances

    private int id;
    private WebElement _element;

    // Getter for age
    public int getId() {
        return id;
    }

    // Setter for age
    public void setId(int id) {
        this.id = id;
    }
    // Getter for name
    public WebElement getElement() {
        return _element;
    }

    // Setter for name
    public void setElement(WebElement element) {
        this._element = element;
    }

    public ElementModel(WebElement element) {
        this.id = idCounter++;
        this._element = element;
    }

    @Override
    public String toString() {
        try {
            String text = _element.getText().trim();
            return "ID: " + id + " | Name: "+text;
        } catch (Exception e) {
            System.out.println("⚠️ Skipping label due to error: " + e.getMessage());
        }
        return "ID: " + id + " | Name: ";
    }
}
