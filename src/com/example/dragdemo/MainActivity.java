package com.example.dragdemo;

import java.util.ArrayList;
import java.util.List;

import com.example.dragdemo.DragLinerLayout.OnDragHeadListener;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout.LayoutParams;

public class MainActivity extends FragmentActivity {
	
	private static final String TAG = "MainActivity";
	
	private DragLinerLayout mDragLinerLayout;
	
	private View mHeaderView;
	private View mContentView;
	
	private ViewPager mViewPager;
	private List<MyFragment> mFragmentList;
	private MyPagerAdapter mPagerAdapter;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		findViews();
		init();
		setListener();
	}

	private void findViews() {
		mDragLinerLayout = (DragLinerLayout) findViewById(R.id.main_draglinerlayout_root);
		
		mHeaderView = LayoutInflater.from(this).inflate(R.layout.header_layout, null);
		mContentView = LayoutInflater.from(this).inflate(R.layout.content_layout, null);
		
		mViewPager = (ViewPager) mContentView.findViewById(R.id.content_viewpager);
	}

	private void init() {
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, 300);
		mHeaderView.setLayoutParams(lp);
		
		LayoutParams lp2 = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		mContentView.setLayoutParams(lp2);
		
		mFragmentList = new ArrayList<MyFragment>();
		for (int i = 0; i < 3; i++) {
			mFragmentList.add(new MyFragment());
		}
		mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), mFragmentList, null);
		mViewPager.setAdapter(mPagerAdapter);
		
		
		mDragLinerLayout.addHeaderView(mHeaderView);
		mDragLinerLayout.addContentView(mContentView);
	}

	private void setListener() {
		mDragLinerLayout.setOnDragHeadListener(new OnDragHeadListener() {
			@Override
			public void onDragStart(DragLinerLayout layout, boolean isMoveUp) {
				Log.d(TAG, "onDragStart, isMoveUp--->" + isMoveUp);
			}
			
			@Override
			public void onAttach(DragLinerLayout layout, boolean isAttachTop) {
				Log.i(TAG, "onAttach, isAttachTop--->" + isAttachTop);
			}
		});
		
		
		
	}
	
}
