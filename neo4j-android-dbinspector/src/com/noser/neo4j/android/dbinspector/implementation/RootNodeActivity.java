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
import java.util.List;

import org.neo4j.android.client.GraphDatabase;
import org.neo4j.android.client.Neo4jServiceException;
import org.neo4j.android.client.NodeIterator;
import org.neo4j.android.common.Direction;
import org.neo4j.android.common.ParcelableNode;
import org.neo4j.android.common.ParcelableRelationship;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;
import roboguice.util.Ln;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.inject.Inject;
import com.noser.neo4j.android.dbinspector.R;
import com.noser.neo4j.android.dbinspector.base.DBInspectorConstants;
import com.noser.neo4j.android.dbinspector.interfaces.IDBManager;

@ContentView(R.layout.rootnode_activity)
public class RootNodeActivity extends RoboActivity {

    @Inject
    private Context context;

    @Inject
    private IDBManager dbManager;

    @InjectView(R.id.referenceNodeLayout)
    private LinearLayout referenceNodeLayout;

    @InjectView(R.id.rootNodesLayout)
    private LinearLayout rootNodesLayout;

    @InjectView(R.id.cycleNodesLayout)
    private LinearLayout cycleNodesLayout;

    @InjectView(R.id.nodeCreateButton)
    ImageButton nodeCreateButton;

    @Inject
    private LayoutInflater inflater;

    @InjectResource(R.string.node_create_default_relationship_name)
    private String defaultRelationshipName;

    private long referenceNodeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getResources().getString(R.string.database_title) + " " + dbManager.getCurrentNeo4jDatabaseName());

        retrieveReferenceNodeId();

        nodeCreateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                askCreateNode();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNodesList();
    }

    private void retrieveReferenceNodeId() {
        try {
            GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
            database.beginTx();
            try {
                ParcelableNode referenceNode = database.getReferenceNode();
                referenceNodeId = referenceNode.getId();
            } finally {
                database.txFinish();
            }
        } catch (Exception e) {
            Ln.e(e, "database exception");
            showErrorDialog();
        }
    }

    private void updateNodesList() {
        try {
            referenceNodeLayout.removeAllViews();
            rootNodesLayout.removeAllViews();
            cycleNodesLayout.removeAllViews();

            GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
            database.beginTx();
            try {
                ParcelableNode referenceNode = database.getReferenceNode();
                addNodeListItem(referenceNodeLayout, referenceNode);

                List<ParcelableNode> rootNodes = new ArrayList<ParcelableNode>();
                NodeIterator nodeIterator = database.getAllNodes();
                while (nodeIterator.hasNext()) {
                    ParcelableNode node = nodeIterator.next();
                    if (!node.hasRelationship(Direction.INCOMING)) {
                        rootNodes.add(node);
                    }
                }
                for (ParcelableNode rootNode : rootNodes) {
                    addNodeListItem(rootNodesLayout, rootNode);
                }

                // TODO: [eRiC] what happens if all graphs are circular? ->
                // pick
                // one node per graph:

            } finally {
                database.txFinish();
            }
        } catch (Exception e) {
            Ln.e(e, "database exception");
            showErrorDialog();
        }
    }

    private void addNodeListItem(LinearLayout nodeLayout, ParcelableNode node) {
        View nodesListItem = inflater.inflate(R.layout.rootnode_activity_listitem, null);
        Button nodeButton = (Button) nodesListItem.findViewById(R.id.rootNodeButton);
        final long nodeId = node.getId();
        nodeButton.setText("" + nodeId);
        nodeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showNodeActivity(nodeId);
            }
        });
        nodeLayout.addView(nodesListItem);
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

    private void showNodeActivity(long nodeId) {
        Intent intent = new Intent(getApplicationContext(), NodeActivity.class);
        intent.putExtra(DBInspectorConstants.INTENTEXTRA_CENTER_NODEID, nodeId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        startActivity(intent);
    }

    private void askCreateNode() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.node_create_title);

        View nodeCreateView = inflater.inflate(R.layout.node_create, null);
        final CheckBox nodeCreateDefaultRelationShipCheckBox = (CheckBox) nodeCreateView
                .findViewById(R.id.nodeCreateDefaultRelationShipCheckBox);
        nodeCreateDefaultRelationShipCheckBox.setChecked(true);

        dialog.setView(nodeCreateView);
        dialog.setButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean createDefaultRelationship = nodeCreateDefaultRelationShipCheckBox.isChecked();
                try {
                    createNode(createDefaultRelationship);
                } catch (Exception e) {
                    Ln.e(e, "could not create node");
                    showErrorDialog();
                }
                updateNodesList();
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

    private void createNode(boolean createDefaultRelationship) throws RemoteException, Neo4jServiceException {
        GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
        database.beginTx();
        try {
            ParcelableNode node = new ParcelableNode();
            long nodeId = database.createNode(node);
            Ln.i("created node '" + node + "' with id '" + nodeId + "'");

            if (createDefaultRelationship) {
                ParcelableRelationship relationship = new ParcelableRelationship();
                relationship.setStartNodeId(referenceNodeId);
                relationship.setEndNodeId(nodeId);
                relationship.setName(defaultRelationshipName);
                long relationshipId = database.createRelationship(relationship);
                Ln.i("created relationship '" + relationship + "' with id '" + relationshipId + "'");
            }
            database.txSuccess();
        } finally {
            database.txFinish();
        }
    }
}
