package com.uzitech.hyperioncontrol.Support;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.uzitech.hyperioncontrol.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PriorityListAdapter extends RecyclerView.Adapter<PriorityListAdapter.MyViewHolder> {

    Activity activity;
    JSONArray priorities;

    onButtonClick buttonClick;

    public PriorityListAdapter(Activity activity, JSONArray priorities, onButtonClick buttonClick) {
        this.activity = activity;
        this.priorities = priorities;

        this.buttonClick = buttonClick;
    }

    public void setItem(JSONArray priorities) {
        this.priorities = priorities;
    }

    @NotNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.source_layout, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NotNull MyViewHolder holder, int position) {
        try {
            JSONObject object = priorities.getJSONObject(position);

            holder.origin.setText(object.getString("origin").split("@::")[0]);
            holder.origin_addr.setText(object.getString("origin").split("@::")[1]);
            holder.type.setText(object.getString("componentId"));
            holder.priority.setText(String.valueOf(object.getInt("priority")));

            if (object.getBoolean("visible")) {
                holder.action.setBackgroundColor(activity.getResources().getColor(android.R.color.holo_red_light, null));
                holder.action.setText(activity.getString(R.string.stop));
                holder.action.setOnClickListener(v -> {
                    try {
                        buttonClick.stopSource(object.getInt("priority"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                holder.action.setBackgroundColor(activity.getResources().getColor(android.R.color.holo_green_light, null));
                holder.action.setText(activity.getString(R.string.select));
                holder.action.setOnClickListener(v -> {
                    try {
                        buttonClick.selectSource(object.getInt("priority"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return priorities.length();
    }

    public interface onButtonClick {
        void stopSource(int priority);

        void selectSource(int priority);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView origin, origin_addr, type, priority;
        Button action;

        public MyViewHolder(View view) {
            super(view);
            origin = view.findViewById(R.id.origin);
            origin_addr = view.findViewById(R.id.origin_addr);
            type = view.findViewById(R.id.type);
            priority = view.findViewById(R.id.priority);
            action = view.findViewById(R.id.action);
        }
    }
}
