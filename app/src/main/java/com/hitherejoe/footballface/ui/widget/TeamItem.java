package com.hitherejoe.footballface.ui.widget;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hitherejoe.footballface.R;
import com.hitherejoe.footballface.data.model.Team;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TeamItem extends LinearLayout implements WearableListView.OnCenterProximityListener {

    private static final String PROPERTY_ALPHA = "alpha";
    private static final float ALPHA_UNSELECTED_ITEM = .5f;
    private static final float ALPHA_SELECTED_ITEM = 1f;
    private static final int DURATION_ANIMATION = 150;

    @Bind(R.id.text_name)
    TextView mTeamNameText;

    @Bind(R.id.image_icon)
    ImageView mTeamIconImage;

    private final ObjectAnimator mIconSelectedAnimator;
    private final ObjectAnimator mLabelSelectedAnimator;
    private final AnimatorSet mSelectedItemAnimatorSet;

    private final ObjectAnimator mIconDeselectedAnimator;
    private final ObjectAnimator mLabelDeselectedAnimator;
    private final AnimatorSet mDeselectedItemAnimatorSet;

    private Team mTeam;

    public TeamItem(Context context) {
        super(context);
        View.inflate(context, R.layout.item_team, this);
        ButterKnife.bind(this);

        mIconDeselectedAnimator = ObjectAnimator.ofFloat(mTeamIconImage, PROPERTY_ALPHA,
                ALPHA_SELECTED_ITEM, ALPHA_UNSELECTED_ITEM);
        mLabelDeselectedAnimator = ObjectAnimator.ofFloat(mTeamNameText, PROPERTY_ALPHA,
                ALPHA_SELECTED_ITEM, ALPHA_UNSELECTED_ITEM);
        mDeselectedItemAnimatorSet = new AnimatorSet().setDuration(DURATION_ANIMATION);
        mDeselectedItemAnimatorSet.playTogether(mIconDeselectedAnimator, mLabelDeselectedAnimator);

        mIconSelectedAnimator = ObjectAnimator.ofFloat(mTeamIconImage, PROPERTY_ALPHA,
                ALPHA_UNSELECTED_ITEM, ALPHA_SELECTED_ITEM);
        mLabelSelectedAnimator = ObjectAnimator.ofFloat(mTeamNameText, PROPERTY_ALPHA,
                ALPHA_UNSELECTED_ITEM, ALPHA_SELECTED_ITEM);
        mSelectedItemAnimatorSet = new AnimatorSet().setDuration(DURATION_ANIMATION);
        mSelectedItemAnimatorSet.playTogether(mIconSelectedAnimator, mLabelSelectedAnimator);
    }

    @Override
    public void onCenterPosition(boolean animate) {
        if (animate) {
            mDeselectedItemAnimatorSet.cancel();
            if (!mSelectedItemAnimatorSet.isRunning()) {
                mIconSelectedAnimator.setFloatValues(
                        mTeamIconImage.getAlpha(), ALPHA_SELECTED_ITEM);
                mLabelSelectedAnimator.setFloatValues(
                        mTeamNameText.getAlpha(), ALPHA_SELECTED_ITEM);
                mSelectedItemAnimatorSet.start();
            }
        } else {
            mSelectedItemAnimatorSet.cancel();
            mTeamIconImage.setAlpha(ALPHA_SELECTED_ITEM);
            mTeamNameText.setAlpha(ALPHA_SELECTED_ITEM);
        }
    }

    @Override
    public void onNonCenterPosition(boolean animate) {
        if (animate) {
            mSelectedItemAnimatorSet.cancel();
            if (!mDeselectedItemAnimatorSet.isRunning()) {
                mIconDeselectedAnimator.setFloatValues(
                        mTeamIconImage.getAlpha(), ALPHA_UNSELECTED_ITEM);
                mLabelDeselectedAnimator.setFloatValues(
                        mTeamNameText.getAlpha(), ALPHA_UNSELECTED_ITEM);
                mDeselectedItemAnimatorSet.start();
            }
        } else {
            mDeselectedItemAnimatorSet.cancel();
            mTeamIconImage.setAlpha(ALPHA_UNSELECTED_ITEM);
            mTeamNameText.setAlpha(ALPHA_UNSELECTED_ITEM);
        }
    }

    public void setTeam(Team team) {
        mTeam = team;
        mTeamNameText.setText(team.name);
        mTeamIconImage.setImageResource(team.logoResourceSmall);
    }

    public Team getTeam() {
        return this.mTeam;
    }

}