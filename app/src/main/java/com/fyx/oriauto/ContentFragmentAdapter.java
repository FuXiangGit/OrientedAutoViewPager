package com.fyx.oriauto;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者 ：付昱翔
 * 时间 ：2017/11/28
 * 描述 ：
 */
public class ContentFragmentAdapter extends FragmentStatePagerAdapter {
    List<Fragment> mFragmentList = new ArrayList<>();

    public ContentFragmentAdapter(FragmentManager fm, List<Fragment> fragmentList) {
        super(fm);
        this.mFragmentList = fragmentList;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }


}
