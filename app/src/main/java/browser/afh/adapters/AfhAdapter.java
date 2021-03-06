package browser.afh.adapters;

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
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import browser.afh.R;
import browser.afh.types.AfhFiles;

public class AfhAdapter extends ArrayAdapter<AfhFiles>
{
    private final Context context;
    public AfhAdapter(Context context, int resource,List<AfhFiles> items) {
        super(context,resource,items);
        this.context = context;
    }

    private void customTab(String Url){
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setShowTitle(true);
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(context, Uri.parse(Url));
    }
    @NonNull
    @Override
    public View getView(int position, final View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if(v == null)
            v= LayoutInflater.from(getContext()).inflate(R.layout.afh_items, null);
        final AfhFiles p = getItem(position);
        if(p != null) {
            TextView name = (TextView) v.findViewById(R.id.rname);
            name.setText(p.name);
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    try {
                      customTab(p.url);
                    } catch (ActivityNotFoundException exc){
                    new MaterialDialog.Builder(context)
                            .title(R.string.no_browser_dialog_title)
                            .content(R.string.no_browser_dialog_content)
                            .neutralText(R.string.no_browser_dialog_assert)
                            .onNeutral(new MaterialDialog.SingleButtonCallback() {
                               @Override
                               public void onClick(@NonNull MaterialDialog dialog,@NonNull DialogAction which) {
                                   dialog.dismiss();
                               }

                            })
                            .show();
                    }
                    return false;
                }
            });
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MaterialDialog.Builder(context)
                            .title(p.name)
                            .content(String.format(context.getString(R.string.file_dialog_content), p.file_size, p.upload_date, p.screenname, p.downloads))
                            .positiveText(R.string.file_dialog_positive_button_label)
                            .neutralText(R.string.file_dialog_neutral_button_label)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    try {
                                        customTab(p.url);
                                    } catch (ActivityNotFoundException exc){
                                        new MaterialDialog.Builder(context)
                                                .title(R.string.no_browser_dialog_title)
                                                .content(R.string.no_browser_dialog_content)
                                                .neutralText(R.string.no_browser_dialog_assert)
                                                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(@NonNull MaterialDialog dialog,@NonNull DialogAction which) {
                                                        dialog.dismiss();
                                                    }

                                                })
                                                .show();
                                    }
                                    dialog.dismiss();
                                }
                            })
                            .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
            });
        }
        return v;
    }
}
