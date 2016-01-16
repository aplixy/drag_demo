package com.example.dragdemo;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.ViewGroup;

public class MyPagerAdapter extends FragmentPagerAdapter {
	
	private List<MyFragment> mFragmentList;
	private List<String> mTitleList;

	public MyPagerAdapter(FragmentManager fm, List<MyFragment> fragmentList, List<String> titleList) {
		super(fm);
		
		mFragmentList = fragmentList;
		mTitleList = titleList;
	}

	@Override
	public Fragment getItem(int position) {
		return mFragmentList.get(position);
	}

	@Override
	public int getCount() {
		return mFragmentList.size();
	}
	
//	@Override
//	public CharSequence getPageTitle(int position) {
//		if (mTitleList == null || position >= mTitleList.size()) return null;
//		
//		return mTitleList.get(position);
//	}
	
//	@Override
//	public int getItemPosition(Object object) {
//		return PagerAdapter.POSITION_NONE;
//	}
//	
//	@Override
//	public Object instantiateItem(ViewGroup container, int position) {
//		return super.instantiateItem(container, position);
//	}

}
