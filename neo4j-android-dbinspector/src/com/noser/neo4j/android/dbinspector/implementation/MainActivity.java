/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.noser.neo4j.android.dbinspector.implementation;

import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.android.client.GraphDatabase;
import org.neo4j.android.client.Neo4jServiceException;
import org.neo4j.android.common.ParcelableNode;
import org.neo4j.android.common.ParcelableRelationship;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import roboguice.util.Ln;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.inject.Inject;
import com.noser.neo4j.android.dbinspector.R;
import com.noser.neo4j.android.dbinspector.base.DBInspectorConstants;
import com.noser.neo4j.android.dbinspector.base.DBInspectorException;
import com.noser.neo4j.android.dbinspector.interfaces.IDBManager;

@ContentView(R.layout.main_activity)
public class MainActivity extends RoboActivity {

    @Inject
    private Context context;

    @Inject
    private IDBManager dbManager;

    @Inject
    private LayoutInflater inflater;

    @InjectView(R.id.mainDatabaseListLayout)
    private LinearLayout mainDatabaseListLayout;

    @InjectView(R.id.mainCreateButton)
    private ImageButton mainCreateButton;

    @InjectView(R.id.mainMenuButton)
    private ImageButton mainMenuButton;

    private Map<String, Integer> databaseNameToIndexMap = new HashMap<String, Integer>();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainCreateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                askCreationOfNeo4jDatabase();
            }
        });

        mainMenuButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                openOptionsMenu();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_test_database:
                createTestDatabase();
                return true;
            case R.id.main_refresh_databases:
                updateNeo4jDatabaseList();
                return true;
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        new WaitForServiceTask().execute();
    }

    private void createTestDatabase() {
        new CreateTestDatabaseTask().execute();
    }

    private void askCreationOfNeo4jDatabase() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.main_create_database_title);

        View mainCreateDatabaseView = inflater.inflate(R.layout.main_activity_createdatabase, null);
        final EditText mainCreateDatabaseEdit = (EditText) mainCreateDatabaseView.findViewById(R.id.mainCreateDatabaseEdit);

        dialog.setView(mainCreateDatabaseView);
        dialog.setButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String databaseName = mainCreateDatabaseEdit.getText().toString();
                if (!databaseName.isEmpty()) {
                    try {
                        if (dbManager.neo4jDatabaseExists(databaseName)) {
                            showDatabaseAlreadyExistsDialog(databaseName);
                        } else {
                            if (dbManager.isCurrentNeo4jDatabaseOpen()) {
                                shutdownNeo4jDatabase(dbManager.getCurrentNeo4jDatabaseName());
                            }
                            openOrCreateNeo4jDatabase(databaseName);
                        }
                    } catch (DBInspectorException e) {
                        Ln.e(e, "failed to check for existing database '" + databaseName + "'.");
                        showErrorDialog();
                    }
                }
            }
        });
        dialog.setButton2(getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private void openOrShutdownNeo4jDatabase(final String databaseName) {
        try {
            if (dbManager.isNeo4jDatabaseOpen(databaseName)) {
                confirmShutdownOfNeo4jDatabase(databaseName);
            } else {
                confirmOpenOfNeo4jDatabase(databaseName);
            }
        } catch (DBInspectorException e) {
            Ln.e(e, "failed to check state of database '" + databaseName + "'.");
            showErrorDialog();
        }
    }

    private void confirmShutdownOfNeo4jDatabase(final String databaseName) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.main_close_database_title);
        Formatter formatter = new Formatter();
        formatter.format(getResources().getString(R.string.main_close_database_question), databaseName);
        dialog.setMessage(formatter.toString());
        dialog.setButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                shutdownNeo4jDatabase(databaseName);
            }
        });
        dialog.setButton2(getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
        formatter.close();
    }

    private void confirmDeletionOfNeo4jDatabase(final String databaseName) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.main_delete_database_title);
        Formatter formatter = new Formatter();
        formatter.format(getResources().getString(R.string.main_delete_database_question), databaseName);
        dialog.setMessage(formatter.toString());
        dialog.setButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dbManager.isCurrentNeo4jDatabaseOpen()) {
                    shutdownNeo4jDatabase(dbManager.getCurrentNeo4jDatabaseName());
                }
                deleteNeo4jDatabase(databaseName);
            }
        });
        dialog.setButton2(getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
        formatter.close();
    }

    private void confirmExportOfNeo4jDatabase(final String databaseName) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.main_export_database_title);
        Formatter formatter = new Formatter();
        formatter.format(getResources().getString(R.string.main_export_database_question), databaseName);
        dialog.setMessage(formatter.toString());
        dialog.setButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dbManager.isCurrentNeo4jDatabaseOpen()) {
                    shutdownNeo4jDatabase(dbManager.getCurrentNeo4jDatabaseName());
                }
                exportNeo4jDatabase(databaseName);
            }
        });
        dialog.setButton2(getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
        formatter.close();
    }

    private void confirmOpenOfNeo4jDatabase(final String databaseName) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.main_open_database_title);
        Formatter formatter = new Formatter();
        formatter.format(getResources().getString(R.string.main_open_database_question), databaseName);
        dialog.setMessage(formatter.toString());
        dialog.setButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dbManager.isCurrentNeo4jDatabaseOpen()) {
                    shutdownNeo4jDatabase(dbManager.getCurrentNeo4jDatabaseName());
                }
                openOrCreateNeo4jDatabase(databaseName);
            }
        });
        dialog.setButton2(getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
        formatter.close();
    }

    private void updateNeo4jDatabaseList() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                List<String> databaseNames;
                try {
                    databaseNames = dbManager.listAvailableNeo4jDatabases();
                } catch (DBInspectorException e) {
                    Ln.e(e, "failed to list available databases.");
                    showErrorDialog();
                    return;
                }
                mainDatabaseListLayout.removeAllViews();
                databaseNameToIndexMap.clear();
                int index = 0;
                for (final String databaseName : databaseNames) {
                    View mainDatabaseListItem = inflater.inflate(R.layout.main_activity_listitem, null);
                    mainDatabaseListItem.setId(index);
                    databaseNameToIndexMap.put(databaseName, index);

                    TextView mainDatabaseText = (TextView) mainDatabaseListItem.findViewById(R.id.mainDatabaseText);
                    mainDatabaseText.setText(databaseName);

                    ImageView mainDatabaseStateView = (ImageView) mainDatabaseListItem.findViewById(R.id.mainDatabaseStateView);
                    updateNeo4jDatabaseState(databaseName, mainDatabaseStateView);

                    ImageButton mainOpenDatabaseButton = (ImageButton) mainDatabaseListItem
                            .findViewById(R.id.mainOpenDatabaseButton);
                    mainOpenDatabaseButton.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (databaseName.equals(dbManager.getCurrentNeo4jDatabaseName())) {
                                showRootNodeActivity();
                            } else {
                                openOrShutdownNeo4jDatabase(databaseName);
                            }
                        }
                    });

                    ImageButton mainDeleteDatabaseButton = (ImageButton) mainDatabaseListItem
                            .findViewById(R.id.mainDeleteDatabaseButton);
                    mainDeleteDatabaseButton.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            confirmDeletionOfNeo4jDatabase(databaseName);
                        }
                    });

                    ImageButton mainExportDatabaseButton = (ImageButton) mainDatabaseListItem
                            .findViewById(R.id.mainExportDatabaseButton);
                    mainExportDatabaseButton.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            confirmExportOfNeo4jDatabase(databaseName);
                        }
                    });

                    mainDatabaseListLayout.addView(mainDatabaseListItem);
                    index++;
                }
                mainDatabaseListLayout.requestLayout();
            }
        });
    }

    private void updateNeo4jDatabaseState(final String databaseName) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Integer index = databaseNameToIndexMap.get(databaseName);
                if (index != null) {
                    View mainDatabaseListItem = mainDatabaseListLayout.findViewById(index);
                    ImageView mainDatabaseStateView = (ImageView) mainDatabaseListItem.findViewById(R.id.mainDatabaseStateView);
                    updateNeo4jDatabaseState(databaseName, mainDatabaseStateView);
                } else {
                    Ln.e("databaseName '" + databaseName + "' not found in databaseNameToIndexMap '" + databaseNameToIndexMap + "'");
                }
            }
        });
    }

    private void updateNeo4jDatabaseState(String databaseName, ImageView mainDatabaseStateView) {
        try {
            if (dbManager.isNeo4jDatabaseOpen(databaseName)) {
                if (databaseName.equals(dbManager.getCurrentNeo4jDatabaseName())) {
                    mainDatabaseStateView.setImageResource(android.R.drawable.presence_online);
                } else {
                    mainDatabaseStateView.setImageResource(android.R.drawable.presence_busy);
                }
            } else {
                mainDatabaseStateView.setImageResource(android.R.drawable.presence_offline);
            }
        } catch (DBInspectorException e) {
            Ln.e(e, "failed to check state of database '" + databaseName + "'.");
            showErrorDialog();
        }
    }

    private void openOrCreateNeo4jDatabase(String databaseName) {
        Ln.i("opening/creating database '" + databaseName + "'");
        new OpenOrCreateNeo4jDatabaseTask().execute(databaseName);
    }

    private void shutdownNeo4jDatabase(String databaseName) {
        Ln.i("shutting down database '" + databaseName + "'");
        try {
            dbManager.shutdownNeo4jDatabase(databaseName);
            updateNeo4jDatabaseState(databaseName);
        } catch (DBInspectorException e) {
            Ln.e(e, "failed to shutdown the database '" + databaseName + "'.");
            showErrorDialog();
        }
    }

    private void deleteNeo4jDatabase(String databaseName) {
        Ln.i("deleting database '" + databaseName + "'");
        try {
            dbManager.deleteNeo4jDatabase(databaseName);
            updateNeo4jDatabaseList();
        } catch (DBInspectorException e) {
            Ln.e(e, "failed to delete the database '" + databaseName + "'.");
            showErrorDialog();
        }
    }

    private void exportNeo4jDatabase(String databaseName) {
        Ln.i("exporting database '" + databaseName + "'");
        try {
            dbManager.exportNeo4jDatabase(databaseName);
            updateNeo4jDatabaseList();
        } catch (DBInspectorException e) {
            Ln.e(e, "failed to export the database '" + databaseName + "'.");
            showErrorDialog();
        }
    }

    private void showDatabaseAlreadyExistsDialog(final String databaseName) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(context).create();
                dialog.setTitle(R.string.error_title);

                Formatter formatter = new Formatter();
                formatter.format(getResources().getString(R.string.main_create_database_already_exists_error), databaseName);
                dialog.setMessage(formatter.toString());
                dialog.setButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialog.show();
                formatter.close();
            }
        });
    }

    private void showErrorDialog() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(context).create();
                dialog.setTitle(R.string.error_title);
                dialog.setMessage(getResources().getText(R.string.database_error));
                dialog.setButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialog.show();
            }
        });
    }

    private void showRootNodeActivity() {
        Intent intent = new Intent(getApplicationContext(), RootNodeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        startActivity(intent);
    }

    private class WaitForServiceTask extends AsyncTask<Void, Void, Void> {

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, "", getResources().getString(R.string.main_waiting_for_service), true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (!dbManager.isNeo4jServiceAvailable()) {
                try {
                    Thread.sleep(DBInspectorConstants.WAIT_FOR_SERVICE_TIME);
                } catch (InterruptedException e) {
                    // do nothing.
                }
            }
            Ln.d("service connected.");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
            updateNeo4jDatabaseList();
        }
    }

    private class OpenOrCreateNeo4jDatabaseTask extends AsyncTask<String, Void, Boolean> {

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, "", getResources().getString(R.string.main_opening_database), true);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String databaseName = params[0];
            try {
                dbManager.openOrCreateNeo4jDatabase(databaseName);
                return true;
            } catch (DBInspectorException e) {
                Ln.e(e, "failed to open database '" + databaseName + "'");
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            progressDialog.dismiss();
            updateNeo4jDatabaseList();
            if (result) {
                showRootNodeActivity();
            } else {
                showErrorDialog();
            }
        }
    }

    /**
     * this creates a test graph with a node count = TEST_DATABASE_DENSITY ^
     * TEST_DATABASE_DEPTH + TEST_DATABASE_DENSITY + 1.
     */
    private class CreateTestDatabaseTask extends AsyncTask<Void, Void, Boolean> {

        private int testIndex;

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, "", getResources().getString(R.string.main_creating_test_database), true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                dbManager.deleteNeo4jDatabase(DBInspectorConstants.TEST_DATABASE);
                dbManager.openOrCreateNeo4jDatabase(DBInspectorConstants.TEST_DATABASE);
                populateDatabase();
                Ln.i("test database '" + DBInspectorConstants.TEST_DATABASE + "' created.");
                return true;
            } catch (Exception e) {
                Ln.e(e, "failed to create test database '" + DBInspectorConstants.TEST_DATABASE + "'.");
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            progressDialog.dismiss();
            updateNeo4jDatabaseList();
            if (!result) {
                showErrorDialog();
            }
        }

        private void populateDatabase() throws Neo4jServiceException, RemoteException {
            GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
            ParcelableNode referenceNode = database.getReferenceNode();
            testIndex = 0;
            database.beginTx();
            try {
                referenceNode.setProperty("test_node_index", "" + 0);
                referenceNode.setProperty("timestamp", "" + System.currentTimeMillis());
                database.updateNode(referenceNode);
                createStarRelationships(database, referenceNode.getId(), 0);

                database.txSuccess();
            } finally {
                database.txFinish();
            }
        }

        private void createStarRelationships(GraphDatabase database, long centerNodeId, int depth) throws RemoteException,
                Neo4jServiceException {
            if (depth < DBInspectorConstants.TEST_DATABASE_DEPTH) {
                for (int index = 0; index < DBInspectorConstants.TEST_DATABASE_DENSITY; index++) {
                    testIndex++;
                    ParcelableNode node = createNode(testIndex);
                    long nodeId = database.createNode(node);
                    ParcelableRelationship relationship = createRelationship(centerNodeId, nodeId, testIndex);
                    database.createRelationship(relationship);

                    createStarRelationships(database, nodeId, depth + 1);
                }
            }
        }

        private ParcelableNode createNode(int index) {
            ParcelableNode node = new ParcelableNode();
            node.setProperty("test_node_index", "" + index);
            node.setProperty("timestamp", "" + System.currentTimeMillis());
            Ln.d("created node '" + node + "'");
            return node;
        }

        private ParcelableRelationship createRelationship(long startNodeId, long endNodeId, int index) {
            ParcelableRelationship relationship = new ParcelableRelationship();
            relationship.setStartNodeId(startNodeId);
            relationship.setEndNodeId(endNodeId);
            relationship.setName("test_relationship");
            relationship.setProperty("test_relationship_index", "" + index);
            relationship.setProperty("timestamp", "" + System.currentTimeMillis());
            Ln.d("created relationship '" + relationship + "'");
            return relationship;
        }
    }
}
