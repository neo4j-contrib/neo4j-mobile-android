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
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.neo4j.android.client.GraphDatabase;
import org.neo4j.android.client.Neo4jServiceException;
import org.neo4j.android.common.ParcelableNode;
import org.neo4j.android.common.ParcelableRelationship;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectExtra;
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

@ContentView(R.layout.node_activity)
public class NodeActivity extends RoboActivity {

    @Inject
    private Context context;

    @Inject
    private IDBManager dbManager;

    @InjectView(R.id.nodeRelationshipsLayout)
    private LinearLayout nodeRelationshipsLayout;

    @Inject
    private LayoutInflater inflater;

    @InjectExtra(value = DBInspectorConstants.INTENTEXTRA_CENTER_NODEID)
    private long centerNodeId;

    @InjectView(R.id.nodeCreateButton)
    private ImageButton nodeCreateButton;

    @InjectView(R.id.nodeCenterButton)
    private Button nodeCenterButton;

    @InjectView(R.id.nodeLoopRelationshipsButton)
    private Button nodeLoopRelationshipsButton;

    @InjectResource(R.string.node_create_default_relationship_name)
    private String defaultRelationshipName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        if (bundle.containsKey(DBInspectorConstants.INTENTEXTRA_MOVE_LEFT)
                && bundle.getBoolean(DBInspectorConstants.INTENTEXTRA_MOVE_LEFT)) {
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        }

        setTitle(getResources().getString(R.string.database_title) + " " + dbManager.getCurrentNeo4jDatabaseName());

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

