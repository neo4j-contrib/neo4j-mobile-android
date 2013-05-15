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

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.neo4j.android.client.GraphDatabase;
import org.neo4j.android.client.Neo4jServiceException;
import org.neo4j.android.client.NodeIterator;
import org.neo4j.android.common.ParcelableNode;
import org.neo4j.android.common.ParcelableRelationship;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;
import roboguice.util.Ln;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.inject.Inject;
import com.noser.neo4j.android.dbinspector.R;
import com.noser.neo4j.android.dbinspector.base.DBInspectorConstants;
import com.noser.neo4j.android.dbinspector.interfaces.IDBManager;

@ContentView(R.layout.relationship_activity)
public class RelationshipActivity extends RoboActivity {

    @Inject
    private Context context;

    @Inject
    private IDBManager dbManager;

    @InjectView(R.id.relationshipsLayout)
    private LinearLayout relationshipsLayout;

    @InjectExtra(value = DBInspectorConstants.INTENTEXTRA_CENTER_NODEID)
    private long centerNodeId;

    @InjectExtra(value = DBInspectorConstants.INTENTEXTRA_PEER_NODEID)
    private long peerNodeId;

    @InjectView(R.id.relationshipCreateButton)
    private ImageButton relationshipCreateButton;

    @Inject
    private LayoutInflater inflater;

    private int buttonMinimumHeight;

