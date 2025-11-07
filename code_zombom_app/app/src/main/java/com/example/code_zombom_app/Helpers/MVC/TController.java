package com.example.code_zombom_app.Helpers.MVC;

/**
 * A generic controller template. A controller should only be associated with A view and A model, but
 * a view and a model can have MANY controllers.
 *
 * @author The Internet
 * @version 1.0.0, 11/4/2025
 * @param <M> The model
 */
public abstract class TController<M extends TModel> {
    protected final M model;

    protected TController(M model) {
        this.model = model;
    }
}
