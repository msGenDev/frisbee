/*
 * Copyright 2014 The GDG Frisbee Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gdg.frisbee.android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.gdg.frisbee.android.Const;
import org.gdg.frisbee.android.R;
import org.gdg.frisbee.android.adapter.EventAdapter;
import org.gdg.frisbee.android.api.PagedList;
import org.gdg.frisbee.android.app.App;
import org.gdg.frisbee.android.cache.ModelCache;
import org.gdg.frisbee.android.special.TaggedEvent;
import org.gdg.frisbee.android.utils.EventDateComparator;
import org.gdg.frisbee.android.utils.TaggedEventDistanceComparator;
import org.gdg.frisbee.android.utils.Utils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Comparator;

import butterknife.ButterKnife;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import retrofit.Callback;
import retrofit.RetrofitError;

public class TaggedEventFragment extends EventFragment {

    private String mCacheKey = "";
    private TaggedEvent mTaggedEvent;
    private Comparator<EventAdapter.Item> mLocationComparator = new TaggedEventDistanceComparator();
    private Comparator<EventAdapter.Item> mCurrentComparator = mLocationComparator;
    private Comparator<EventAdapter.Item> mDateComparator = new EventDateComparator();

    public static TaggedEventFragment newInstance(String cacheKey, TaggedEvent taggedEvent) {
        TaggedEventFragment frag = new TaggedEventFragment();
        Bundle args = new Bundle();
        args.putString(Const.EXTRA_TAGGED_EVENT_CACHEKEY, cacheKey);
        args.putParcelable(Const.EXTRA_TAGGED_EVENT, taggedEvent);
        frag.setArguments(args);
        return frag;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            Bundle args = getArguments();
            mCacheKey = args.getString(Const.EXTRA_TAGGED_EVENT_CACHEKEY);
            mTaggedEvent = args.getParcelable(Const.EXTRA_TAGGED_EVENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_events, container, false);
        ButterKnife.inject(this, v);
        return v;
    }

    @Override
    void fetchEvents() {
        setIsLoading(true);

        Callback<PagedList<org.gdg.frisbee.android.api.model.TaggedEvent>> listener = new Callback<PagedList<org.gdg.frisbee.android.api.model.TaggedEvent>>() {

            @Override
            public void success(final PagedList<org.gdg.frisbee.android.api.model.TaggedEvent> taggedEventPagedList, final retrofit.client.Response response) {
                mEvents.addAll(taggedEventPagedList.getItems());

                App.getInstance().getModelCache().putAsync(mCacheKey, mEvents, DateTime.now().plusHours(2), new ModelCache.CachePutListener() {
                    @Override
                    public void onPutIntoCache() {
                        mAdapter.addAll(mEvents);
                        sortEvents();
                        setIsLoading(false);
                    }
                });
            }

            @Override
            public void failure(final RetrofitError error) {

            }

        };

        if (Utils.isOnline(getActivity())) {
            mClient.getHub()
                    .getTaggedEventUpcomingList(mTaggedEvent.getTag(), DateTime.now(), listener);
        } else {
            App.getInstance().getModelCache().getAsync(mCacheKey, false, new ModelCache.CacheListener() {
                @Override
                public void onGet(Object item) {
                    ArrayList<org.gdg.frisbee.android.api.model.TaggedEvent> events = (ArrayList<org.gdg.frisbee.android.api.model.TaggedEvent>) item;

                    mAdapter.addAll(events);
                    sortEvents();
                    setIsLoading(false);
                    if (isAdded())
                        Crouton.makeText(getActivity(), getString(R.string.cached_content), Style.INFO).show();
                }

                @Override
                public void onNotFound(String key) {
                    setIsLoading(false);
                    if (isAdded())
                        Crouton.makeText(getActivity(), getString(R.string.offline_alert), Style.ALERT).show();
                }
            });
        }
    }

    private void sortEvents() {
        mAdapter.sort(mCurrentComparator);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.special_event_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mCurrentComparator == mLocationComparator) {
            menu.findItem(R.id.order_by_date).setVisible(true);
            menu.findItem(R.id.order_by_distance).setVisible(false);
        } else {
            menu.findItem(R.id.order_by_distance).setVisible(true);
            menu.findItem(R.id.order_by_date).setVisible(false);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.order_by_date) {
            mCurrentComparator = mDateComparator;
            setIsLoading(true);
            sortEvents();
            setIsLoading(false);
            getActivity().supportInvalidateOptionsMenu();
            scrollToSoonestEvent();
            return true;
        } else if (item.getItemId() == R.id.order_by_distance) {
            mCurrentComparator = mLocationComparator;
            setIsLoading(true);
            sortEvents();
            setIsLoading(false);
            getActivity().supportInvalidateOptionsMenu();
            getListView().setSelection(0);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scrollToSoonestEvent() {
        long now = System.currentTimeMillis();
        for (int i = 0; i < mAdapter.getCount(); i++) {
            if (mAdapter.getItem(i).getStart().getMillis() > now) {
                getListView().setSelection(i);
                return;
            }
        }
    }
}
