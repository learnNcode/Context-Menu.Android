package com.yalantis.dropdownmenu;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kirill-Penzykov on 23.12.2014.
 */
public class MenuAdapter {

    private static final int ANIMATION_DURATION_MILLIS = 150;

    private OnItemClickListener mOnItemClickListener;
    private Context mContext;
    private LinearLayout mMenuWrapper;
    private LinearLayout mTextWrapper;
    private View mClickedView;
    private List<MenuObject> mMenuObjects;
    private AnimatorSet mAnimatorSetHideMenu;
    private AnimatorSet mAnimatorSetShowMenu;
    private boolean mIsMenuOpen = false;
    private boolean mIsAnimationRun = false;
    private int mActionBarSize;

    public interface OnItemClickListener {
        public void onClick(View v);
    }

    public MenuAdapter(Context context, LinearLayout menuWrapper, LinearLayout textWrapper, List<MenuObject> menuObjects,
                       OnItemClickListener onItemClickListener) {
        this.mContext = context;
        this.mMenuWrapper = menuWrapper;
        this.mTextWrapper = textWrapper;
        this.mMenuObjects = menuObjects;
        this.mOnItemClickListener = onItemClickListener;

        mActionBarSize = Utils.getDefaultActionBarSize(mContext);

        setViews();
        resetAnimations();
        mAnimatorSetShowMenu = setOpenCloseAnimation(false);
        mAnimatorSetHideMenu = setOpenCloseAnimation(true);
    }

    public int getItemCount() {
        return mMenuObjects.size();
    }

    /**
     * Creating views and filling to wrappers
     */
    private void setViews() {
        for (MenuObject menuObject : mMenuObjects) {
            mTextWrapper.addView(Utils.getItemTextView(mContext, menuObject.getTitle(), mActionBarSize));
            mMenuWrapper.addView(Utils.getItemImageButton(mContext, mActionBarSize, menuObject.getDrawable(), clickItem));
        }
    }

    /**
     * Set starting params to vertical animations
     */
    private void resetVerticalAnimation(View view, boolean toTop) {
        if (!mIsMenuOpen) {
            ViewHelper.setRotation(view, 0);
            ViewHelper.setRotationY(view, 0);
            ViewHelper.setRotationX(view, -90);
        }
        ViewHelper.setPivotX(view, view.getMeasuredHeight() / 2);
        ViewHelper.setPivotY(view, !toTop ? 0 : view.getMeasuredHeight());
    }

    /**
     * Set starting params to side animations
     */
    private void resetSideAnimation(View view) {
        if (!mIsMenuOpen) {
            ViewHelper.setRotation(view, 0);
            ViewHelper.setRotationY(view, -90);
            ViewHelper.setRotationX(view, 0);
        }
        ViewHelper.setPivotX(view, view.getMeasuredHeight());
        ViewHelper.setPivotY(view, view.getMeasuredHeight() / 2);

    }

    /**
     * Set starting params to text animations
     */
    private void resetTextAnimation(View v) {
        ViewHelper.setAlpha(v, !mIsMenuOpen ? 0 : 1);
        ViewHelper.setTranslationX(v, !mIsMenuOpen ? mActionBarSize : 0);
    }

    /**
     * Set starting params to all animations
     */
    private void resetAnimations() {
        for (int i = 0; i < getItemCount(); i++) {
            resetTextAnimation(mTextWrapper.getChildAt(i));
            if (i == 0) {
                resetSideAnimation(mMenuWrapper.getChildAt(i));
            } else {
                resetVerticalAnimation(mMenuWrapper.getChildAt(i), false);
            }
        }
    }

    /**
    *   Creates Open / Close AnimatorSet
    */
    private AnimatorSet setOpenCloseAnimation(boolean isCloseAnimation) {
        List<Animator> textAnimations = new ArrayList<>();
        List<Animator> imageAnimations = new ArrayList<>();

        if (isCloseAnimation) {
            for (int i = getItemCount() - 1; i >= 0; i--) {
                fillOpenClosingAnimations(isCloseAnimation, textAnimations, imageAnimations, i);
            }
        } else {
            for (int i = 0; i < getItemCount(); i++) {
                fillOpenClosingAnimations(isCloseAnimation, textAnimations, imageAnimations, i);
            }
        }

        AnimatorSet textCloseAnimatorSet = new AnimatorSet();
        textCloseAnimatorSet.playSequentially(textAnimations);

        AnimatorSet imageCloseAnimatorSet = new AnimatorSet();
        imageCloseAnimatorSet.playSequentially(imageAnimations);

        AnimatorSet animatorFullSet = new AnimatorSet();
        animatorFullSet.playTogether(imageCloseAnimatorSet, textCloseAnimatorSet);
        animatorFullSet.setDuration(ANIMATION_DURATION_MILLIS);
        animatorFullSet.addListener(mCloseOpenAnimatorListener);
        return animatorFullSet;
    }

