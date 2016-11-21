package browser.afh.data;

/*
 * Copyright (C) 2016 Ritayan Chakraborty (out386) and Harsh Shandilya (MSF-Jarvis)
 */
/*
 * This file is part of AFH Browser.
 *
 * AFH Browser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AFH Browser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AFH Browser. If not, see <http://www.gnu.org/licenses/>.
 */

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.baoyz.widget.PullRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import browser.afh.R;
import browser.afh.adapters.AfhAdapter;
import browser.afh.tools.Comparators;
import browser.afh.tools.Constants;
import browser.afh.types.AfhFiles;

public class FindFiles {
    private final TextView mTextView;
    private String json = "";
    private final ScrollView sv;
    private List<AfhFiles> filesD = new ArrayList<>();
    private AfhAdapter adapter;
    private final PullRefreshLayout pullRefreshLayout;
    private String savedID;
    private boolean sortByDate;
    private final RequestQueue queue;
    private final Context context;
    private final String TAG = Constants.TAG;
    private SimpleDateFormat sdf;

    public FindFiles(View rootView, RequestQueue queue) {
        queue.start();
        this.queue = queue;
        context = rootView.getContext();

        sdf = new SimpleDateFormat("yyyy/MM/dd \nHH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());

        mTextView = (TextView) rootView.findViewById(R.id.tv);
        sv = (ScrollView) rootView.findViewById(R.id.tvSv);
        ListView fileList = (ListView) rootView.findViewById(R.id.list);
        CheckBox sortCB = (CheckBox) rootView.findViewById(R.id.sortCB);


        sortCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    sortByDate = true;
                    Collections.sort(filesD,Comparators.byUploadDate);
                } else {
                    sortByDate = false;
                    Collections.sort(filesD,Comparators.byFileName);
                }
                adapter.notifyDataSetChanged();
            }
        });
        pullRefreshLayout = (PullRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);
        pullRefreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                filesD = new ArrayList<>();
                start(savedID);
            }
        });
        adapter = new AfhAdapter(context, R.layout.afh_items, filesD);
        fileList.setAdapter(adapter);
    }

    public void start(final String did) {
        savedID = did;
        String url = String.format(Constants.DID, did);
        Log.i(TAG, "start: DID: " + did);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "onResponse: " + response);
                        json = response;
                        List<String> fid = null;
                        try {
                            sv.setVisibility(View.GONE);
                            fid = parse();
                        } catch (Exception e) {
                            Log.i(TAG, "onResponse: ERRORS! " + e.toString());
                            pullRefreshLayout.setRefreshing(false);
                            sv.setVisibility(View.VISIBLE);
                            mTextView.setText(String.format(context.getString(R.string.json_parse_error),  e.toString()));
                        }
                        if(fid != null && fid.size() > 0) {
                            Log.i(TAG, "onResponse: NOT NULL : " + fid.get(0));
                            sv.setVisibility(View.GONE);
                            queryDirs(fid);
                        }
                        else Log.i(TAG, "onResponse: Fid null");

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.toString().contains("NoConnectionError")) {
                    Log.i(TAG, "onErrorResponse: No connection");
                    pullRefreshLayout.setRefreshing(false);
                    return;
                }
                sv.setVisibility(View.VISIBLE);
                mTextView.setText(error.toString());
                start(did);
            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(274000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }

    private void queryDirs(List<String> did) {

        for (String url : did) {
            Log.i(TAG, "queryDirs: did : " + did);
            final String link = url;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.i(TAG, "onResponseDirs: " + response);
                            try {
                                parseFiles(response);
                            } catch (Exception e) {
                                pullRefreshLayout.setRefreshing(false);
                                sv.setVisibility(View.VISIBLE);
                                mTextView.setText(String.format("%s\n\n\n%s%s %s", mTextView.getText().toString(), context.getString(R.string.json_parse_error), link, e.toString()));
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    pullRefreshLayout.setRefreshing(false);
                    if (error.toString().contains("NoConnectionError"))
                        return;
                    sv.setVisibility(View.VISIBLE);
                    mTextView.setText(String.format("%s\n\n\n%s  :   %s", mTextView.getText().toString(), link, error.toString()));
                }
            });

            stringRequest.setRetryPolicy(new DefaultRetryPolicy(274000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(stringRequest);

        }

    }

    private List<String> parse() throws Exception {
        JSONObject afhJson;
        afhJson = new JSONObject(json);
        mTextView.setText("");
        List<String> fid = new ArrayList<>();
        JSONArray data = afhJson.getJSONArray("DATA");
        for (int i = 0; i < data.length(); i++) {
            Log.i(TAG, "parse: " + data.getJSONObject(i).getString(context.getString(R.string.flid_key)));
            fid.add(String.format(Constants.FLID, data.getJSONObject(i).getString(context.getString(R.string.flid_key))));
        }
        if (fid.size() == 0) {
            sv.setVisibility(View.VISIBLE);
            mTextView.setText(R.string.error_no_files_found);
        }
        // The first list of available files is here
        pullRefreshLayout.setRefreshing(false);
        return fid;
    }

    private void print() {
        if(sortByDate) {
            Collections.sort(filesD, Comparators.byUploadDate);
        } else {
            Collections.sort(filesD, Comparators.byFileName);
        }
        Log.i(TAG, "New Files: Data changed : " + filesD.size() + " items");
        adapter.notifyDataSetChanged();

    }

    private void parseFiles(String Json) throws Exception {
        JSONObject fileJson = new JSONObject(Json);

        JSONObject data;
        if(fileJson.isNull("DATA"))
            return;

        // Data will be an Object, but if it is empty, it'll be an array
        Object dataObj = fileJson.get("DATA");
        if(! (dataObj instanceof JSONObject))
            return;
        data = (JSONObject) dataObj;

        JSONArray files = null;
        if(! data.isNull("files"))
            files = data.getJSONArray("files");
        if(files != null) {
            for (int i = 0; i < files.length(); i++) {
                JSONObject file = files.getJSONObject(i);
                String name = file.getString("name");
                String url = file.getString("url");
                String upload_date = file.getString("upload_date");
                String file_size = file.getString("file_size");
                String hDate = sdf.format(new Date(Integer.parseInt(upload_date) * 1000L));
                filesD.add(new AfhFiles(name, url, file_size, hDate));
            }
        }
        JSONArray folders = null;
        if(! data.isNull("folders"))
            folders = data.getJSONArray("folders");

        if(folders != null) {
            List<String> foldersD = new ArrayList<>();
            for (int i = 0; i < folders.length(); i++) {
                foldersD.add("https://www.androidfilehost.com/api/?action=folder&flid=" + folders.getJSONObject(i).getString("flid"));
            }
            if(foldersD.size() > 0)
                queryDirs(foldersD);
        }
        print();
    }

}
