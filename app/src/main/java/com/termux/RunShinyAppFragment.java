package com.termux;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.app.TermuxInstaller;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 */
public class RunShinyAppFragment extends Fragment {

    // https://medium.com/hackernoon/android-recyclerview-onitemclicklistener-getadapterposition-a-better-way-3c789baab4db

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private List<String> packages;

    private List<Pair<String, String>> mInstalledPackages;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RunShinyAppFragment() {}

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static RunShinyAppFragment newInstance(int columnCount) {
        RunShinyAppFragment fragment = new RunShinyAppFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_r_package_installer_list,
            container, false);


        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        // Setup any handles to view objects here
        RecyclerView recyclerView = view.findViewById(R.id.list);
        EditText mEditText = view.findViewById(R.id.search_box);

        // Button
        Button mBtnAddLib = view.findViewById(R.id.add_lib_button);

        mBtnAddLib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String libName = mEditText.getText().toString();
                Log.e("TRollo", libName);
                //TermuxInstaller.installRPackage(libName);
                //TermuxInstaller.getInstalledPackages();
                Toast.makeText(view.getContext(), libName, Toast.LENGTH_LONG);
            }
        });

        TermuxInstaller.cleanOutput();

        TermuxInstaller.mCurrentOutputObservable.subscribe(s -> {
            List<Pair<String, String>> l = extractLibraries(s);
            // RecyclerView recyclerView = (RecyclerView) rview;
            recyclerView.setAdapter(new RPackageRecyclerViewAdapter(l));
        });

        // Set the adapter
        Context context = view.getContext();
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }

        TermuxInstaller.getShinyDependentPackages();
    }


    private List<Pair<String, String>> extractLibraries(String s){
        List<String> l = Arrays.asList(s.split("\n"));
        List<Pair<String, String>> libs = new LinkedList<>();

        for (String str : l) {
            List<String> splittedRow = Arrays.asList(str.split(" "));

            if (splittedRow.size() != 2)
                continue;

            libs.add(
                new Pair<>(
                    splittedRow.get(0).replaceAll("\"", "").trim(),
                    splittedRow.get(1).replaceAll("\"", "").trim()
                )
            );
        }

        return libs;
    }
}
