package com.termux;

import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.app.TermuxInstaller;
import com.termux.dummy.DummyContent.DummyItem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem}.
 * TODO: Replace the implementation with code for your data type.
 */
public class RPackageRecyclerViewAdapter extends RecyclerView.Adapter<RPackageRecyclerViewAdapter.ViewHolder> {

    private final List<Pair<String, String>>  mValues;

    public RPackageRecyclerViewAdapter(List<Pair<String, String>> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.fragment_r_package_installer, parent, false);

/*
        View view = LayoutInflater.from(parent.getContext())
            .inflate(viewType, parent, false);
*/

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position).first;
        holder.mIdView.setText(mValues.get(position).first);
        holder.mContentView.setText(mValues.get(position).second);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public String mItem;
        public final ImageButton mActionButton;
        public String action;

        public ViewHolder(View view) {
            super(view);

            mView = view;
            mIdView = (TextView) view.findViewById(R.id.item_number);
            mContentView = (TextView) view.findViewById(R.id.content);
            mActionButton = view.findViewById(R.id.action_menu);

            //
            mActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final EditText taskEditText = new EditText(view.getContext());
                    AlertDialog dialog = new AlertDialog.Builder(view.getContext())
                        .setTitle("Provide an action")
                        .setMessage("Call this action")
                        .setView(taskEditText)
                        .setPositiveButton("Approve", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                action = String.valueOf(taskEditText.getText());

                                // Update a database
                                /// SQLiteDatabase db = new PackagesDbHelper(view.getContext());
                                // ContentValues cv = new ContentValues();
                                // cv.put(PackagesContract.PackageEntry.COLUMN_NAME_NAME, );

                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                    dialog.show();
                }
            });

            // Attach a click listener to the entire row view
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

            String action = PackagesDbHelper.getPackageAction(
                view.getContext(), "shiny"
            );

            Log.e("CLICKED-APP", action);
            TermuxInstaller.performAction("shiny", action);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
