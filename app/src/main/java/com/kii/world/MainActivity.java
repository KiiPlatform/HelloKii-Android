//
//
// Copyright 2017 Kii Corporation
// http://kii.com
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//

package com.kii.world;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kii.cloud.storage.KiiBucket;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.callback.KiiObjectCallBack;
import com.kii.cloud.storage.callback.KiiQueryCallBack;
import com.kii.cloud.storage.query.KiiQuery;
import com.kii.cloud.storage.query.KiiQueryResult;

public class MainActivity extends Activity implements OnItemClickListener {

    private static final String TAG = "MainActivity";

    // Define strings used for creating objects.
    private static final String BUCKET_NAME = "myBucket";
    private static final String OBJECT_KEY = "myObjectValue";

    // Define the UI elements.
    private ProgressDialog mProgress;

    // Define the list view.
    private ListView mListView;

    // Define the list adapter.
    private ObjectAdapter mListAdapter;

    // Define the object count to easily see
    // object names incrementing.
    private int mObjectCount = 0;

    // Define a custom list adapter to handle KiiObjects.
    public class ObjectAdapter extends ArrayAdapter<KiiObject> {

        // Define variables.
        int resource;
        String response;
        Context context;

        // Initialize the adapter.
        public ObjectAdapter(Context context, int resource,
                List<KiiObject> items) {
            super(context, resource, items);

            // Save the resource for later.
            this.resource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            // Define the row view.
            LinearLayout rowView;

            // Get a reference to the object.
            KiiObject obj = getItem(position);

            // If the row view is not yet created
            if (convertView == null) {

                // Create the row view by inflating the xml resource
                //  (res/layout/row.xml).
                rowView = new LinearLayout(getContext());
                String inflater = Context.LAYOUT_INFLATER_SERVICE;
                LayoutInflater vi;
                vi = (LayoutInflater) getContext().getSystemService(inflater);
                vi.inflate(resource, rowView, true);

            }

            // If the row view is already created, reuse it.
            else {
                rowView = (LinearLayout) convertView;
            }

            // Get the text fields for the row.
            TextView titleText = (TextView) rowView
                    .findViewById(R.id.rowTextTitle);
            TextView subtitleText = (TextView) rowView
                    .findViewById(R.id.rowTextSubtitle);

            // Set the content of the row texts.
            titleText.setText(obj.getString(OBJECT_KEY));
            subtitleText.setText(obj.toUri().toString());

            // Return the row view.
            return rowView;
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Display the layout defined in res/layout/main.xml to the screen.
        setContentView(R.layout.main);

        // Create an empty object adapter.
        mListAdapter = new ObjectAdapter(this, R.layout.row,
                new ArrayList<KiiObject>());

        mListView = (ListView) this.findViewById(R.id.list);
        mListView.setOnItemClickListener(this);
        // Set the adapter to the list view.
        mListView.setAdapter(mListAdapter);

        // Query for any objects created previously.
        this.loadObjects();

    }

    // Load any existing objects associated with this user from the server.
    // This is done on view creation.
    private void loadObjects() {

        // Empty the adapter.
        mListAdapter.clear();

        // Show a progress dialog.
        mProgress = ProgressDialog.show(MainActivity.this, "", "Loading...",
                true);

        // Create an empty KiiQuery. This query will retrieve all results
        // sorted by the creation date.
        KiiQuery query = new KiiQuery(null);
        query.sortByDesc("_created");

        // Define the bucket to query.
        KiiBucket bucket = KiiUser.getCurrentUser().bucket(BUCKET_NAME);

        // Perform the query.
        bucket.query(new KiiQueryCallBack<KiiObject>() {

            // Catch the result from the callback method.
            public void onQueryCompleted(int token,
                                         KiiQueryResult<KiiObject> result, Exception e) {

                // Hide the progress dialog.
                mProgress.dismiss();

                // Check for an exception. The request was successfully processed if e==null.
                if (e == null) {

                    // Add the objects to the list view via the adapter.
                    List<KiiObject> objLists = result.getResult();
                    for (KiiObject obj : objLists) {
                        mListAdapter.add(obj);
                    }

                    // Tell the console and the user that objects were loaded.
                    Log.v(TAG, "Objects loaded: " + result.getResult().toString());
                    showToast("Objects loaded");

                }

                // A failure occurred when processing the request.
                else {

                    // Tell the console and the user that objects were not loaded.
                    Log.v(TAG, "Error loading objects: " + e.getLocalizedMessage());
                    showToast("Error loading objects: " + e.getLocalizedMessage());

                }
            }
        }, query);

    }

