package com.micdm.okeybalance.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.micdm.okeybalance.Application;
import com.micdm.okeybalance.R;
import com.micdm.okeybalance.events.Event;
import com.micdm.okeybalance.events.LoadBalanceEvent;

import java.math.BigDecimal;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.functions.Action1;
import rx.functions.Func1;

public class BalanceFragment extends Fragment {

    public static BalanceFragment newInstance(String balance) {
        BalanceFragment fragment = new BalanceFragment();
        Bundle arguments = new Bundle();
        arguments.putString("balance", balance);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Bind(R.id.f__balance__balance)
    protected TextView balanceView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f__balance, container, false);
        ButterKnife.bind(this, view);

        // balanceView.setText(getArguments().getString("balance"));
        Application.getEventBus().getEventObservable(LoadBalanceEvent.class)
            .subscribe(new Action1<Event>() {
                @Override
                public void call(Event event) {
                    balanceView.setText(((LoadBalanceEvent) event).balance);
                }
            });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}
