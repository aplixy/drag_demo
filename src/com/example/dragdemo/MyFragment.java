package com.example.dragdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class MyFragment extends Fragment {
	
	private FragmentActivity mActivity;
	
	private View mView;
	private ListView mListView;
	
	
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
		mActivity = getActivity();
	}

	private void setListener() {
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Toast.makeText(mActivity, "onItemClick--->" + position, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	
}
