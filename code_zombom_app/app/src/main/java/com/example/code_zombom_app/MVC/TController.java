package com.example.code_zombom_app.MVC;

/**
 * A generic controller template. A controller should only be associated with A view and A model, but
 * a view and a model can have MANY controllers.
 *
 * @author The Internet
 * @version 1.0.0, 11/4/2025
 * @param <M>
 * @param <V>
 */
public abstract class TController<M extends TModel<V>, V extends TView<M>> {
    protected final M model;
    protected  final V view;

    protected TController(M model, V view) {
        this.model = model;
        this.view = view;
        model.addView(view);
    }
}
