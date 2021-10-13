package com.termux;

import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
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
                                String packageName = mIdView.getText().toString();
                                String packageVersion = mContentView.getText().toString();

                                // Update a database
                                SQLiteDatabase db = new PackagesDbHelper(view.getContext()).getWritableDatabase();
                                ContentValues cv = new ContentValues();
                                cv.put(PackagesContract.PackageEntry.COLUMN_NAME_NAME, packageName);
                                cv.put(PackagesContract.PackageEntry.COLUMN_NAME_VERSION, packageVersion);
                                cv.put(PackagesContract.PackageEntry.COLUMN_NAME_ACTION, action);

                                // Values to compare
                                Log.e("DB", "Compare " + packageName +
                                    " " + packageVersion);

                                // Upsert operation
                                db.beginTransaction();

                                int affectedRows = db.update(
                                    PackagesContract.PackageEntry.TABLE_NAME,
                                    cv,
                                    "name = ? and version = ?",
                                    new String[]{packageName, packageVersion}
                                    );

                                Log.e("DB", "Upsert - affected: " + affectedRows);

                                if (affectedRows == 0) {
                                    long result  = db.insert(
                                        PackagesContract.PackageEntry.TABLE_NAME,
                                        null,
                                        cv
                                    );
                                    Log.e("DB", "Upsert - inserted: " + result);
                                }

                                db.setTransactionSuccessful();
                                db.endTransaction();
                                db.close();

                                Log.e("DB", "Upsert");

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

            String packageName = mIdView.getText().toString();
            String packageVersion = mContentView.getText().toString();

/*
            String action = PackagesDbHelper.getPackageAction(
                view.getContext(), packageName
            );
*/

            SQLiteDatabase db =
                new PackagesDbHelper(view.getContext()).getReadableDatabase();

            Cursor cursor = db.rawQuery(
                "select * from package where name = ?",
                new String[]{packageName}
            );

            cursor.moveToLast();
            String action = cursor.getString(cursor.getColumnIndex("action"));

            db.close();

            Log.e("CLICKED-APP", packageName + "::" + action);
            TermuxInstaller.performAction(packageName, action);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
