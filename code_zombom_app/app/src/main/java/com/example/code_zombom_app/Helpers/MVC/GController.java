package com.example.code_zombom_app.Helpers.MVC;

import android.content.Context;

/**
 * A general controller for the project
 *
 * @author Dang Nguyen
 * @version 1.0.0, 11/5/2025
 */
public abstract class GController<M extends TModel> extends TController {
    protected final Context context;

    public GController(M model, Context context) {
        super(model);
        this.context = context;
    }
}
