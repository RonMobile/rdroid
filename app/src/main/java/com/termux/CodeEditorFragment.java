package com.termux;

import android.os.Bundle;

import android.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import io.github.rosemoe.editor.widget.CodeEditor;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CodeEditorFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * https://github.com/Rosemoe/CodeEditor
 * https://hossainkhan.medium.com/source-code-syntax-highlighting-on-android-taking-full-control-b704fd4bd8ee
 * https://github.com/PDDStudio/highlightjs-android
 * https://github.com/kbiakov/CodeView-android?utm_source=android-arsenal.com&utm_medium=referral&utm_campaign=4216
 *
 */
public class CodeEditorFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private FloatingActionButton floatingActionButton;

    public CodeEditorFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CodeEditorFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CodeEditorFragment newInstance(String param1, String param2) {
        CodeEditorFragment fragment = new CodeEditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        CodeEditor codeEditor = view.findViewById(R.id.view_code_editor);
        floatingActionButton = view.findViewById(R.id.btn_run_code);

        floatingActionButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    int currentLine = codeEditor.getCursor().getRightLine();
                    String selectedText = codeEditor.getText().getLineString(currentLine);
                    Log.e("FloatingButton", selectedText + " " + currentLine);

                }
            }
        );



/*        CodeView codeView = (CodeView) view.findViewById(R.id.view_code_editor);
        codeView.setOptions(Options.Default.get(view.getContext())
            .withLanguage("r")
            .withTheme(ColorTheme.MONOKAI));*/

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_code_editor, container, false);
    }
}
