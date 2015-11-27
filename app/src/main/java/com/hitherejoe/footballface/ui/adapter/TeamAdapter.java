package com.hitherejoe.footballface.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableListView;
import android.view.ViewGroup;

import com.hitherejoe.footballface.R;
import com.hitherejoe.footballface.data.model.Team;
import com.hitherejoe.footballface.ui.widget.TeamItem;

public class TeamAdapter extends WearableListView.Adapter {
    private Team[] mTeamsArray;
    private Context mContext;

    public TeamAdapter(Team[] colors) {
        mTeamsArray = colors;
    }

    @Override
    public TeamItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        return new TeamItemViewHolder(new TeamItem(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        TeamItemViewHolder teamItemViewHolder = (TeamItemViewHolder) holder;
        Team team = mTeamsArray[position];
        teamItemViewHolder.teamItem.setTeam(team);

        RecyclerView.LayoutParams layoutParams =
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        int colorPickerItemMargin = (int) mContext.getResources()
                .getDimension(R.dimen.team_picker_item_margin);
        if (position == 0) {
            layoutParams.setMargins(0, colorPickerItemMargin, 0, 0);
        } else if (position == mTeamsArray.length - 1) {
            layoutParams.setMargins(0, 0, 0, colorPickerItemMargin);
        } else {
            layoutParams.setMargins(0, 0, 0, 0);
        }
        teamItemViewHolder.itemView.setLayoutParams(layoutParams);
    }

    @Override
    public int getItemCount() {
        return mTeamsArray.length;
    }

    public static class TeamItemViewHolder extends WearableListView.ViewHolder {
        public final TeamItem teamItem;

        public TeamItemViewHolder(TeamItem team) {
            super(team);
            teamItem = team;
        }
    }
}