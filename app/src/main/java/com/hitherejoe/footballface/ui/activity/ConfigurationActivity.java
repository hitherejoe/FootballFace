package com.hitherejoe.footballface.ui.activity;

import android.app.Activity;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.view.WindowInsets;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;
import com.hitherejoe.footballface.R;
import com.hitherejoe.footballface.data.model.Team;
import com.hitherejoe.footballface.ui.adapter.TeamAdapter;
import com.hitherejoe.footballface.util.WatchFaceUtil;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ConfigurationActivity extends Activity implements
        WearableListView.ClickListener, WearableListView.OnScrollListener {

    private GoogleApiClient mGoogleApiClient;

    @Bind(R.id.content)
    BoxInsetLayout mLayoutContent;

    @Bind(R.id.text_header)
    TextView mTeamsHeader;

    @Bind(R.id.list_teams)
    WearableListView mTeamsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        ButterKnife.bind(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        mLayoutContent.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if (!insets.isRound()) {
                    v.setPaddingRelative(
                            getResources().getDimensionPixelSize(R.dimen.content_padding_start),
                            v.getPaddingTop(),
                            v.getPaddingEnd(),
                            v.getPaddingBottom());
                }
                return v.onApplyWindowInsets(insets);
            }
        });

        setupTeamList();
    }

    private void setupTeamList() {
        mTeamsList.setHasFixedSize(true);
        mTeamsList.setClickListener(this);
        mTeamsList.addOnScrollListener(this);

        String[] teamNames = getResources().getStringArray(R.array.array_teams);
        TypedArray icons = getResources().obtainTypedArray(R.array.array_logos);
        TypedArray smallIcons = getResources().obtainTypedArray(R.array.array_logos_small);
        TypedArray colors = getResources().obtainTypedArray(R.array.array_colors);

        final Team[] teams = new Team[teamNames.length];
        for (int i = 0; i < teamNames.length; i++) {
            String name = teamNames[i];
            int resource = icons.getResourceId(i, 0);
            int resourceSmall = smallIcons.getResourceId(i, 0);
            int color = colors.getResourceId(i, 0);
            teams[i] = new Team(name, resource, resourceSmall, color);
        }
        mTeamsList.setAdapter(new TeamAdapter(teams));
        icons.recycle();
        smallIcons.recycle();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        TeamAdapter.TeamItemViewHolder teamItemViewHolder =
                (TeamAdapter.TeamItemViewHolder) viewHolder;
        updateConfigDataItem(teamItemViewHolder.teamItem.getTeam());
        finish();
    }

    @Override
    public void onTopEmptyRegionClick() {}

    @Override
    public void onScroll(int scroll) {}

    @Override
    public void onAbsoluteScrollChange(int scroll) {
        float newTranslation = Math.min(-scroll, 0);
        mTeamsHeader.setTranslationY(newTranslation);
    }

    @Override
    public void onScrollStateChanged(int scrollState) {}

    @Override
    public void onCentralPositionChanged(int centralPosition) {}

    private void updateConfigDataItem(final Team team) {
        DataMap configKeysToOverwrite = new DataMap();
        configKeysToOverwrite.putString(WatchFaceUtil.KEY_TEAM_NAME, team.name);
        configKeysToOverwrite.putInt(WatchFaceUtil.KEY_TEAM_COLOR, team.color);
        configKeysToOverwrite.putInt(WatchFaceUtil.KEY_TEAM_LOGO, team.logoResource);
        WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
    }

}