    private void updateNodesList() {
        try {
            nodeRelationshipsLayout.removeAllViews();
            GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
            database.beginTx();
            try {
                ParcelableNode centerNode = database.getNodeById(centerNodeId);
                List<ParcelableRelationship> relationships = centerNode.getRelationships();
                final Set<Long> pointingToCenterNodeNodeIds = new TreeSet<Long>();
                Set<ParcelableNode> peerNodes = new TreeSet<ParcelableNode>(new Comparator<ParcelableNode>() {

                    @Override
                    public int compare(ParcelableNode firstNode, ParcelableNode secondNode) {
                        if (firstNode.getId() < secondNode.getId()) {
                            return -1;
                        } else if (firstNode.getId() > secondNode.getId()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
                int loopRelationshipsCount = 0;
                String lastLoopRelationshipName = "";
                for (ParcelableRelationship relationship : relationships) {
                    if (relationship.getStartNodeId() == relationship.getEndNodeId()) {
                        loopRelationshipsCount++;
                        lastLoopRelationshipName = relationship.getName();
                    } else {
                        if (centerNodeId == relationship.getStartNodeId()) {
                            peerNodes.add(database.getNodeById(relationship.getEndNodeId()));
                        } else {
                            pointingToCenterNodeNodeIds.add(relationship.getStartNodeId());
                            peerNodes.add(database.getNodeById(relationship.getStartNodeId()));
                        }
                    }
                }

                nodeCenterButton.setText("" + centerNodeId);
                nodeCenterButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (pointingToCenterNodeNodeIds.isEmpty()) {
                            // do nothing.
                        } else if (pointingToCenterNodeNodeIds.size() == 1) {
                            showNodeActivity(pointingToCenterNodeNodeIds.iterator().next(), true);
                        } else {
                            showRelatedStartNodes(pointingToCenterNodeNodeIds);
                        }
                    }
                });

                String text = getResources().getString(R.string.node_relationships_loop_button);
                if (loopRelationshipsCount == 0) {
                    nodeLoopRelationshipsButton.setText(text);
                } else if (loopRelationshipsCount == 1) {
                    nodeLoopRelationshipsButton.setText(text + "\n" + lastLoopRelationshipName);
                } else {
                    nodeLoopRelationshipsButton.setText(text + "\n# " + loopRelationshipsCount);
                }
                nodeLoopRelationshipsButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        showRelationshipActivity(centerNodeId, centerNodeId);
                    }
                });

                for (ParcelableNode peerNode : peerNodes) {
                    addNodeRelationshipsListItem(peerNode);
                }
            } finally {
                database.txFinish();
            }
        } catch (Exception e) {
            Ln.e(e, "database exception");
            showErrorDialog();
        }
    }

    private void showRelatedStartNodes(Set<Long> nodeIdSet) {
        final List<Long> nodeIds = new ArrayList<Long>(nodeIdSet);
        final CharSequence[] items = new CharSequence[nodeIds.size()];
        int index = 0;
        for (Long nodeId : nodeIds) {
            items[index] = nodeId.toString();
            index++;
        }

        Formatter formatter = new Formatter();
        formatter.format(getResources().getString(R.string.node_select_related_node_question), centerNodeId);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(formatter.toString());
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                showNodeActivity(nodeIds.get(item), true);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        formatter.close();
    }

    private void addNodeRelationshipsListItem(ParcelableNode peerNode) {

        final long peerNodeId = peerNode.getId();

        List<ParcelableRelationship> relationships = peerNode.getRelationships();
        boolean foundBackwardRelationship = false;
        boolean foundForwardRelationship = false;
        long lastRelationshipId = 0;
        String lastRelationshipName = "";
        int relationshipCount = 0;
        for (ParcelableRelationship relationship : relationships) {
            if (relationship.getStartNodeId() == centerNodeId) {
                foundForwardRelationship = true;
                relationshipCount++;
                lastRelationshipId = relationship.getId();
                lastRelationshipName = relationship.getName();
            }
            if (relationship.getEndNodeId() == centerNodeId) {
                foundBackwardRelationship = true;
                relationshipCount++;
                lastRelationshipId = relationship.getId();
                lastRelationshipName = relationship.getName();
            }
        }

        View nodeRelationshipsListItem = inflater.inflate(R.layout.node_activity_listitem, null);

        Button relationshipsButton = (Button) nodeRelationshipsListItem.findViewById(R.id.nodeItemRelationshipButton);
        String text;
        if (foundForwardRelationship && foundBackwardRelationship) {
            text = getResources().getString(R.string.node_relationships_both_button);
        } else if (foundForwardRelationship) {
            text = getResources().getString(R.string.node_relationships_forward_button);
        } else if (foundBackwardRelationship) {
            text = getResources().getString(R.string.node_relationships_backward_button);
        } else {
            text = getResources().getString(R.string.node_relationships_unknown_button);
        }
        if (relationshipCount == 1) {
            relationshipsButton.setText(text + "\n" + lastRelationshipId + "\n" + lastRelationshipName);
        } else {
            relationshipsButton.setText(text + "\n# " + relationshipCount);
        }
        relationshipsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showRelationshipActivity(centerNodeId, peerNodeId);
            }
        });

        Button nodeButton = (Button) nodeRelationshipsListItem.findViewById(R.id.nodeItemNodeButton);
        nodeButton.setText("" + peerNode.getId());
        nodeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showNodeActivity(peerNodeId, false);
            }
        });

        nodeRelationshipsLayout.addView(nodeRelationshipsListItem);
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

    private void showNodeActivity(long nodeId, boolean moveLeft) {
        Intent intent = new Intent(getApplicationContext(), NodeActivity.class);
        intent.putExtra(DBInspectorConstants.INTENTEXTRA_CENTER_NODEID, nodeId);
        intent.putExtra(DBInspectorConstants.INTENTEXTRA_MOVE_LEFT, moveLeft);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        startActivity(intent);
        finish();
        if (moveLeft) {
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        }
    }

    private void showRelationshipActivity(long centerNodeId, long peerNodeId) {
        Intent intent = new Intent(getApplicationContext(), RelationshipActivity.class);
        intent.putExtra(DBInspectorConstants.INTENTEXTRA_CENTER_NODEID, centerNodeId);
        intent.putExtra(DBInspectorConstants.INTENTEXTRA_PEER_NODEID, peerNodeId);
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
                relationship.setStartNodeId(centerNodeId);
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
