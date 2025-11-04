package com.example.code_zombom_app.MVC;

import java.util.ArrayList;

/**
 * A generic model class as explained in the lectures. A model can associate with MANY views. Every
 * model class should extends this class.
 *
 * @author Lecture slides
 * @version final
 * @param <V> A View that implements TView
 * @see TView
 */
public abstract class TModel<V extends TView> {
    private ArrayList<V> views;

    protected TModel() {
        views = new ArrayList<V>();
    }

    /**
     * Use this method to associate a model with a view
     *
     * @param view The view that implements TView to add
     * @see TView
     */
    public void addView(V view) {
        if (!views.contains(view))
            views.add(view);
    }

    /**
     * Use this method to disassociate a view from this model
     *
     * @param view The view to remove
     * @see TView
     */
    public void deleteView(V view) {
        views.remove(view);
    }

    /**
     * Notify all views in the event of update
     *
     * @see TView
     */
    public void notifyViews() {
        for (V view : views)
            view.update(this);
    }
}