    private int buttonMinimumWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buttonMinimumHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DBInspectorConstants.BUTTON_MINIMUM_HEIGHT, getResources().getDisplayMetrics());
        buttonMinimumWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DBInspectorConstants.BUTTON_MINIMUM_WIDTH, getResources().getDisplayMetrics());

        setTitle(getResources().getString(R.string.database_title) + " " + dbManager.getCurrentNeo4jDatabaseName());

        relationshipCreateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                askCreateRelationship();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRelationshipsList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == DBInspectorConstants.DEFAULT_REQUESTCODE) && (resultCode == RESULT_OK)) {
            if (data.getExtras().containsKey(DBInspectorConstants.INTENTEXTRA_NODE_DELETED)) {
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateRelationshipsList() {
        try {
            relationshipsLayout.removeAllViews();
            GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
            database.beginTx();
            try {
                ParcelableNode centerNode = database.getNodeById(centerNodeId);
                List<ParcelableRelationship> relationshipsFromCenterNode = new ArrayList<ParcelableRelationship>();
                List<ParcelableRelationship> relationshipsToCenterNode = new ArrayList<ParcelableRelationship>();
                for (ParcelableRelationship relationship : centerNode.getRelationships()) {
                    if ((centerNodeId == relationship.getStartNodeId()) && (peerNodeId == relationship.getEndNodeId())) {
                        relationshipsFromCenterNode.add(relationship);
                    }
                    if ((centerNodeId != peerNodeId) && (peerNodeId == relationship.getStartNodeId())
                            && (centerNodeId == relationship.getEndNodeId())) {
                        relationshipsToCenterNode.add(relationship);
                    }
                }

                Button nodeCenterButton = (Button) findViewById(R.id.nodeCenterButton);
                nodeCenterButton.setText("" + centerNodeId);
                nodeCenterButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showNodePropertyActivity(centerNodeId);
                    }
                });

                Button nodePeerButton = (Button) findViewById(R.id.nodePeerButton);
                if (centerNodeId == peerNodeId) {
                    nodePeerButton.setVisibility(View.INVISIBLE);
                } else {
                    nodePeerButton.setText("" + peerNodeId);
                    nodePeerButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showNodePropertyActivity(peerNodeId);
                        }
                    });
                }

                for (ParcelableRelationship relationship : relationshipsFromCenterNode) {
                    addRelationshipsListItem(relationship, true);
                }
                for (ParcelableRelationship relationship : relationshipsToCenterNode) {
                    addRelationshipsListItem(relationship, false);
                }
            } finally {
                database.txFinish();
            }
        } catch (Exception e) {
            Ln.e(e, "database exception");
            showErrorDialog();
        }
    }

    private void addRelationshipsListItem(final ParcelableRelationship relationship, boolean forward) {
        Button relationshipButton = new Button(context);
        relationshipButton.setMinimumHeight(buttonMinimumHeight);
        relationshipButton.setMinimumWidth(buttonMinimumWidth);
        String text;
        if (centerNodeId != peerNodeId) {
            if (forward) {
                text = getResources().getString(R.string.node_relationships_forward_button);
            } else {
                text = getResources().getString(R.string.node_relationships_backward_button);
            }
        } else {
            text = getResources().getString(R.string.node_relationships_both_button);
        }
        relationshipButton.setText(text + "\n" + relationship.getId() + "\n" + relationship.getName());
        relationshipButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showRelationshipPropertyActivity(relationship.getId());
            }
        });

        relationshipsLayout.addView(relationshipButton);
    }

    private void showErrorDialog() {
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

    private void showNodePropertyActivity(long nodeId) {
        Intent intent = new Intent(getApplicationContext(), NodePropertiesActivity.class);
        intent.putExtra(DBInspectorConstants.INTENTEXTRA_NODEID, nodeId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        startActivityForResult(intent, DBInspectorConstants.DEFAULT_REQUESTCODE);
    }

    private void showRelationshipPropertyActivity(long relationshipId) {
        Intent intent = new Intent(getApplicationContext(), RelationshipPropertiesActivity.class);
        intent.putExtra(DBInspectorConstants.INTENTEXTRA_RELATIONSHIPID, relationshipId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        startActivity(intent);
    }

    private void askCreateRelationship() {
        List<Long> nodeIds;
        try {
            nodeIds = getAllNodeIds();
        } catch (Exception e) {
            Ln.e(e, "database exception");
            showErrorDialog();
            return;
        }

        View relationshipCreateView = inflater.inflate(R.layout.relationship_create, null);

        final TextView relationshipCreateStartNodeIdText = (TextView) relationshipCreateView
                .findViewById(R.id.relationshipCreateStartNodeIdText);

        Formatter formatter = new Formatter();
        formatter.format(getResources().getString(R.string.relationship_create_startnode), centerNodeId);
        relationshipCreateStartNodeIdText.setText(formatter.toString());
        formatter.close();

        final EditText relationshipCreateNameText = (EditText) relationshipCreateView.findViewById(R.id.relationshipCreateNameText);

        final Spinner relationshipCreateEndnodeIdSpinner = (Spinner) relationshipCreateView
                .findViewById(R.id.relationshipCreateEndnodeIdSpinner);
        ArrayAdapter<Long> spinnerAdapter = new ArrayAdapter<Long>(context, android.R.layout.simple_spinner_item, nodeIds);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        relationshipCreateEndnodeIdSpinner.setAdapter(spinnerAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.relationship_create_title);
        dialog.setView(relationshipCreateView);
        dialog.setButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = relationshipCreateNameText.getText().toString();
                long endNodeId = (Long) relationshipCreateEndnodeIdSpinner.getSelectedItem();
                try {
                    createRelationship(name, endNodeId);
                } catch (Exception e) {
                    Ln.e(e, "could not create relationship");
                    showErrorDialog();
                }
                updateRelationshipsList();
            }
        });
        dialog.setButton2(getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private List<Long> getAllNodeIds() throws RemoteException, Neo4jServiceException {
        GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
        database.beginTx();
        try {
            NodeIterator iterator = database.getAllNodes();
            List<Long> nodeIds = new ArrayList<Long>();
            while (iterator.hasNext()) {
                ParcelableNode node = iterator.next();
                nodeIds.add(node.getId());
            }
            return nodeIds;
        } finally {
            database.txFinish();
        }
    }

    private void createRelationship(String name, long endNodeId) throws RemoteException, Neo4jServiceException {
        GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
        database.beginTx();
        try {
            ParcelableRelationship relationship = new ParcelableRelationship();
            relationship.setStartNodeId(centerNodeId);
            relationship.setEndNodeId(endNodeId);
            relationship.setName(name);
            long relationshipId = database.createRelationship(relationship);
            database.txSuccess();
            Ln.i("created relationship '" + relationship + "' with id '" + relationshipId + "'");
        } finally {
            database.txFinish();
        }
    }
}
