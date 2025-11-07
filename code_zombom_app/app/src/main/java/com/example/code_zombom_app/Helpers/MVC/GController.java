package com.example.code_zombom_app.Helpers.MVC;

import android.content.Context;
import android.widget.EditText;

/**
 * A general controller for the project
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/5/2025
 */
public abstract class GController<M extends TModel> extends TController {
    public GController(M model) {
        super(model);
    }

    /**
     * Get the input from an EditText field
     *
     * @param editText The EditText field to read the input from
     * @return The string input
     * @see android.widget.EditText
     */
    protected String getInput(EditText editText) {
        return editText.getText().toString().trim();
    }
}
