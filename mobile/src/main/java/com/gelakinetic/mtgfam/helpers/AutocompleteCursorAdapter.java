/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers;

import android.app.SearchManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.SimpleCursorAdapter;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.CardSearchProvider;

import java.util.concurrent.RejectedExecutionException;

/**
 * This cursor adapter provides suggestions for card names directly from the database
 */
public class AutocompleteCursorAdapter extends SimpleCursorAdapter implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String[] CARD_NAME_PROJECTION = new String[]{
            CardDbAdapter.KEY_ID,
            CardDbAdapter.KEY_NAME,
    };
    private static final Uri SEARCH_URI =
            Uri.parse("content://" + CardSearchProvider.AUTHORITY + "/" + SearchManager.SUGGEST_URI_PATH_QUERY);
    private final String[] mAutocompleteFilter = new String[1];

    private final FamiliarFragment mFragment;

    /**
     * Standard constructor.
     *
     * @param context  The context where the ListView associated with this SimpleListItemFactory is running
     * @param from     A list of column names representing the data to bind to the UI. Can be null if the cursor is not
     *                 available yet.
     * @param to       The views that should display column in the "from" parameter. These should all be TextViews. The
     *                 first N views in this list are given the values of the first N columns in the from parameter.
     *                 Can be null if the cursor is not available yet.
     * @param textView The text view which we are watching for changes
     */
    public AutocompleteCursorAdapter(FamiliarFragment context, String[] from, int[] to, AutoCompleteTextView textView, boolean showArrowhead) {
        super(context.getActivity(), showArrowhead ? R.layout.list_item_1_arrowhead : R.layout.list_item_1, null, from, to, 0);
        mFragment = context;
        LoaderManager.getInstance(mFragment).initLoader(0, null, this);
        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                /* Don't care */
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                /* Don't care */
            }

            @Override
            public void afterTextChanged(Editable editable) {
                /* Preform a query */
                mAutocompleteFilter[0] = String.valueOf(editable);
                try {
                    LoaderManager.getInstance(mFragment).restartLoader(0, null, AutocompleteCursorAdapter.this);
                } catch (RejectedExecutionException e) {
                    // Autocomplete broke, but at least it won't take down the whole app
                }
            }
        });
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /* Now create and return a CursorLoader that will take care of creating a Cursor for the data being displayed.
         */
        String select = "(" + CardDbAdapter.KEY_NAME + ")";
        return new CursorLoader(mFragment.requireActivity(), SEARCH_URI, CARD_NAME_PROJECTION, select, mAutocompleteFilter,
                CardDbAdapter.KEY_NAME + " COLLATE LOCALIZED ASC");
    }

    /**
     * Called when a previously created loader has finished its load.
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        try {
            Cursor old;
            if (data != null && data.isClosed()) {
                old = this.swapCursor(null);

            } else {
                old = this.swapCursor(data);
            }
            if (old != null) {
                old.close();
            }
        } catch (NullPointerException e) {
            /* eat it */
        }
    }

    /**
     * Called when a previously created loader is being reset, and thus making its data unavailable.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        this.swapCursor(null);
    }

    /**
     * Converts the cursor into a CharSequence.
     *
     * @param cursor the cursor to convert to a CharSequence
     * @return a CharSequence representing the value
     */
    @Override
    public CharSequence convertToString(@NonNull Cursor cursor) {
        try {
            return cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_NAME));
        } catch (Exception e) {
            /* If there is any problem, return the empty string */
            return "";
        }
    }

    /**
     * Get the data item associated with the specified position in the data set.
     * If there is an exception, safely abandon the cursor
     *
     * @param position Position of the item whose data we want within the adapter's data set.
     * @return The data at the position, or null
     */
    @Override
    public Object getItem(int position) {
        try {
            return super.getItem(position);
        } catch (Exception e) {
            this.swapCursor(null);
            return null;
        }
    }

    /**
     * Get the row id associated with the specified position in the list.
     * If there is an exception, safely abandon the cursor
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position) {
        try {
            return super.getItemId(position);
        } catch (Exception e) {
            this.swapCursor(null);
            return 0;
        }
    }

    /**
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * inflate(int, android.view.ViewGroup, boolean) to specify a root view and to prevent
     * attachment to the root.
     * If there is an exception, safely abandon the cursor
     *
     * @param position    The position of the item within the adapter's data set of the item whose
     *                    view we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *                    is non-null and of an appropriate type before using. If it is not possible
     *                    to convert this view to display the correct data, this method can create a
     *                    new view. Heterogeneous lists can specify their number of view types, so
     *                    that this View is always of the right type (see getViewTypeCount() and
     *                    getItemViewType(int)).
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        try {
            return super.getView(position, convertView, parent);
        } catch (Exception e) {
            /* Now that prior failures swap in a null cursor, this should never be called */
            this.swapCursor(null);
            if (convertView != null) {
                return convertView;
            } else {
                return new View(mFragment.getActivity());
            }
        }
    }
}
