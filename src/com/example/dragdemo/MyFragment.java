package com.example.dragdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

public class MyFragment extends Fragment {
	
	private View mView;
	private ListView mListView;
	
	private OnScrollListener mOnScrollListener;
	
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		
		mView = inflater.inflate(R.layout.fragment_one_day, null);
		
		return mView;
	}
	
	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		findViews();
		init();
		setListener();
	}

	private void findViews() {
		mListView = (ListView) mView.findViewById(R.id.listview1);
	}

	private void init() {
		
	}

	private void setListener() {
		if (mOnScrollListener != null) mListView.setOnScrollListener(mOnScrollListener);
	}
	
	public void setOnScrollListener(OnScrollListener l) {
		mOnScrollListener = l;
		if (mListView != null) mListView.setOnScrollListener(mOnScrollListener);
	}
	
	
}
