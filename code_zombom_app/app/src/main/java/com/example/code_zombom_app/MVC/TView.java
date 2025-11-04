package com.example.code_zombom_app.MVC;

/**
 * A generic view as shown in lectures. A view should be associated with a SINGLE model. Every view
 * should implement this interface
 *
 * @author Lecture Slides
 * @version Final
 * @param <M> The type of mode;
 * @see TModel
 */
public interface TView<M> {

    /**
     * Determines how a view is updated after a call from the model.
     *
     * @param model The model that is associated with this view
     * @see TModel
     */
    public void update(M model);
}
