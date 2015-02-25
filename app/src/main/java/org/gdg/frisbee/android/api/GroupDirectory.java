/*
 * Copyright 2013-2015 The GDG Frisbee Project
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

package org.gdg.frisbee.android.api;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.reflect.TypeToken;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.gdg.frisbee.android.api.deserializer.ZuluDateTimeDeserializer;
import org.gdg.frisbee.android.api.model.Directory;
import org.gdg.frisbee.android.api.model.Event;
import org.gdg.frisbee.android.api.model.EventFullDetails;
import org.gdg.frisbee.android.api.model.Pulse;
import org.gdg.frisbee.android.api.model.TaggedEvent;
import org.joda.time.DateTime;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import timber.log.Timber;

/**
 * GDG Aachen
 * org.gdg.frisbee.android.api
 * <p/>
 * User: maui
 * Date: 21.04.13
 * Time: 22:08
 */
public class GroupDirectory {

    private static final String BASE_URL = "https://developers.google.com";
    private static final String DIRECTORY_URL = "https://hub.gdgx.io/api/v1/chapters?perpage=-1";
    private static final String ALL_CALENDAR_URL = BASE_URL + "/events/calendar/fc?start=1366581600&end=1367186400&_=1366664352089";
    private static final String GDL_CALENDAR_URL = BASE_URL + "/events/calendar/fc?calendar=gdl&start=1366581600&end=1367186400&_=1366664644691";
    private static final String CHAPTER_CALENDAR_URL = BASE_URL + "/events/feed/json";
    private static final String TAGGED_EVENTS_URL_UPCOMING = "https://hub.gdgx.io/api/v1/events/tag/%s/upcoming";
    private static final String EVENT_DETAIL_URL = "https://hub.gdgx.io/api/v1/events/";
    private static final String SHOWCASE_NEXT_URL = BASE_URL + "/showcase/next";
    private static final String PULSE_URL = BASE_URL + "/groups/pulse_stats/";
    private static final String COUNTRY_PULSE_URL = BASE_URL + "/groups/pulse_stats/%s/";
    private static Hub _hubInstance;

    public GroupDirectory() {
    }

    public Hub getHub() {
        if (_hubInstance == null) {
            _hubInstance = new RestAdapter.Builder()
                    .setEndpoint("https://hub.gdgx.io/api/v1")
                    .build().create(Hub.class);
        }
        return _hubInstance;
    }

    public static interface Hub {

        @GET("/chapters?perpage=-1")
        void getDirectory(Callback<Directory> callback);

        @GET("/events/{id}")
        void getEvent(@Path("id") String eventId, Callback<EventFullDetails> callback);

        @GET("/events/tag/{tag}/upcoming?perPage=1000")
        void getTaggedEventUpcomingList(@Path("tag") String tag, @Query("_") DateTime now, Callback<PagedList<TaggedEvent>> callback);

    }

    public ApiRequest getDirectory(Response.Listener<Directory> successListener, Response.ErrorListener errorListener) {
        GsonRequest<Void, Directory> dirReq = new GsonRequest<Void, Directory>(Request.Method.GET,
                DIRECTORY_URL,
                Directory.class,
                successListener,
                errorListener,
                GsonRequest.getGson(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES));
        return new ApiRequest(dirReq);
    }

    public ApiRequest getPulse(Response.Listener<Pulse> successListener, Response.ErrorListener errorListener) {
        GsonRequest<Void, Pulse> pulseReq = new GsonRequest<Void, Pulse>(Request.Method.GET,
                PULSE_URL,
                Pulse.class,
                successListener,
                errorListener,
                GsonRequest.getGson(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES));
        return new ApiRequest(pulseReq);
    }

    public ApiRequest getCountryPulse(String country, Response.Listener<Pulse> successListener, Response.ErrorListener errorListener) {
        GsonRequest<Void, Pulse> pulseReq = null;
        try {
            Timber.d(String.format(COUNTRY_PULSE_URL, URLEncoder.encode(country, "utf-8")));
            pulseReq = new GsonRequest<Void, Pulse>(Request.Method.GET,
                    String.format(COUNTRY_PULSE_URL, country.replaceAll(" ","%20")),
                    Pulse.class,
                    successListener,
                    errorListener,
                    GsonRequest.getGson(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return new ApiRequest(pulseReq);
    }

    public ApiRequest getEvent(String eventId, Response.Listener<EventFullDetails> successListener, Response.ErrorListener errorListener) {
        GsonRequest<Void, EventFullDetails> eventReq = new GsonRequest<Void, EventFullDetails>(Request.Method.GET,
                EVENT_DETAIL_URL + eventId,
                EventFullDetails.class,
                successListener,
                errorListener,
                GsonRequest.getGson(FieldNamingPolicy.IDENTITY, new ZuluDateTimeDeserializer()));
        eventReq.setRetryPolicy(new DefaultRetryPolicy(
                60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return new ApiRequest(eventReq);
    }

    public ApiRequest getChapterEventList(final DateTime start, final DateTime end, final String chapterId, Response.Listener<ArrayList<Event>> successListener, Response.ErrorListener errorListener) {

        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair() {
            @Override
            public String getName() {
                return "group";
            }

            @Override
            public String getValue() {
                return chapterId;
            }
        });

        params.add(new NameValuePair() {
            @Override
            public String getName() {
                return "start";
            }

            @Override
            public String getValue() {
                return ""+(int)(start.getMillis()/1000);
            }
        });

        if(end != null) {
            params.add(new NameValuePair() {
                @Override
                public String getName() {
                    return "end";
                }

                @Override
                public String getValue() {
                    return ""+(int)(end.getMillis()/1000);
                }
            });
        }
        params.add(new NameValuePair() {
            @Override
            public String getName() {
                return "_";
            }

            @Override
            public String getValue() {
                return ""+(int)(new DateTime().getMillis()/1000);
            }
        });
        Type type = new TypeToken<ArrayList<Event>>() {}.getType();

        String url = CHAPTER_CALENDAR_URL;
        url += "?"+URLEncodedUtils.format(params, "UTF-8");

        GsonRequest<Void, ArrayList<Event>> eventReq = new GsonRequest<Void, ArrayList<Event>>(Request.Method.GET,
                url,
                type,
                successListener,
                errorListener,
                GsonRequest.getGson(FieldNamingPolicy.IDENTITY));

        return new ApiRequest(eventReq);
    }

}