    /**
     * Filling arrays of animations to build Set of Closing / Opening animations
    */
    private void fillOpenClosingAnimations(boolean isCloseAnimation, List<Animator> textAnimations, List<Animator> imageAnimations, int wrapperPosition) {
        AnimatorSet textAnimatorSet = new AnimatorSet();
        Animator textAppearance = isCloseAnimation ?
                AnimatorUtils.alfaDisappear(mTextWrapper.getChildAt(wrapperPosition))
                : AnimatorUtils.alfaAppear(mTextWrapper.getChildAt(wrapperPosition));

        Animator textTranslation = isCloseAnimation ?
                AnimatorUtils.translationRight(mTextWrapper.getChildAt(wrapperPosition), mContext.getResources().getDimension(R.dimen.text_right_translation))
                : AnimatorUtils.translationLeft(mTextWrapper.getChildAt(wrapperPosition), mContext.getResources().getDimension(R.dimen.text_right_translation));

        textAnimatorSet.playTogether(textAppearance, textTranslation);
        textAnimations.add(textAnimatorSet);

        Animator imageRotation = isCloseAnimation ?
                wrapperPosition == 0 ? AnimatorUtils.rotationCloseToRight(mMenuWrapper.getChildAt(wrapperPosition)) : AnimatorUtils.rotationCloseVertical(mMenuWrapper.getChildAt(wrapperPosition))
                : wrapperPosition == 0 ? AnimatorUtils.rotationOpenFromRight(mMenuWrapper.getChildAt(wrapperPosition)) : AnimatorUtils.rotationOpenVertical(mMenuWrapper.getChildAt(wrapperPosition));
        imageAnimations.add(imageRotation);
    }

    private View.OnClickListener clickItem = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mIsMenuOpen && !mIsAnimationRun) {
                mClickedView = v;
                toggleIsAnimationRun();
                int childIndex = mMenuWrapper.indexOfChild(v);
                if (childIndex == -1) { return; }

                buildChosenAnimation(childIndex);
                toggleIsMenuOpen();
            }
        }
    };

    private void buildChosenAnimation(int childIndex){
        List<Animator> fadeOutTextTopAnimatorList = new ArrayList<>();
        List<Animator> closeToBottomImageAnimatorList = new ArrayList<>();
        for (int i = 0; i < childIndex; i++) {
            View view = mMenuWrapper.getChildAt(i);
            resetVerticalAnimation(view, true);
            closeToBottomImageAnimatorList.add(AnimatorUtils.rotationCloseVertical(view));
            fadeOutTextTopAnimatorList.add(AnimatorUtils.fadeOutSet(mTextWrapper.getChildAt(i), mContext.getResources().getDimension(R.dimen.text_right_translation)));
        }
        AnimatorSet closeToBottom = new AnimatorSet();
        closeToBottom.playSequentially(closeToBottomImageAnimatorList);
        AnimatorSet fadeOutTop = new AnimatorSet();
        fadeOutTop.playSequentially(fadeOutTextTopAnimatorList);

        List<Animator> fadeOutTextBottomAnimatorList = new ArrayList<>();
        List<Animator> closeToTopAnimatorObjects = new ArrayList<>();
        for (int i = getItemCount() - 1; i > childIndex; i--) {
            View view = mMenuWrapper.getChildAt(i);
            resetVerticalAnimation(view, false);
            closeToTopAnimatorObjects.add(AnimatorUtils.rotationCloseVertical(view));
            fadeOutTextBottomAnimatorList.add(AnimatorUtils.fadeOutSet(mTextWrapper.getChildAt(i), mContext.getResources().getDimension(R.dimen.text_right_translation)));
        }
        AnimatorSet closeToTop = new AnimatorSet();
        closeToTop.playSequentially(closeToTopAnimatorObjects);
        AnimatorSet fadeOutBottom = new AnimatorSet();
        fadeOutBottom.playSequentially(fadeOutTextBottomAnimatorList);

        resetSideAnimation(mMenuWrapper.getChildAt(childIndex));
        ObjectAnimator closeToRight = AnimatorUtils.rotationCloseToRight(mMenuWrapper.getChildAt(childIndex));
        closeToRight.addListener(mChosenItemFinishAnimatorListener);
        AnimatorSet fadeOutChosenText = AnimatorUtils.fadeOutSet(mTextWrapper.getChildAt(childIndex), mContext.getResources().getDimension(R.dimen.text_right_translation));

        AnimatorSet imageFullAnimatorSet = new AnimatorSet();
        imageFullAnimatorSet.play(closeToBottom).with(closeToTop);
        AnimatorSet textFullAnimatorSet = new AnimatorSet();
        textFullAnimatorSet.play(fadeOutTop).with(fadeOutBottom);
        if (closeToBottomImageAnimatorList.size() >= closeToTopAnimatorObjects.size()) {
            imageFullAnimatorSet.play(closeToBottom).before(closeToRight);
            textFullAnimatorSet.play(fadeOutTop).before(fadeOutChosenText);
        } else {
            imageFullAnimatorSet.play(closeToTop).before(closeToRight);
            textFullAnimatorSet.play(fadeOutBottom).before(fadeOutChosenText);
        }

        AnimatorSet fullAnimatorSet = new AnimatorSet();
        fullAnimatorSet.playTogether(imageFullAnimatorSet,textFullAnimatorSet);
        fullAnimatorSet.setDuration(ANIMATION_DURATION_MILLIS);
        fullAnimatorSet.start();
    }

    public void menuToggle() {

        if (!mIsAnimationRun) {
            resetAnimations();
            mIsAnimationRun = true;
            if (mIsMenuOpen) {
                mAnimatorSetHideMenu.start();
            } else {
                mAnimatorSetShowMenu.start();
            }
            toggleIsMenuOpen();
        }
    }

    private void toggleIsAnimationRun() {
        mIsAnimationRun = !mIsAnimationRun;
    }

    private void toggleIsMenuOpen() {
        mIsMenuOpen = !mIsMenuOpen;
    }


    private Animator.AnimatorListener mCloseOpenAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            toggleIsAnimationRun();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    };

    private Animator.AnimatorListener mChosenItemFinishAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            toggleIsAnimationRun();
            mOnItemClickListener.onClick(mClickedView);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    };

}
