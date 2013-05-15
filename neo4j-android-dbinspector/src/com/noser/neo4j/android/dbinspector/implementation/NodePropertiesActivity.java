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
import java.util.List;

import org.neo4j.android.client.GraphDatabase;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.inject.Inject;
import com.noser.neo4j.android.dbinspector.R;
import com.noser.neo4j.android.dbinspector.base.DBInspectorConstants;
import com.noser.neo4j.android.dbinspector.interfaces.IDBManager;

@ContentView(R.layout.node_properties_activity)
public class NodePropertiesActivity extends RoboActivity {

    @Inject
    private Context context;

    @Inject
    private IDBManager dbManager;

    @Inject
    private LayoutInflater inflater;

    @InjectExtra(value = DBInspectorConstants.INTENTEXTRA_NODEID)
    private long nodeId;

    @InjectView(R.id.nodePropertiesListLayout)
    private LinearLayout nodePropertiesListLayout;

    @InjectView(R.id.nodePropertiesTitle)
    private TextView nodePropertiesTitle;

    @InjectView(R.id.nodeCreatePropertyButton)
    ImageButton nodeCreatePropertyButton;

    @InjectView(R.id.nodeDeleteButton)
    ImageButton nodeDeleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getResources().getString(R.string.database_title) + " " + dbManager.getCurrentNeo4jDatabaseName());

        nodeCreatePropertyButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                askCreateProperty();
            }
        });
        nodeDeleteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                confirmDeletionOfNode();
            }
        });

        Formatter formatter = new Formatter();
        formatter.format(getResources().getString(R.string.node_properties_text), nodeId);
        nodePropertiesTitle.setText(formatter.toString());
        formatter.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNodePropertiesList();
    }

    private void updateNodePropertiesList() {
        try {
            nodePropertiesListLayout.removeAllViews();
            GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
            database.beginTx();
            try {
                ParcelableNode node = database.getNodeById(nodeId);
                for (String key : node.getPropertyKeys()) {
                    Object value = node.getProperty(key);
                    addNodePropertiesListItem(key, value);
                }
            } finally {
                database.txFinish();
            }
        } catch (Exception e) {
            Ln.e(e, "database exception. node id '" + nodeId + "'");
            showErrorDialog();
        }
    }

    private void addNodePropertiesListItem(final String key, final Object value) {

        View propertyListItem = inflater.inflate(R.layout.property_listitem, null);

        TextView propertyKeyText = (TextView) propertyListItem.findViewById(R.id.propertyKeyText);
        propertyKeyText.setText(key);
        TextView propertyValueText = (TextView) propertyListItem.findViewById(R.id.propertyValueText);
        propertyValueText.setText("" + value);

        ImageButton propertyEditButton = (ImageButton) propertyListItem.findViewById(R.id.propertyEditButton);
        if (value instanceof String) {
            final String valueString = (String) value;
            propertyEditButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    askUpdateProperty(key, valueString);
                }
            });
        } else {
            propertyEditButton.setEnabled(false);
        }

        ImageButton propertyDeleteButton = (ImageButton) propertyListItem.findViewById(R.id.propertyDeleteButton);
        propertyDeleteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                confirmDeletionOfProperty(key, value);
            }
        });

        nodePropertiesListLayout.addView(propertyListItem);
    }

    private void confirmDeletionOfProperty(final String key, final Object value) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.property_delete_title);
        Formatter formatter = new Formatter();
        formatter.format(getResources().getString(R.string.property_delete_from_node_question), key, value, nodeId);
        dialog.setMessage(formatter.toString());
        dialog.setButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
                    database.beginTx();
                    try {
                        ParcelableNode node = database.getNodeById(nodeId);
                        node.removeProperty(key);
                        database.updateNode(node);
                        database.txSuccess();
                    } finally {
                        database.txFinish();
                    }
                } catch (Exception e) {
                    Ln.e(e, "failed to delete property with key '" + key + "' and value '" + value + "' from node with id '"
                            + nodeId + "'.");
                    showErrorDialog();
                }
            }

        });
        dialog.setButton2(getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
        formatter.close();
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

    private void askCreateProperty() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.property_create_title);

        View propertyCreateView = inflater.inflate(R.layout.property_create, null);
        final EditText propertyKeyText = (EditText) propertyCreateView.findViewById(R.id.propertyKeyText);
        final EditText propertyValueText = (EditText) propertyCreateView.findViewById(R.id.propertyValueText);

        dialog.setView(propertyCreateView);
        dialog.setButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String key = propertyKeyText.getText().toString();
                String value = propertyValueText.getText().toString();
                if (!key.isEmpty() && !value.isEmpty()) {
                    createProperty(key, value);
                } else {
                    showPropertyKeyValueEmptyError(key, value);
                }
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

    private void showPropertyKeyValueEmptyError(String key, String value) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.error_title);
        Formatter formatter = new Formatter();
        formatter.format(getResources().getString(R.string.property_key_or_value_empty_error), key, value);
        dialog.setMessage(formatter.toString());
        dialog.setButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
        formatter.close();
    }

    private void createProperty(String key, String value) {
        try {
            GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
            database.beginTx();
            try {
                ParcelableNode node = database.getNodeById(nodeId);
                if (!node.hasProperty(key)) {
                    node.setProperty(key, value);
                    database.updateNode(node);
                    database.txSuccess();
                } else {
                    showPropertyAlreadyExistsError(key, value);
                }
            } finally {
                database.txFinish();
            }
        } catch (Exception e) {
            Ln.e(e, "failed to create new property with key '" + key + "' and value '" + value + "' on node with id '" + nodeId
                    + "'.");
            showErrorDialog();
        }
        updateNodePropertiesList();
    }

    private void showPropertyAlreadyExistsError(String key, String value) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.error_title);
        Formatter formatter = new Formatter();
        formatter.format(getResources().getString(R.string.property_create_already_exists_on_node_error), key, value, nodeId);
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

    private void askUpdateProperty(final String key, String value) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.property_update_title);

        View propertyEditView = inflater.inflate(R.layout.property_edit, null);
        final TextView propertyKeyText = (TextView) propertyEditView.findViewById(R.id.propertyKeyText);
        propertyKeyText.setText(key);
        final EditText propertyValueText = (EditText) propertyEditView.findViewById(R.id.propertyValueText);
        propertyValueText.setText(value);

        dialog.setView(propertyEditView);
        dialog.setButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String value = propertyValueText.getText().toString();
                if (!value.isEmpty()) {
                    try {
                        GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
                        database.beginTx();
                        try {
                            ParcelableNode node = database.getNodeById(nodeId);
                            node.setProperty(key, value);
                            database.updateNode(node);
                            database.txSuccess();
                        } finally {
                            database.txFinish();
                        }
                    } catch (Exception e) {
                        Ln.e(e, "failed to update new property with key '" + key + "' and value '" + value + "' on node with id '"
                                + nodeId + "'.");
                        showErrorDialog();
                    }
                } else {
                    showPropertyKeyValueEmptyError(key, value);
                }
                updateNodePropertiesList();
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

    private void confirmDeletionOfNode() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.node_delete_title);
        Formatter formatter = new Formatter();
        formatter.format(getResources().getString(R.string.node_delete_question), nodeId);
        dialog.setMessage(formatter.toString());
        dialog.setButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    GraphDatabase database = dbManager.getCurrentNeo4jDatabase();
                    database.beginTx();
                    try {
                        ParcelableNode node = database.getNodeById(nodeId);
                        ParcelableNode referenceNode = database.getReferenceNode();
                        if (nodeId != referenceNode.getId()) {
                            List<ParcelableRelationship> relationships = node.getRelationships();
                            for (ParcelableRelationship relationship : relationships) {
                                database.deleteRelationship(relationship.getId());
                            }
                            database.deleteNode(nodeId);
                            database.txSuccess();
                        } else {
                            showCannotDeleteReferenceNodeError();
                        }
                    } finally {
                        database.txFinish();
                    }
                    Intent data = new Intent();
                    data.putExtra(DBInspectorConstants.INTENTEXTRA_NODE_DELETED, true);
                    setResult(RESULT_OK, data);
                    finish();
                } catch (Exception e) {
                    Ln.e(e, "failed to delete node with id '" + nodeId + "'.");
                    showErrorDialog();
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
        formatter.close();
    }

    private void showCannotDeleteReferenceNodeError() {
        AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.setTitle(R.string.error_title);
        dialog.setMessage(getResources().getText(R.string.node_delete_referencenode_error));
        dialog.setButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
    }
}