    // The user has chosen to create an object from the Options menu.
    // Perform that action here...
    public void addItem(View v) {

        // Show a progress dialog.
        mProgress = ProgressDialog.show(MainActivity.this, "",
                "Creating Object...", true);

        // Create an incremented title for the object.
        String value = "MyObject " + (++mObjectCount);

        // Get a reference to the KiiBucket.
        KiiBucket bucket = KiiUser.getCurrentUser().bucket(BUCKET_NAME);

        // Create a new KiiObject instance and set the key-value pair.
        KiiObject obj = bucket.object();
        obj.set(OBJECT_KEY, value);

        // Save the object asynchronously.
        obj.save(new KiiObjectCallBack() {

            // Catch the result from the callback.
            public void onSaveCompleted(int token, KiiObject o, Exception e) {

                // Hide the progress dialog.
                mProgress.dismiss();

                // Check for an exception. The request was successfully processed if e==null.
                if (e == null) {

                    // Tell the console and the user that the object was created.
                    Log.d(TAG, "Created object: " + o.toString());
                    showToast("Created object");

                    // Insert the object at the beginning of the list adapter.
                    MainActivity.this.mListAdapter.insert(o, 0);

                }

                // A failure occurred when processing the request.
                else {

                    // Tell the console and the user that the object was not created.
                    Log.d(TAG, "Error creating object: " + e.getLocalizedMessage());
                    showToast("Error creating object" + e.getLocalizedMessage());

                }

            }
        });

    }

    // The user has tapped an object on the list.
    // This action is used to possibly delete the tapped object.
    // To confirm the deletion, show a dialog to the user.
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, final int arg2,
                            long arg3) {
        // Build an alert dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Would you like to remove this item?")
                .setCancelable(true)
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {

                            // If the user chooses "Yes"
                            public void onClick(DialogInterface dialog, int id) {

                                // Perform the delete action to the tapped
                                // object.
                                MainActivity.this.performDelete(arg2);
                            }
                        })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {

                    // If the user chooses "No"
                    public void onClick(DialogInterface dialog, int id) {

                        // Simply dismiss the dialog.
                        dialog.cancel();
                    }
                });

        // Show the alert dialog.
        builder.create().show();

    }

    // The user has chosen to delete an object.
    // Perform that action here...
    void performDelete(int position) {

        // Show a progress dialog.
        mProgress = ProgressDialog.show(MainActivity.this, "",
                "Deleting object...", true);

        // Get the object to delete with the index number of the tapped
        // row.
        final KiiObject o = MainActivity.this.mListAdapter.getItem(position);

        // Delete the object asynchronously.
        o.delete(new KiiObjectCallBack() {

            // Catch the result from the callback method.
            public void onDeleteCompleted(int token, Exception e) {

                // Hide the progress dialog.
                mProgress.dismiss();

                // Check for an exception. The request was successfully processed if e==null.
                if (e == null) {

                    // Tell the console and the user that the object was deleted.
                    Log.d(TAG, "Deleted object: " + o.toString());
                    showToast("Deleted object");

                    // Remove the object from the list adapter.
                    MainActivity.this.mListAdapter.remove(o);

                }

                // A failure occurred when processing the request.
                else {

                    // Tell the console and the user that the object was not deleted.
                    Log.d(TAG, "Error deleting object: " + e.getLocalizedMessage());
                    showToast("Error deleting object: " + e.getLocalizedMessage());

                }

            }
        });
    }

    // The user can add items from the Options menu.
    // Create that menu here from the res/menu/menu.xml file.
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        MainActivity.this.addItem(null);
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
